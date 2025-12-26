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
    
    @Query("SELECT * FROM exercises WHERE sourceGameId = :gameId LIMIT 1")
    suspend fun findBySourceGame(gameId: Long): Exercise?
    
    @Query("SELECT e.* FROM exercises e INNER JOIN games g ON e.sourceGameId = g.id WHERE g.userId = :userId")
    suspend fun findByUserId(userId: Long): List<Exercise>
    
    @Query("SELECT e.* FROM exercises e WHERE e.id NOT IN (SELECT exerciseId FROM exercise_attempts WHERE userId = :userId AND solved = 1) AND e.rating BETWEEN :minRating AND :maxRating ORDER BY RANDOM() LIMIT 1")
    suspend fun findUnsolvedExercise(userId: Long, minRating: Int, maxRating: Int): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: ExerciseAttempt): Long

    @Query("SELECT * FROM exercise_attempts WHERE userId = :userId")
    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>
    
    @Query("SELECT COUNT(*) FROM exercise_attempts WHERE userId = :userId AND solved = 1")
    suspend fun countSolvedExercises(userId: Long): Int

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<Exercise>
}
