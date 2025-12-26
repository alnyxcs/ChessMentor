package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt

interface ExerciseRepository {
    suspend fun findById(id: Long): Exercise?
    suspend fun findSuitableExercise(userId: Long, userRating: Int): Exercise?
    suspend fun findByTheme(themeId: Long): List<Exercise>
    
    // ✅ НОВОЕ: Проверка существования упражнения из конкретной позиции
    suspend fun findBySourceGameAndMove(gameId: Long, moveNumber: Int): Exercise?
    
    // ✅ НОВОЕ: Получить все упражнения пользователя (из его ошибок)
    suspend fun findByUserId(userId: Long): List<Exercise>

    suspend fun saveExercise(exercise: Exercise): Exercise
    suspend fun saveAttempt(attempt: ExerciseAttempt): ExerciseAttempt
    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>
}