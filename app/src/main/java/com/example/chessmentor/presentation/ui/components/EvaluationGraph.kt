package com.example.chessmentor.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/**
 * Качество хода
 */
enum class MoveQuality {
    BRILLIANT, GREAT_MOVE, BEST_MOVE, GOOD, INACCURACY, MISTAKE, BLUNDER;

    fun getEmoji(): String = when(this) {
        BRILLIANT -> "💎"
        GREAT_MOVE -> "👍"
        BEST_MOVE -> "⭐"
        GOOD -> "✅"
        INACCURACY -> "⚡"
        MISTAKE -> "⚠️"
        BLUNDER -> "❌"
    }

    fun getDescription(): String = when(this) {
        BRILLIANT -> "Блестящий ход"
        GREAT_MOVE -> "Отличный ход"
        BEST_MOVE -> "Лучший ход"
        GOOD -> "Хороший ход"
        INACCURACY -> "Неточность"
        MISTAKE -> "Ошибка"
        BLUNDER -> "Грубая ошибка"
    }

    fun getColor(): Color = when(this) {
        BRILLIANT -> Color(0xFF03A9F4)
        GREAT_MOVE -> Color(0xFF4CAF50)
        BEST_MOVE -> Color(0xFF8BC34A)
        GOOD -> Color.Gray
        INACCURACY -> Color(0xFFFFC107)
        MISTAKE -> Color(0xFFFF9800)
        BLUNDER -> Color(0xFFF44336)
    }
}

/**
 * Ключевой момент в партии
 */
data class KeyMoment(
    val moveIndex: Int,
    val san: String,
    val quality: MoveQuality,
    val evaluation: Int,
    val evaluationChange: Int
)

/**
 * График оценки позиции по ходам партии
 */
@Composable
fun EvaluationGraph(
    evaluations: List<Int>,
    currentMoveIndex: Int,
    keyMoments: List<KeyMoment> = emptyList(),
    onMomentClick: ((KeyMoment) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📈 График оценки", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                if (currentMoveIndex >= 0 && currentMoveIndex < evaluations.size) {
                    val currentEval = evaluations[currentMoveIndex]
                    Text(
                        text = formatEvaluation(currentEval),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            currentEval > 200 -> Color(0xFF4CAF50)
                            currentEval < -200 -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (keyMoments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "⚠️ Ключевые моменты:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                keyMoments.take(3).forEach { moment ->
                    KeyMomentItem(
                        moment = moment,
                        onClick = { onMomentClick?.invoke(moment) }
                    )
                }
            }
            else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Анализ...", color = Color.Gray)
                }
            }
        }
    }
}

/**
 * Отрисовка графика
 */
private fun DrawScope.drawEvaluationGraph(
    evaluations: List<Int>,
    currentMoveIndex: Int,
    keyMoments: List<KeyMoment>
) {
    if (evaluations.isEmpty()) return

    val width = size.width
    val height = size.height
    val padding = 20f
    val graphWidth = width - padding * 2
    val graphHeight = height - padding * 2

    val maxEval = 1000
    val minEval = -1000
    val centerY = height / 2

    drawLine(Color.Gray, Offset(padding, centerY), Offset(width - padding, centerY), 1f)

    val whitePath = Path()
    evaluations.forEachIndexed { index, eval ->
        val x = padding + (index.toFloat() / (evaluations.size - 1).coerceAtLeast(1)) * graphWidth
        val y = centerY - (eval.coerceIn(minEval, maxEval).toFloat() / maxEval) * (graphHeight / 2)
        if (index == 0) whitePath.moveTo(x, y) else whitePath.lineTo(x, y)
    }

    val lastX = padding + graphWidth
    val areaPath = Path().apply {
        addPath(whitePath)
        lineTo(lastX, centerY)
        lineTo(padding, centerY)
        close()
    }
    drawPath(areaPath, Color(0x404CAF50))

    drawPath(whitePath, Color(0xFF2196F3), style = Stroke(3f))

    keyMoments.forEach { moment ->
        if (moment.moveIndex in evaluations.indices) {
            val x = padding + (moment.moveIndex.toFloat() / (evaluations.size - 1).coerceAtLeast(1)) * graphWidth
            val y = centerY - (evaluations[moment.moveIndex].coerceIn(minEval, maxEval).toFloat() / maxEval) * (graphHeight / 2)

            val markerColor = moment.quality.getColor()
            drawCircle(markerColor, 6f, Offset(x, y))
            drawLine(markerColor.copy(0.3f), Offset(x, padding), Offset(x, height - padding), 2f)
        }
    }

    if (currentMoveIndex in evaluations.indices) {
        val x = padding + (currentMoveIndex.toFloat() / (evaluations.size - 1).coerceAtLeast(1)) * graphWidth
        drawLine(Color(0xFFFFEB3B), Offset(x, padding), Offset(x, height - padding), 3f)
    }
}

/**
 * Форматирование оценки
 */
private fun formatEvaluation(evaluation: Int): String {
    return when {
        evaluation.absoluteValue >= 100000 -> "Мат"
        else -> "%.2f".format(evaluation / 100.0).let { if (evaluation > 0) "+$it" else it }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 10.sp)
    }
}


@Composable
private fun KeyMomentItem(moment: KeyMoment, onClick: () -> Unit) { // <-- Добавляем onClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick), // <-- Делаем кликабельным
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${moment.quality.getEmoji()} Ход ${moment.moveIndex / 2 + 1}: ${moment.san}",
            fontSize = 12.sp
        )
        Text(
            text = formatEvaluation(moment.evaluationChange),
            fontSize = 12.sp,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
    }
}
