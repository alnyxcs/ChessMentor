// data/local/AppDatabase.kt
package com.example.chessmentor.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessmentor.data.local.dao.AnalyzedMoveDao
import com.example.chessmentor.data.local.dao.ExerciseDao
import com.example.chessmentor.data.local.dao.GameDao
import com.example.chessmentor.data.local.dao.MistakeDao
import com.example.chessmentor.data.local.dao.UserDao
import com.example.chessmentor.domain.entity.AnalyzedMove
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.User
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
    version = 4,
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

        /**
         * Миграция 2 → 3: Добавление таблицы analyzed_moves
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Migrating from version 2 to 3: Adding analyzed_moves table")

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

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_analyzed_moves_gameId ON analyzed_moves(gameId)"
                )

                Log.d(TAG, "Migration 2 → 3 completed")
            }
        }

        /**
         * Миграция 3 → 4: Добавление колонки evaluationsJson в games
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Migrating from version 3 to 4: Adding evaluationsJson column")

                database.execSQL(
                    "ALTER TABLE games ADD COLUMN evaluationsJson TEXT DEFAULT NULL"
                )

                Log.d(TAG, "Migration 3 → 4 completed")
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
                    // ✅ ИСПРАВЛЕНО: Миграции для версий 2→3 и 3→4
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    // ✅ ИСПРАВЛЕНО: Destructive только для версии 1
                    .fallbackToDestructiveMigrationFrom(1)
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