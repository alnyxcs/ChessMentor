package com.example.chessmentor.domain.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["userId"])] // <-- ДОБАВЬТЕ ЭТО
)
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    val userId: Long,
    val pgnData: String,
    val playerColor: ChessColor,
    val timeControl: String? = null,
    val playedAt: Long,

    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING,
    val analyzedAt: Long? = null,

    val accuracy: Double? = null,
    val averageEvaluationLoss: Int? = null,
    val totalMistakes: Int = 0,
    val blundersCount: Int = 0,
    val mistakesCount: Int = 0,
    val inaccuraciesCount: Int = 0
) {

    init {
        require(pgnData.isNotBlank()) {
            "PGN данные не могут быть пустыми"
        }
        require(accuracy == null || accuracy in 0.0..100.0) {
            "Точность должна быть от 0 до 100"
        }
    }

    /**
     * Завершён ли анализ
     */
    fun isAnalyzed(): Boolean = analysisStatus == AnalysisStatus.COMPLETED

    /**
     * Можно ли просматривать результаты
     */
    fun canViewResults(): Boolean = analysisStatus.isViewable()

    /**
     * Обновить статус анализа на "В процессе"
     */
    fun startAnalysis(): Game {
        return copy(analysisStatus = AnalysisStatus.IN_PROGRESS)
    }

    /**
     * Завершить анализ с результатами
     */
    fun completeAnalysis(
        accuracy: Double,
        avgLoss: Int,
        blunders: Int,
        mistakes: Int,
        inaccuracies: Int
    ): Game {
        return copy(
            analysisStatus = AnalysisStatus.COMPLETED,
            analyzedAt = System.currentTimeMillis(),
            accuracy = accuracy,
            averageEvaluationLoss = avgLoss,
            totalMistakes = blunders + mistakes + inaccuracies,
            blundersCount = blunders,
            mistakesCount = mistakes,
            inaccuraciesCount = inaccuracies
        )
    }

    /**
     * Отметить анализ как проваленный
     */
    fun failAnalysis(): Game {
        return copy(
            analysisStatus = AnalysisStatus.FAILED,
            analyzedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Получить краткое описание партии
     */
    fun getShortDescription(): String {
        val colorSymbol = playerColor.getSymbol()
        val time = timeControl ?: "неизвестно"
        return "$colorSymbol $time"
    }

    override fun toString(): String {
        return "Game(id=$id, userId=$userId, color=$playerColor, status=$analysisStatus)"
    }
}