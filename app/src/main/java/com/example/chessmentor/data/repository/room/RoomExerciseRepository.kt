// data/repository/room/RoomExerciseRepository.kt
package com.example.chessmentor.data.repository.room

import com.example.chessmentor.data.local.dao.ExerciseDao
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.repository.ExerciseRepository

class RoomExerciseRepository(private val exerciseDao: ExerciseDao) : ExerciseRepository {

    override suspend fun findById(id: Long): Exercise? = exerciseDao.findById(id)

    override suspend fun findSuitableExercise(userId: Long, userRating: Int, excludeId: Long): Exercise? {
        val ratingRange = 300
        return exerciseDao.findUnsolvedExercise(
            userId = userId,
            minRating = userRating - ratingRange,
            maxRating = userRating + ratingRange,
            excludeId = excludeId
        )
    }

    override suspend fun findByTheme(theme: String): List<Exercise> =
        exerciseDao.findByTheme(theme)

    override suspend fun findByPattern(pattern: String): List<Exercise> =
        exerciseDao.findByPattern(pattern)

    override suspend fun findBySourceGameAndMove(gameId: Long, moveNumber: Int): Exercise? {
        return exerciseDao.findBySourceGame(gameId)
    }

    override suspend fun findByUserId(userId: Long): List<Exercise> =
        exerciseDao.findByUserId(userId)

    override suspend fun findAnyExerciseExcept(excludeId: Long): Exercise? =
        exerciseDao.findAnyExerciseExcept(excludeId)

    override suspend fun findByFen(fen: String): Exercise? = exerciseDao.findByFen(fen)

    override suspend fun deleteDuplicates() = exerciseDao.deleteDuplicates()

    override suspend fun countExercises(): Int = exerciseDao.countExercises()

    override suspend fun saveExercise(exercise: Exercise): Exercise {
        val id = exerciseDao.insertExercise(exercise)
        return exercise.copy(id = id)
    }

    override suspend fun saveAttempt(attempt: ExerciseAttempt): ExerciseAttempt {
        val id = exerciseDao.insertAttempt(attempt)
        return attempt.copy(id = id)
    }

    override suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt> =
        exerciseDao.getAttemptsByUserId(userId)

    // ============================================================
    // НОВЫЕ МЕТОДЫ ДЛЯ КЛАСТЕРИЗАЦИИ
    // ============================================================

    override suspend fun findByCluster(
        clusterId: String,
        excludeId: Long,
        limit: Int
    ): List<Exercise> = exerciseDao.findByCluster(clusterId, excludeId, limit)

    override suspend fun findSimilarByPattern(
        pattern: String,
        excludeId: Long,
        minDifficulty: Int,
        maxDifficulty: Int,
        limit: Int
    ): List<Exercise> = exerciseDao.findSimilarByPattern(
        pattern, excludeId, minDifficulty, maxDifficulty, limit
    )

    override suspend fun findForMistakeRepetition(
        userId: Long,
        pattern: String,
        difficulty: Int,
        excludeId: Long,
        limit: Int
    ): List<Exercise> = exerciseDao.findForMistakeRepetition(
        userId, pattern, difficulty, excludeId, limit
    )

    override suspend fun incrementTimesShown(exerciseId: Long) =
        exerciseDao.incrementTimesShown(exerciseId)

    override suspend fun incrementTimesSolved(exerciseId: Long) =
        exerciseDao.incrementTimesSolved(exerciseId)
}