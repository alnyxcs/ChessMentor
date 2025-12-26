package com.example.chessmentor.data.repository.room

import com.example.chessmentor.data.local.dao.ExerciseDao
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.repository.ExerciseRepository

class RoomExerciseRepository(private val exerciseDao: ExerciseDao) : ExerciseRepository {

    override suspend fun findById(id: Long): Exercise? = exerciseDao.findById(id)

    override suspend fun findSuitableExercise(userId: Long, userRating: Int): Exercise? {
        // ✅ УЛУЧШЕНО: Используем оптимизированный запрос с фильтром по рейтингу
        val ratingRange = 200 // ±200 от рейтинга пользователя
        return exerciseDao.findUnsolvedExercise(
            userId = userId,
            minRating = userRating - ratingRange,
            maxRating = userRating + ratingRange
        )
    }

    override suspend fun findByTheme(themeId: Long): List<Exercise> = 
        exerciseDao.findByTheme(themeId)
    
    // ✅ НОВОЕ: Поиск по игре и ходу
    override suspend fun findBySourceGameAndMove(gameId: Long, moveNumber: Int): Exercise? {
        // Упрощённый вариант - ищем по gameId
        // Можно добавить поле moveNumber в Exercise если нужна точность
        return exerciseDao.findBySourceGame(gameId)
    }
    
    // ✅ НОВОЕ: Упражнения пользователя
    override suspend fun findByUserId(userId: Long): List<Exercise> = 
        exerciseDao.findByUserId(userId)

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
}