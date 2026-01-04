package com.example.chessmentor.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessmentor.data.local.dao.*
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.data.local.TypeConverters as MyConverters

@Database(
    entities = [
        User::class,
        Game::class,
        Mistake::class,
        Exercise::class,
        ExerciseAttempt::class,
        AnalyzedMove::class
    ],
    version = 5, // ✅ Версия 5
    exportSchema = false
)
@TypeConverters(MyConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun analyzedMoveDao(): AnalyzedMoveDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "chess_mentor_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ==================== МИГРАЦИИ ====================

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS analyzed_moves (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        gameId INTEGER NOT NULL,
                        moveIndex INTEGER NOT NULL,
                        moveNumber INTEGER NOT NULL,
                        color TEXT NOT NULL,
                        quality TEXT NOT NULL,
                        san TEXT NOT NULL,
                        bestMove TEXT,
                        evalBefore INTEGER NOT NULL,
                        evalAfter INTEGER NOT NULL,
                        evalLoss INTEGER NOT NULL,
                        comment TEXT,
                        FOREIGN KEY (gameId) REFERENCES games(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_analyzed_moves_gameId ON analyzed_moves(gameId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE games ADD COLUMN evaluationsJson TEXT DEFAULT NULL")
            }
        }

        /**
         * Получить экземпляр базы данных (Singleton)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // ✅ Разрешаем деструктивную миграцию, если автоматическая не сработает
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()

                INSTANCE = instance
                Log.d(TAG, "Database instance created successfully")
                instance
            }
        }

        /**
         * Очистить instance (для тестирования)
         */
        fun clearInstance() {
            INSTANCE = null
        }

        /**
         * Callback для событий базы данных
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d(TAG, "Database created for the first time, version: ${db.version}")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d(TAG, "Database opened, version: ${db.version}")
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                Log.w(TAG, "⚠️ Destructive migration performed! All data was lost.")
            }
        }
    }
}