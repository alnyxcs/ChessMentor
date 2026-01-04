package com.example.chessmentor.domain.analysis

/**
 * Хранит результат анализа: лучший ход + оценка + цепочка продолжения (PV).
 */
data class AnalysisLine(
    val moveUci: String,                // Лучший ход (например, "e2e4")
    val scoreCentipawns: Int,           // Оценка в сантипешках (100 = 1 пешка)
    val isMate: Boolean = false,        // Это мат?
    val mateIn: Int = 0,                // Через сколько ходов мат
    val principalVariation: List<String> = emptyList() // Вся цепочка: ["e2e4", "e7e5", "g1f3"]
)