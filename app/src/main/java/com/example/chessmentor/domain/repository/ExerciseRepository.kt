package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt

interface ExerciseRepository {
    suspend fun findById(id: Long): Exercise?
    suspend fun findSuitableExercise(userId: Long, userRating: Int, excludeId: Long = -1): Exercise?
    suspend fun findByTheme(themeId: Long): List<Exercise>
    suspend fun findBySourceGameAndMove(gameId: Long, moveNumber: Int): Exercise?
    suspend fun findByUserId(userId: Long): List<Exercise>
    suspend fun findAnyExerciseExcept(excludeId: Long): Exercise?
    suspend fun findByFen(fen: String): Exercise?
    suspend fun deleteDuplicates()
    suspend fun countExercises(): Int

    suspend fun saveExercise(exercise: Exercise): Exercise
    suspend fun saveAttempt(attempt: ExerciseAttempt): ExerciseAttempt
    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>
}


