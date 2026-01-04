package com.example.chessmentor.presentation.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Модель данных для стрелки.
 * Используется и в Тренировке, и в Анализе.
 */
data class BoardArrow(
    val from: Square,
    val to: Square,
    val color: Color
)

/**
 * Вспомогательная функция парсинга UCI (e2e4) в стрелку.
 */
fun parseBestMoveToArrow(uciMove: String?, color: Color = Color(0xAA4CAF50)): BoardArrow? {
    if (uciMove == null || uciMove.length < 4) return null
    return try {
        val from = Square.fromValue(uciMove.substring(0, 2).uppercase())
        val to = Square.fromValue(uciMove.substring(2, 4).uppercase())
        BoardArrow(from, to, color)
    } catch (e: Exception) { null }
}

/**
 * УНИВЕРСАЛЬНАЯ ФУНКЦИЯ РИСОВАНИЯ СТРЕЛКИ
 * Рисует стрелку на любом Canvas (DrawScope).
 */
fun DrawScope.drawBoardArrow(
    arrow: BoardArrow,
    squareSize: Float,
    flipped: Boolean,
    alpha: Float = 0.8f
) {
    // 1. Вычисляем координаты клеток
    val startFile = if (flipped) 7 - arrow.from.file.ordinal else arrow.from.file.ordinal
    val startRank = if (flipped) arrow.from.rank.ordinal else 7 - arrow.from.rank.ordinal
    val endFile = if (flipped) 7 - arrow.to.file.ordinal else arrow.to.file.ordinal
    val endRank = if (flipped) arrow.to.rank.ordinal else 7 - arrow.to.rank.ordinal

    // Центры клеток
    val startX = startFile * squareSize + squareSize / 2
    val startY = startRank * squareSize + squareSize / 2
    val endX = endFile * squareSize + squareSize / 2
    val endY = endRank * squareSize + squareSize / 2

    // 2. Настройки стиля
    val color = arrow.color
    val strokeWidth = squareSize * 0.15f // Толщина зависит от размера клетки
    val arrowHeadSize = squareSize * 0.45f // Размер наконечника

    // 3. Математика поворота
    val angle = atan2(endY - startY, endX - startX)

    // Координаты "усов" наконечника
    val arrowX1 = endX - arrowHeadSize * cos(angle - Math.PI / 6)
    val arrowY1 = endY - arrowHeadSize * sin(angle - Math.PI / 6)
    val arrowX2 = endX - arrowHeadSize * cos(angle + Math.PI / 6)
    val arrowY2 = endY - arrowHeadSize * sin(angle + Math.PI / 6)

    // Корректируем конец линии, чтобы она не торчала из-под острия
    val lineEndX = endX - (arrowHeadSize * 0.6f) * cos(angle)
    val lineEndY = endY - (arrowHeadSize * 0.6f) * sin(angle)

    // 4. Рисуем линию
    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(lineEndX.toFloat(), lineEndY.toFloat()),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        alpha = alpha
    )

    // 5. Рисуем наконечник (треугольник)
    val path = Path().apply {
        moveTo(endX, endY)
        lineTo(arrowX1.toFloat(), arrowY1.toFloat())
        lineTo(arrowX2.toFloat(), arrowY2.toFloat())
        close()
    }
    drawPath(path, color, alpha = alpha)
}