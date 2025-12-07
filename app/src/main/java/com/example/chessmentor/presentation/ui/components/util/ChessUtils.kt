// presentation/ui/util/ChessUtils.kt
package com.example.chessmentor.presentation.ui.components.util

import com.example.chessmentor.data.engine.ChessEngine

/**
 * Форматирование оценки позиции
 */
fun formatEvaluation(evaluation: Int): String {
    return ChessEngine.formatEvaluation(evaluation)
}

/**
 * Проверка на мат
 */
fun isMateScore(score: Int): Boolean {
    return ChessEngine.isMateScore(score)
}

/**
 * Конвертация сантипешек в пешки
 */
fun centipawnsToPawns(centipawns: Int): Double {
    return centipawns / 100.0
}

/**
 * Форматирование потери в пешках
 */
fun formatLoss(centipawns: Int): String {
    return if (centipawns > 50000) {
        "Мат!"
    } else {
        "-%.1f".format(centipawns / 100.0)
    }
}