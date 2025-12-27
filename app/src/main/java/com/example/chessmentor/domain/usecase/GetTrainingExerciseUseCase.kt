package com.example.chessmentor.domain.usecase

import android.util.Log
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.repository.ExerciseRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository

class GetTrainingExerciseUseCase(
    private val userRepository: UserRepository,
    private val mistakeRepository: MistakeRepository,
    private val exerciseRepository: ExerciseRepository
) {
    companion object {
        private const val TAG = "GetTrainingExercise"
    }

    data class Input(val userId: Long, val excludeExerciseId: Long = -1)

    sealed class Result {
        data class Success(val exercise: Exercise) : Result()
        data class Error(val message: String) : Result()
        object NoSuitableExercises : Result()
    }

    suspend fun execute(input: Input): Result {
        val user = userRepository.findById(input.userId)
            ?: return Result.Error("Пользователь не найден")

        Log.d(TAG, "Looking for exercise for user ${user.id}, excluding ${input.excludeExerciseId}")

        // 1. Найти нерешённое упражнение (исключая текущее)
        var exercise = exerciseRepository.findSuitableExercise(
            user.id!!, 
            user.rating, 
            excludeId = input.excludeExerciseId
        )

        if (exercise != null) {
            Log.d(TAG, "Found unsolved exercise: ${exercise.id}")
            return Result.Success(exercise)
        }

        // 2. Если все решены, найти любое другое
        exercise = exerciseRepository.findAnyExerciseExcept(input.excludeExerciseId)
        if (exercise != null) {
            Log.d(TAG, "All solved, returning random exercise: ${exercise.id}")
            return Result.Success(exercise)
        }

        // 3. Создадим новое из последней ошибки
        val lastMistake = mistakeRepository.findByUserId(user.id).lastOrNull()
        if (lastMistake != null && !lastMistake.fenBefore.isNullOrEmpty()) {
            Log.d(TAG, "Creating new exercise from mistake")
            val newExercise = Exercise(
                fenPosition = lastMistake.fenBefore,
                prompt = "Найдите лучший ход в этой позиции.",
                solutionMoves = listOf(lastMistake.bestMove),
                rating = user.rating,
                themeId = lastMistake.themeId,
                sourceGameId = lastMistake.gameId
            )
            val savedExercise = exerciseRepository.saveExercise(newExercise)
            return Result.Success(savedExercise)
        }

        Log.d(TAG, "No exercises available")
        return Result.NoSuitableExercises
    }
}
