package com.example.chessmentor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chessmentor.data.local.dao.ExerciseDao
import com.example.chessmentor.data.local.dao.GameDao
import com.example.chessmentor.data.local.dao.MistakeDao
import com.example.chessmentor.data.local.dao.UserDao
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
        ExerciseAttempt::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(MyConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chess_mentor_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}