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

    // Проверяем, это мат или обычная оценка
    val isMate = abs(evaluation) > 50000

    // Вычисляем преимущество белых
    val whiteAdvantage = if (isMate) {
        // Для мата: почти полная заливка в пользу выигрывающей стороны
        if (evaluation > 0) 0.95f else 0.05f
    } else {
        // Для обычной оценки: используем логистическую функцию
        val normalizedEval = evaluation.coerceIn(-100000, 100000)
        1.0f / (1.0f + Math.exp(-normalizedEval / 20000.0)).toFloat()
    }

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
                // Показываем оценку если чёрные лучше
                if (evaluation < -50 || (isMate && evaluation < 0)) {
                    Text(
                        text = formatEvaluation(evaluation),
                        color = Color.White,
                        fontSize = 11.sp,
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
                // Показываем оценку если белые лучше
                if (evaluation > 50 || (isMate && evaluation > 0)) {
                    Text(
                        text = formatEvaluation(evaluation),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Форматирование оценки для отображения
 * Поддерживает матовые позиции в формате M1, M2, -M1, -M2
 */
fun formatEvaluation(evaluation: Int): String {
    // Проверяем, это мат или обычная оценка
    val isMate = abs(evaluation) > 50000

    return if (isMate) {
        // Матовая позиция: вычисляем количество ходов до мата
        val movesToMate = if (evaluation > 0) {
            (100000 - evaluation) / 100
        } else {
            (100000 + evaluation) / 100
        }

        // Форматируем как M1, M2, -M1, -M2
        if (evaluation > 0) {
            "M$movesToMate"
        } else {
            "-M$movesToMate"
        }
    } else {
        // Обычная оценка в пешках
        val pawns = evaluation / 100.0
        when {
            pawns > 0 -> "+%.1f".format(pawns)
            pawns < 0 -> "%.1f".format(pawns)
            else -> "0.0"
        }
    }
}