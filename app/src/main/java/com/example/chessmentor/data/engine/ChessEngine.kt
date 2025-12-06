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

    companion object {
        const val MATE_VALUE = 100000

        fun isMateScore(score: Int): Boolean = abs(score) > 50000

        fun getMateInMoves(score: Int): Int {
            return if (score > 0) {
                (MATE_VALUE - score) / 100
            } else {
                -(MATE_VALUE + score) / 100
            }
        }

        fun formatEvaluation(score: Int): String {
            return if (isMateScore(score)) {
                val moves = getMateInMoves(score)
                if (score > 0) "M$moves" else "-M$moves"
            } else {
                String.format("%.1f", score / 100.0)
            }
        }
    }
}