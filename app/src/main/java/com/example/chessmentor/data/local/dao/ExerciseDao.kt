package com.example.chessmentor.data.local.dao

import androidx.room.*
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun findById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE themeId = :themeId")
    suspend fun findByTheme(themeId: Long): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: ExerciseAttempt): Long

    @Query("SELECT * FROM exercise_attempts WHERE userId = :userId")
    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<Exercise>
}