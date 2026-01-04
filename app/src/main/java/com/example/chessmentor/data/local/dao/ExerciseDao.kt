// data/local/dao/ExerciseDao.kt
package com.example.chessmentor.data.local.dao

import androidx.room.*
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt

@Dao
interface ExerciseDao {

    // ============================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // ============================================================

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun findById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE tacticalPattern = :pattern")
    suspend fun findByPattern(pattern: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE theme = :theme")
    suspend fun findByTheme(theme: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE sourceGameId = :gameId LIMIT 1")
    suspend fun findBySourceGame(gameId: Long): Exercise?

    @Query("""
        SELECT e.* FROM exercises e 
        INNER JOIN games g ON e.sourceGameId = g.id 
        WHERE g.userId = :userId
    """)
    suspend fun findByUserId(userId: Long): List<Exercise>

    @Query("SELECT * FROM exercises WHERE fenPosition = :fen LIMIT 1")
    suspend fun findByFen(fen: String): Exercise?

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    // ============================================================
    // ПОИСК УПРАЖНЕНИЙ ДЛЯ ТРЕНИРОВКИ
    // ============================================================

    /**
     * Поиск нерешённого упражнения с учётом сложности
     */
    @Query("""
        SELECT e.* FROM exercises e 
        WHERE e.id != :excludeId 
        AND e.id NOT IN (
            SELECT exerciseId FROM exercise_attempts 
            WHERE userId = :userId AND solved = 1
        ) 
        AND e.difficultyRating BETWEEN :minRating AND :maxRating 
        ORDER BY RANDOM() 
        LIMIT 1
    """)
    suspend fun findUnsolvedExercise(
        userId: Long,
        minRating: Int,
        maxRating: Int,
        excludeId: Long = -1
    ): Exercise?

    /**
     * Получить любое упражнение кроме указанного
     */
    @Query("SELECT * FROM exercises WHERE id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun findAnyExerciseExcept(excludeId: Long): Exercise?

    // ============================================================
    // КЛАСТЕРИЗАЦИЯ (НОВОЕ)
    // ============================================================

    /**
     * Найти упражнения из того же кластера
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE patternClusterId = :clusterId 
        AND id != :excludeId 
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun findByCluster(
        clusterId: String,
        excludeId: Long = -1,
        limit: Int = 3
    ): List<Exercise>

    /**
     * Найти упражнения с тем же тактическим паттерном
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE tacticalPattern = :pattern 
        AND id != :excludeId 
        AND difficultyRating BETWEEN :minDifficulty AND :maxDifficulty
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun findSimilarByPattern(
        pattern: String,
        excludeId: Long = -1,
        minDifficulty: Int = 0,
        maxDifficulty: Int = 100,
        limit: Int = 3
    ): List<Exercise>

    /**
     * Найти упражнения для повторения ошибки
     * (тот же паттерн, похожая сложность)
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE tacticalPattern = :pattern 
        AND id != :excludeId 
        AND id NOT IN (
            SELECT exerciseId FROM exercise_attempts 
            WHERE userId = :userId AND solved = 1
        )
        AND difficultyRating BETWEEN :difficulty - 15 AND :difficulty + 15
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun findForMistakeRepetition(
        userId: Long,
        pattern: String,
        difficulty: Int,
        excludeId: Long = -1,
        limit: Int = 3
    ): List<Exercise>

    // ============================================================
    // СТАТИСТИКА
    // ============================================================

    @Query("SELECT COUNT(*) FROM exercise_attempts WHERE userId = :userId AND solved = 1")
    suspend fun countSolvedExercises(userId: Long): Int

    @Query("""
        SELECT tacticalPattern, COUNT(*) as count 
        FROM exercises 
        GROUP BY tacticalPattern 
        ORDER BY count DESC
    """)
    suspend fun getPatternDistribution(): List<PatternCount>

    // ============================================================
    // ВСТАВКА И ОБНОВЛЕНИЕ
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: ExerciseAttempt): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("SELECT * FROM exercise_attempts WHERE userId = :userId")
    suspend fun getAttemptsByUserId(userId: Long): List<ExerciseAttempt>

    /**
     * Удалить дубликаты по FEN
     */
    @Query("""
        DELETE FROM exercises 
        WHERE id NOT IN (
            SELECT MIN(id) FROM exercises GROUP BY fenPosition
        )
    """)
    suspend fun deleteDuplicates()

    /**
     * Обновить статистику упражнения
     */
    @Query("""
        UPDATE exercises 
        SET timesShown = timesShown + 1 
        WHERE id = :exerciseId
    """)
    suspend fun incrementTimesShown(exerciseId: Long)

    @Query("""
        UPDATE exercises 
        SET timesSolved = timesSolved + 1,
            successRate = (timesSolved + 1) * 100 / (timesShown + 1)
        WHERE id = :exerciseId
    """)
    suspend fun incrementTimesSolved(exerciseId: Long)
}

/**
 * Для запроса статистики паттернов
 */
data class PatternCount(
    val tacticalPattern: String,
    val count: Int
)