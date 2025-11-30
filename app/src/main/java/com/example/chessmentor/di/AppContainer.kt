package com.example.chessmentor.di

import android.content.Context
import com.example.chessmentor.data.local.AppDatabase
import com.example.chessmentor.data.repository.room.RoomGameRepository
import com.example.chessmentor.data.repository.room.RoomMistakeRepository
import com.example.chessmentor.data.repository.room.RoomUserRepository
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository
import com.example.chessmentor.domain.repository.ExerciseRepository
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.data.local.dao.ExerciseDao

import com.example.chessmentor.data.repository.room.RoomExerciseRepository
import com.example.chessmentor.domain.usecase.*


/**
 * Контейнер зависимостей приложения, работающий с Room
 */
class AppContainer(context: Context) { // <-- Принимаем Context

    // Создаем экземпляр базы данных
    private val database = AppDatabase.getInstance(context)

    // ==================== REPOSITORIES ====================

    val userRepository: UserRepository by lazy {
        RoomUserRepository(database.userDao())
    }

    val gameRepository: GameRepository by lazy {
        RoomGameRepository(database.gameDao())
    }

    val mistakeRepository: MistakeRepository by lazy {
        RoomMistakeRepository(database.mistakeDao())
    }

    val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    // ==================== USE CASES ====================

    val registerUserUseCase: RegisterUserUseCase by lazy {
        RegisterUserUseCase(userRepository)
    }

    val loginUserUseCase: LoginUserUseCase by lazy {
        LoginUserUseCase(userRepository)
    }

    val uploadGameUseCase: UploadGameUseCase by lazy {
        UploadGameUseCase(gameRepository, userRepository)
    }

    val analyzeGameUseCase: AnalyzeGameUseCase by lazy {
        AnalyzeGameUseCase(gameRepository, mistakeRepository, userRepository)
    }

    val getUserStatisticsUseCase: GetUserStatisticsUseCase by lazy {
        GetUserStatisticsUseCase(userRepository, gameRepository, mistakeRepository)
    }

    val getTrainingExerciseUseCase: GetTrainingExerciseUseCase by lazy {
            GetTrainingExerciseUseCase(
                userRepository,
                mistakeRepository,
                exerciseRepository
            )
    }
    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
