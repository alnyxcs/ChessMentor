// presentation/ui/components/MiniEvaluationGraph.kt

package com.example.chessmentor.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake
import kotlin.math.abs
import kotlin.math.exp

/**
 * Компактный график оценки для карточки партии
 *
 * Может работать:
 * 1. С реальными оценками (evaluations)
 * 2. С генерацией оценок из ошибок (game + mistakes)
 */
@Composable
fun MiniEvaluationGraph(
    evaluations: List<Int> = emptyList(),
    game: Game? = null,
    mistakes: List<Mistake> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Используем реальные оценки или генерируем из ошибок
    val displayEvaluations = remember(evaluations, game, mistakes) {
        if (evaluations.isNotEmpty()) {
            evaluations
        } else if (game != null) {
            generateEvaluationsFromMistakes(game, mistakes)
        } else {
            emptyList()
        }
    }

    if (displayEvaluations.isEmpty()) {
        EmptyMiniGraph(modifier)
        return
    }

    val whiteColor = Color(0xFF4CAF50)  // Зелёный для преимущества белых
    val blackColor = Color(0xFF424242)  // Тёмный для преимущества чёрных
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2

        if (displayEvaluations.size < 2) return@Canvas

        // Градиентные области
        val whitePath = Path()
        val blackPath = Path()

        whitePath.moveTo(0f, centerY)
        blackPath.moveTo(0f, centerY)

        displayEvaluations.forEachIndexed { index, eval ->
            val x = (index.toFloat() / (displayEvaluations.size - 1)) * canvasWidth
            val normalizedEval = normalizeEvalMini(eval)
            val y = centerY - normalizedEval * (canvasHeight / 2 - 2f)

            if (normalizedEval >= 0) {
                whitePath.lineTo(x, y)
                blackPath.lineTo(x, centerY)
            } else {
                whitePath.lineTo(x, centerY)
                blackPath.lineTo(x, y)
            }
        }

        whitePath.lineTo(canvasWidth, centerY)
        whitePath.close()
        blackPath.lineTo(canvasWidth, centerY)
        blackPath.close()

        drawPath(path = whitePath, color = whiteColor.copy(alpha = 0.5f))
        drawPath(path = blackPath, color = blackColor.copy(alpha = 0.5f))

        // Линия оценки
        val linePath = Path()
        displayEvaluations.forEachIndexed { index, eval ->
            val x = (index.toFloat() / (displayEvaluations.size - 1)) * canvasWidth
            val normalizedEval = normalizeEvalMini(eval)
            val y = centerY - normalizedEval * (canvasHeight / 2 - 2f)

            if (index == 0) linePath.moveTo(x, y)
            else linePath.lineTo(x, y)
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )

        // Центральная линия
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1f
        )
    }
}

/**
 * Генерирует примерные оценки на основе ошибок
 */
private fun generateEvaluationsFromMistakes(
    game: Game,
    mistakes: List<Mistake>
): List<Int> {
    // Примерное количество ходов (из PGN сложно получить без парсинга)
    val estimatedMoves = 40 // Средняя партия
    val evaluations = mutableListOf<Int>()
    evaluations.add(0) // Начальная позиция

    var currentEval = 0

    for (halfMove in 0 until estimatedMoves * 2) {
        val moveNumber = (halfMove / 2) + 1
        val isWhiteMove = halfMove % 2 == 0
        val color = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK

        val mistake = mistakes.find { it.moveNumber == moveNumber && it.color == color }

        if (mistake != null) {
            // Применяем потерю от ошибки
            currentEval += if (isWhiteMove) {
                -mistake.evaluationLoss
            } else {
                mistake.evaluationLoss
            }
        } else {
            // Небольшие случайные колебания
            currentEval += if (isWhiteMove) {
                (-5..5).random()
            } else {
                (-5..5).random()
            }
        }

        currentEval = currentEval.coerceIn(-1500, 1500)
        evaluations.add(currentEval)

        // Прекращаем если ошибок больше нет и прошли все известные
        if (mistakes.isEmpty() || (mistakes.isNotEmpty() && moveNumber > mistakes.maxOf { it.moveNumber } + 5)) {
            break
        }
    }

    return evaluations
}

/**
 * Заглушка для пустого мини-графика
 */
@Composable
private fun EmptyMiniGraph(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
    )
}

private fun normalizeEvalMini(centipawns: Int): Float {
    if (abs(centipawns) > 50000) {
        return if (centipawns > 0) 0.9f else -0.9f
    }
    val capped = centipawns.coerceIn(-1000, 1000)
    return (2.0f / (1.0f + exp(-capped / 350.0f).toFloat()) - 1.0f)
        .coerceIn(-0.9f, 0.9f)
}