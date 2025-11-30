package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.repository.ExerciseRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository

/**
 * Use Case: Получение упражнения для тренировки
 *
 * Логика:
 * 1. Найти самые частые темы ошибок пользователя.
 * 2. Найти или сгенерировать упражнение по этой теме.
 */
class GetTrainingExerciseUseCase(
    private val userRepository: UserRepository,
    private val mistakeRepository: MistakeRepository,
    private val exerciseRepository: ExerciseRepository
) {

    data class Input(val userId: Long)

    sealed class Result {
        data class Success(val exercise: Exercise) : Result()
        data class Error(val message: String) : Result()
        object NoSuitableExercises : Result()
    }

    suspend fun execute(input: Input): Result {
        val user = userRepository.findById(input.userId)
            ?: return Result.Error("Пользователь не найден")

        // 1. Найти упражнение, созданное из ошибки пользователя
        var exercise = exerciseRepository.findSuitableExercise(user.id!!, user.rating)

        if (exercise != null) {
            return Result.Success(exercise)
        }

        // 2. Если нет, создадим новое из последней ошибки
        val lastMistake = mistakeRepository.findByUserId(user.id!!).lastOrNull()
        if (lastMistake != null) {
            // Создаём упражнение на лету
            val newExercise = Exercise(
                fenPosition = lastMistake.fenBefore!!,
                prompt = "Найдите лучший ход в этой позиции.",
                solutionMoves = listOf(lastMistake.bestMove),
                rating = user.rating,
                themeId = lastMistake.themeId,
                sourceGameId = lastMistake.gameId
            )
            val savedExercise = exerciseRepository.saveExercise(newExercise)
            return Result.Success(savedExercise)
        }

        return Result.NoSuitableExercises
    }
}