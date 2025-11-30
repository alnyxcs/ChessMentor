package com.example.chessmentor.data.repository.room

import com.example.chessmentor.data.local.dao.ExerciseDao
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.repository.ExerciseRepository
class RoomExerciseRepository(private val exerciseDao: ExerciseDao) : ExerciseRepository {


    override suspend fun findById(id: Long): Exercise? = exerciseDao.findById(id)

    override suspend fun findSuitableExercise(userId: Long, userRating: Int): Exercise? {
        val solvedIds = exerciseDao.getAttemptsByUserId(userId).map { it.exerciseId }.toSet()
        val allExercises = exerciseDao.getAllExercises()
        return allExercises.find { it.id !in solvedIds }
    }

    override suspend fun findByTheme(themeId: Long): List<Exercise> = exerciseDao.findByTheme(themeId)

    override suspend fun saveExercise(exercise: Exercise): Exercise {
        val id = exerciseDao.insertExercise(exercise)
        return exercise.copy(id = id)
    }

    override suspend fun saveAttempt(attempt: ExerciseAttempt): ExerciseAttempt {
        val id = exerciseDao.insertAttempt(attempt)
        return attempt.copy(id = id)
    }

    override suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt> = exerciseDao.getAttemptsByUserId(userId)
}