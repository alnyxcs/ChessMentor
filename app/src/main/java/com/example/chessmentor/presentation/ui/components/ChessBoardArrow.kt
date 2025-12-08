// presentation/ui/components/ChessBoardArrow.kt
package com.example.chessmentor.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Данные для отрисовки стрелки на доске
 */
data class BoardArrow(
    val from: Square,
    val to: Square,
    val color: Color = Color(0xFF4CAF50),
    val strokeWidth: Float = 10f
)

/**
 * Компонент для отрисовки стрелок на шахматной доске
 */
@Composable
fun ChessBoardWithArrows(
    arrows: List<BoardArrow>,
    boardSize: Dp,
    isFlipped: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val squareSize = size.width / 8f

        arrows.forEach { arrow ->
            val fromFile = arrow.from.file.ordinal
            val fromRank = arrow.from.rank.ordinal
            val toFile = arrow.to.file.ordinal
            val toRank = arrow.to.rank.ordinal

            val (displayFromFile, displayFromRank) = if (isFlipped) {
                Pair(7 - fromFile, fromRank)
            } else {
                Pair(fromFile, 7 - fromRank)
            }

            val (displayToFile, displayToRank) = if (isFlipped) {
                Pair(7 - toFile, toRank)
            } else {
                Pair(toFile, 7 - toRank)
            }

            val fromX = (displayFromFile + 0.5f) * squareSize
            val fromY = (displayFromRank + 0.5f) * squareSize
            val toX = (displayToFile + 0.5f) * squareSize
            val toY = (displayToRank + 0.5f) * squareSize

            drawArrowOnCanvas(
                start = Offset(fromX, fromY),
                end = Offset(toX, toY),
                color = arrow.color,
                strokeWidth = arrow.strokeWidth
            )
        }
    }
}

/**
 * Отрисовка стрелки на canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowOnCanvas(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        alpha = 0.8f
    )

    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowHeadLength = strokeWidth * 3f
    val arrowHeadAngle = Math.PI / 6

    val arrowPoint1 = Offset(
        x = end.x - arrowHeadLength * cos(angle - arrowHeadAngle).toFloat(),
        y = end.y - arrowHeadLength * sin(angle - arrowHeadAngle).toFloat()
    )

    val arrowPoint2 = Offset(
        x = end.x - arrowHeadLength * cos(angle + arrowHeadAngle).toFloat(),
        y = end.y - arrowHeadLength * sin(angle + arrowHeadAngle).toFloat()
    )

    val arrowHeadPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(arrowPoint1.x, arrowPoint1.y)
        lineTo(arrowPoint2.x, arrowPoint2.y)
        close()
    }

    drawPath(
        path = arrowHeadPath,
        color = color,
        alpha = 0.8f
    )
}

/**
 * Парсинг UCI-хода в стрелку
 * Пример: "e2e4" -> BoardArrow(E2, E4)
 */
fun parseBestMoveToArrow(
    uciMove: String?,
    color: Color = Color(0xFF4CAF50),
    strokeWidth: Float = 10f
): BoardArrow? {
    if (uciMove == null || uciMove.length < 4) return null

    return try {
        val fromSquare = Square.fromValue(uciMove.substring(0, 2).uppercase())
        val toSquare = Square.fromValue(uciMove.substring(2, 4).uppercase())

        BoardArrow(
            from = fromSquare,
            to = toSquare,
            color = color,
            strokeWidth = strokeWidth
        )
    } catch (e: Exception) {
        null
    }
}