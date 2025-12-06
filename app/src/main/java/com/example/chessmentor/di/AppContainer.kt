// di/AppContainer.kt
package com.example.chessmentor.di

import android.content.Context
import com.example.chessmentor.data.engine.ChessEngine
import com.example.chessmentor.data.engine.StockfishEngine
import com.example.chessmentor.data.local.AppDatabase
import com.example.chessmentor.data.repository.room.*
import com.example.chessmentor.domain.repository.*
import com.example.chessmentor.domain.usecase.*

class AppContainer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // База данных
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // Движок - локальный Stockfish
    val chessEngine: ChessEngine by lazy {
        StockfishEngine(context)
    }

    // DAO
    private val userDao by lazy { database.userDao() }
    private val gameDao by lazy { database.gameDao() }
    private val mistakeDao by lazy { database.mistakeDao() }
    private val exerciseDao by lazy { database.exerciseDao() }

    // Репозитории
    val userRepository: UserRepository by lazy {
        RoomUserRepository(userDao)
    }

    val gameRepository: GameRepository by lazy {
        RoomGameRepository(gameDao)
    }

    val mistakeRepository: MistakeRepository by lazy {
        RoomMistakeRepository(mistakeDao)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(exerciseDao)
    }

    // Use Cases
    val registerUserUseCase by lazy {
        RegisterUserUseCase(userRepository)
    }

    val loginUserUseCase by lazy {
        LoginUserUseCase(userRepository)
    }

    val uploadGameUseCase by lazy {
        UploadGameUseCase(
            gameRepository = gameRepository,
            userRepository = userRepository
        )
    }

    val analyzeGameUseCase by lazy {
        AnalyzeGameUseCase(
            gameRepository = gameRepository,
            mistakeRepository = mistakeRepository,
            userRepository = userRepository,
            chessEngine = chessEngine
        )
    }

    val getTrainingExerciseUseCase by lazy {
        GetTrainingExerciseUseCase(
            userRepository = userRepository,
            mistakeRepository = mistakeRepository,
            exerciseRepository = exerciseRepository
        )
    }

    val getUserStatisticsUseCase by lazy {
        GetUserStatisticsUseCase(
            userRepository = userRepository,
            gameRepository = gameRepository,
            mistakeRepository = mistakeRepository
        )
    }

    // Очистка ресурсов
    fun cleanup() {
        chessEngine.destroy()
    }
}