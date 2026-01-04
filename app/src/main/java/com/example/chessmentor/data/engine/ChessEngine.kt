// data/engine/ChessEngine.kt
package com.example.chessmentor.data.engine

import kotlin.math.abs

/**
 * Интерфейс шахматного движка
 */
interface ChessEngine {

    suspend fun init(): Boolean
    suspend fun evaluate(fen: String, depthLimit: Int = 15): Int
    suspend fun getBestMove(fen: String, depthLimit: Int = 15): String?
    fun destroy()

    suspend fun setOption(name: String, value: String)

    /**
     * НОВОЕ: Получить лучший ход вместе с линией продолжения (PV)
     * @param fen Позиция в формате FEN
     * @param depthLimit Глубина анализа
     * @return Результат анализа с линией или null при ошибке
     */
    suspend fun getBestMoveWithLine(fen: String, depthLimit: Int = 15): AnalysisLine?

    companion object {
        const val MATE_VALUE = 100000
        const val MATE_THRESHOLD = 90000

        /**
         * Проверяет, является ли оценка матовой
         */
        fun isMateScore(score: Int): Boolean = abs(score) >= MATE_THRESHOLD

        /**
         * Получить количество ходов до мата
         * @return положительное число = мат за белых, отрицательное = мат за чёрных
         */
        fun getMateInMoves(score: Int): Int {
            if (!isMateScore(score)) return 0
            val moves = (MATE_VALUE - abs(score)) / 100
            return if (score > 0) moves else -moves
        }

        /**
         * Форматирование оценки для UI
         */
        fun formatEvaluation(score: Int): String {
            return if (isMateScore(score)) {
                val moves = abs(getMateInMoves(score))
                if (score > 0) "+M$moves" else "-M$moves"
            } else {
                val pawns = score / 100.0
                if (pawns >= 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
            }
        }
    }
}

/**
 * Результат анализа с линией продолжения
 */
data class AnalysisLine(
    val bestMove: String,                    // Лучший ход (UCI)
    val score: Int,                          // Оценка в сантипешках
    val mateIn: Int?,                        // Мат в N ходов (null если не мат)
    val principalVariation: List<String>     // Главная линия (до 6 ходов)
)