// presentation/ui/components/EvaluationBar.kt

package com.example.chessmentor.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.data.engine.ChessEngine
import kotlin.math.exp

@Composable
fun EvaluationBar(
    evaluation: Int,
    modifier: Modifier = Modifier
) {
    val isMate = ChessEngine.isMateScore(evaluation)
    val whiteAdvantage = calculateWhiteAdvantage(evaluation, isMate)

    val animatedAdvantage by animateFloatAsState(
        targetValue = whiteAdvantage,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "advantageAnimation"
    )

    val whiteColor = Color.White
    val blackColor = Color(0xFF1A1A1A)
    val borderColor = Color.Gray

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя часть: БЕЛЫЕ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(animatedAdvantage.coerceAtLeast(0.02f))
                    .background(whiteColor),
                contentAlignment = Alignment.Center
            ) {
                if (animatedAdvantage > 0.55f) {
                    Text(
                        text = ChessEngine.formatEvaluation(evaluation),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Нижняя часть: ЧЁРНЫЕ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight((1f - animatedAdvantage).coerceAtLeast(0.02f))
                    .background(blackColor),
                contentAlignment = Alignment.Center
            ) {
                if (animatedAdvantage < 0.45f) {
                    Text(
                        text = ChessEngine.formatEvaluation(evaluation),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun calculateWhiteAdvantage(evaluation: Int, isMate: Boolean): Float {
    if (isMate) {
        return if (evaluation > 0) 0.98f else 0.02f
    }

    val cappedEval = evaluation.coerceIn(-3000, 3000)
    val normalized = 1.0f / (1.0f + exp(-cappedEval / 400.0f).toFloat())

    return normalized.coerceIn(0.02f, 0.98f)
}