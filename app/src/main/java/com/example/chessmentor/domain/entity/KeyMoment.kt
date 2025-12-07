// domain/entity/KeyMoment.kt
package com.example.chessmentor.domain.entity

/**
 * Ключевой момент партии (для отображения в SummaryScreen)
 */
data class KeyMoment(
    val moveIndex: Int,           // Индекс хода (полухода)
    val san: String,              // Нотация хода (e.g., "Nxe5")
    val quality: MoveQuality,     // Качество хода
    val evaluationBefore: Int,    // Оценка до хода (в сантипешках)
    val evaluationAfter: Int,     // Оценка после хода
    val evaluationChange: Int,    // Изменение оценки
    val isPlayerMove: Boolean,    // Ход игрока (не противника)
    val comment: String? = null   // Комментарий
)