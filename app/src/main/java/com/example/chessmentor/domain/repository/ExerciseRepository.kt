// domain/repository/ExerciseRepository.kt
package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt

interface ExerciseRepository {

    // ============================================================
    // БАЗОВЫЕ МЕТОДЫ
    // ============================================================

    suspend fun findById(id: Long): Exercise?

    suspend fun findSuitableExercise(
        userId: Long,
        userRating: Int,
        excludeId: Long = -1
    ): Exercise?

    suspend fun findByTheme(theme: String): List<Exercise>

    suspend fun findByPattern(pattern: String): List<Exercise>

    suspend fun findBySourceGameAndMove(gameId: Long, moveNumber: Int): Exercise?

    suspend fun findByUserId(userId: Long): List<Exercise>

    suspend fun findAnyExerciseExcept(excludeId: Long): Exercise?

    suspend fun findByFen(fen: String): Exercise?

    suspend fun deleteDuplicates()

    suspend fun countExercises(): Int

    suspend fun saveExercise(exercise: Exercise): Exercise

    suspend fun saveAttempt(attempt: ExerciseAttempt): ExerciseAttempt

    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>

    // ============================================================
    // МЕТОДЫ КЛАСТЕРИЗАЦИИ (НОВЫЕ)
    // ============================================================

    /**
     * Найти упражнения из того же кластера
     */
    suspend fun findByCluster(
        clusterId: String,
        excludeId: Long = -1,
        limit: Int = 3
    ): List<Exercise>

    /**
     * Найти упражнения с тем же тактическим паттерном
     */
    suspend fun findSimilarByPattern(
        pattern: String,
        excludeId: Long = -1,
        minDifficulty: Int = 0,
        maxDifficulty: Int = 100,
        limit: Int = 3
    ): List<Exercise>

    /**
     * Найти упражнения для повторения ошибки
     */
    suspend fun findForMistakeRepetition(
        userId: Long,
        pattern: String,
        difficulty: Int,
        excludeId: Long = -1,
        limit: Int = 3
    ): List<Exercise>

    /**
     * Увеличить счётчик показов
     */
    suspend fun incrementTimesShown(exerciseId: Long)

    /**
     * Увеличить счётчик решений
     */
    suspend fun incrementTimesSolved(exerciseId: Long)
}