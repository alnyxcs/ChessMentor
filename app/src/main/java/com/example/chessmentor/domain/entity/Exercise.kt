// domain/entity/Exercise.kt
package com.example.chessmentor.domain.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.chessmentor.data.local.TypeConverters as MyConverters

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["tacticalPattern"]),
        Index(value = ["difficultyRating"]),
        Index(value = ["patternClusterId"])
    ]
)
@TypeConverters(MyConverters::class)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    val fenPosition: String,

    val solutionMoves: List<String>,

    /**
     * Основной тактический паттерн задачи
     */
    val tacticalPattern: TacticalPattern,

    /**
     * Идентификатор кластера похожих позиций
     * Формат: "{pattern}_{subtype}_{difficulty}"
     * Пример: "FORK_KNIGHT_MEDIUM"
     */
    val patternClusterId: String,

    /**
     * Сложность задачи (0-100)
     */
    val difficultyRating: Int,

    /**
     * Дополнительные характеристики паттерна
     */
    val patternFeatures: List<String> = emptyList(),

    /**
     * Тема (deprecated, используйте tacticalPattern)
     */
    val theme: TacticalTheme = TacticalTheme.TACTIC,

    /**
     * Рейтинг (deprecated, используйте difficultyRating)
     */
    val rating: Int = 1500,

    val createdFrom: ExerciseSource = ExerciseSource.MISTAKE,
    val sourceGameId: Long? = null,
    val sourceMoveNumber: Int? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val timesShown: Int = 0,
    val timesSolved: Int = 0,
    val successRate: Int = 0
) {
    fun getEffectiveDifficulty(): Int {
        if (timesShown < 3) return difficultyRating

        return when {
            successRate > 80 -> (difficultyRating * 0.8).toInt()
            successRate < 30 -> (difficultyRating * 1.2).toInt()
            else -> difficultyRating
        }
    }
}