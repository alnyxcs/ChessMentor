// data/engine/ChessEngine.kt
package com.example.chessmentor.data.engine

import kotlin.math.abs

interface ChessEngine {

    suspend fun init(): Boolean
    suspend fun evaluate(fen: String, depthLimit: Int = 15): Int
    suspend fun getBestMove(fen: String, depthLimit: Int = 15): String?
    fun destroy()

    // ✅ НОВОЕ: Установка опций движка (Threads, Hash, и т.д.)
    suspend fun setOption(name: String, value: String)

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