package com.example.chessmentor.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min

/**
 * Шкала оценки позиции
 *
 * @param evaluation Оценка в сантипешках (0 = равенство, + = белые лучше, - = черные лучше)
 * @param modifier Модификатор
 */
@Composable
fun EvaluationBar(
    evaluation: Int,
    modifier: Modifier = Modifier
) {
    val whiteColor = Color(0xFFF0F0F0)
    val blackColor = Color(0xFF2C2C2C)

    // Нормализуем оценку (от -1000 до +1000 пешек = -100000 до +100000 сантипешек)
    val normalizedEval = evaluation.coerceIn(-100000, 100000)

    // Преобразуем в проценты (0.5 = равенство, 1.0 = белые выигрывают, 0.0 = черные выигрывают)
    // Используем логистическую функцию для плавного перехода
    val whiteAdvantage = 1.0f / (1.0f + Math.exp(-normalizedEval / 20000.0)).toFloat()

    Row(
        modifier = modifier
            .width(40.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            // Черная часть (сверху)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - whiteAdvantage)
                    .background(blackColor),
                contentAlignment = Alignment.Center
            ) {
                if (evaluation < -50) {
                    Text(
                        text = formatEvaluation(evaluation),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Белая часть (снизу)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(whiteAdvantage)
                    .background(whiteColor),
                contentAlignment = Alignment.Center
            ) {
                if (evaluation > 50) {
                    Text(
                        text = formatEvaluation(evaluation),
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Форматирование оценки для отображения
 */
private fun formatEvaluation(evaluation: Int): String {
    return when {
        abs(evaluation) >= 100000 -> "M" // Мат
        else -> {
            val pawns = evaluation / 100.0
            when {
                pawns > 0 -> "+%.1f".format(pawns)
                pawns < 0 -> "%.1f".format(pawns)
                else -> "0.0"
            }
        }
    }
}