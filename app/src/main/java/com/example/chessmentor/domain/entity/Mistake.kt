package com.example.chessmentor.domain.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "mistakes",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["gameId"])] // <-- ДОБАВЬТЕ ЭТО
)
data class Mistake(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val gameId: Long,
    val themeId: Long,
    val moveNumber: Int,
    val evaluationLoss: Int,
    val mistakeType: MistakeType,
    val bestMove: String,
    val userMove: String,
    val comment: String? = null,
    val fenBefore: String? = null
) {
    init {
        require(moveNumber > 0) { "Номер хода должен быть положительным" }
        require(evaluationLoss >= 0) { "Потеря оценки не может быть отрицательной" }
        require(bestMove.isNotBlank()) { "Лучший ход не может быть пустым" }
        require(userMove.isNotBlank()) { "Ход игрока не может быть пустым" }
    }

    fun getDescription(): String = mistakeType.getDescription()
    fun getColor(): String = mistakeType.getColor()
    fun getEmoji(): String = mistakeType.getEmoji()
    fun getEvaluationLossInPawns(): Double = evaluationLoss / 100.0

    fun getFormattedDescription(): String {
        val emoji = getEmoji()
        val loss = getEvaluationLossInPawns()
        return "Ход $moveNumber. $userMove $emoji (Лучше: $bestMove) -${String.format("%.2f", loss)}"
    }

    fun isCritical(): Boolean = mistakeType == MistakeType.BLUNDER
    fun getSeverity(): Int = when (mistakeType) {
        MistakeType.BLUNDER -> 10
        MistakeType.MISTAKE -> 6
        MistakeType.INACCURACY -> 3
    }

    override fun toString(): String {
        return "Mistake(id=$id, move=$moveNumber, type=$mistakeType, loss=$evaluationLoss)"
    }
}



/**
 * Адаптивный классификатор ошибок
 *
 * Определяет тип ошибки на основе потери оценки и рейтинга игрока.
 * Пороги зависят от уровня игрока - для новичков требования мягче.
 */
object MistakeClassifier {

    /**
     * Пороги для классификации ошибок
     */
    data class Thresholds(
        val inaccuracy: Int,  // Минимальная потеря для неточности
        val mistake: Int,     // Минимальная потеря для ошибки
        val blunder: Int      // Минимальная потеря для грубой ошибки
    )

    /**
     * Рассчитать пороги на основе рейтинга игрока
     */
    fun calculateThresholds(rating: Int): Thresholds {
        val factor = when {
            rating < 800 -> 2.5    // Новички - мягкие требования
            rating < 1200 -> 2.0
            rating < 1600 -> 1.5
            rating < 2000 -> 1.2
            else -> 1.0            // Эксперты - строгие требования
        }

        return Thresholds(
            inaccuracy = (30 * factor).toInt(),
            mistake = (75 * factor).toInt(),
            blunder = (200 * factor).toInt()
        )
    }

    /**
     * Классифицировать ошибку на основе потери оценки и рейтинга
     *
     * @return MistakeType или null если потеря незначительна
     */
    fun classify(evaluationLoss: Int, userRating: Int): MistakeType? {
        val thresholds = calculateThresholds(userRating)

        return when {
            evaluationLoss >= thresholds.blunder -> MistakeType.BLUNDER
            evaluationLoss >= thresholds.mistake -> MistakeType.MISTAKE
            evaluationLoss >= thresholds.inaccuracy -> MistakeType.INACCURACY
            else -> null  // Потеря незначительна
        }
    }

    /**
     * Получить минимальную потерю для типа ошибки с учётом рейтинга
     */
    fun getMinLossForType(type: MistakeType, userRating: Int): Int {
        val thresholds = calculateThresholds(userRating)
        return when (type) {
            MistakeType.INACCURACY -> thresholds.inaccuracy
            MistakeType.MISTAKE -> thresholds.mistake
            MistakeType.BLUNDER -> thresholds.blunder
        }
    }
}