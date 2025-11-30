package com.example.chessmentor.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake

/**
 * Мини-график оценки для карточки партии
 */
@Composable
fun MiniEvaluationGraph(
    game: Game,
    mistakes: List<Mistake>,
    modifier: Modifier = Modifier
) {
    if (!game.isAnalyzed()) return

    // Получаем оценки (упрощенная логика)
    val evaluations = remember (game.id) {
        generateMiniEvaluations(game, mistakes)
    }

    if (evaluations.isNotEmpty()) {
        Canvas(modifier = modifier) {
            drawMiniEvaluationGraph(evaluations)
        }
    }
}

/**
 * Генерация оценок для мини-графика
 */
private fun generateMiniEvaluations(game: Game, mistakes: List<Mistake>): List<Int> {
    val evaluations = mutableListOf<Int>()
    evaluations.add(0)

    var currentEval = 0
    val totalMoves = (game.pgnData.split(" ").size) / 2

    for (i in 1..totalMoves) {
        val mistake = mistakes.find { it.moveNumber == i }
        if (mistake != null) {
            currentEval += if (i % 2 != 0) {
                -mistake.evaluationLoss
            } else {
                mistake.evaluationLoss
            }
        }

        currentEval = currentEval.coerceIn(-1500, 1500)
        evaluations.add(currentEval)
    }

    return evaluations
}

/**
 * Отрисовка мини-графика
 */
private fun DrawScope.drawMiniEvaluationGraph(evaluations: List<Int>) {
    val width = size.width
    val height = size.height

    val maxEval = 1000
    val minEval = -1000

    // Центральная линия
    val centerY = height / 2
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 1f
    )

    // Линия графика
    val linePath = Path()
    evaluations.forEachIndexed { index, eval ->
        val x = (index.toFloat() / (evaluations.size - 1).coerceAtLeast(1)) * width
        val normalizedEval = eval.coerceIn(minEval, maxEval)
        val y = centerY - (normalizedEval.toFloat() / maxEval) * (height / 2)

        if (index == 0) {
            linePath.moveTo(x, y)
        } else {
            linePath.lineTo(x, y)
        }
    }

    drawPath(
        path = linePath,
        color = Color(0xFF2196F3),
        style = Stroke(width = 2f)
    )
}