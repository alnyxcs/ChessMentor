// presentation/ui/screen/SummaryScreen.kt
package com.example.chessmentor.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.KeyMoment
import com.example.chessmentor.domain.entity.MoveQuality
import com.example.chessmentor.presentation.ui.components.EvaluationGraph
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import kotlin.math.abs

@Composable
fun SummaryScreen(
    paddingValues: PaddingValues,
    game: Game,
    keyMoments: List<KeyMoment>,
    boardViewModel: BoardViewModel,
    onReviewClick: () -> Unit
) {
    val currentMoveIndex by boardViewModel.currentMoveIndex

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        item {
            SummaryHeader(game = game)
        }

        // Основные метрики
        if (game.isAnalyzed()) {
            item {
                GameMetricsCard(game = game)
            }
        }

        // График оценки
        item {
            val evaluations = boardViewModel.evaluations.toList()
            EvaluationGraph(
                evaluations = evaluations,
                keyMoments = keyMoments,
                currentMoveIndex = currentMoveIndex,
                playerColor = game.playerColor,
                onMoveClick = { index ->
                    boardViewModel.goToMove(index)
                },
                modifier = Modifier.fillMaxWidth(),
                showMoveNumbers = true,
                animateChanges = true
            )
        }

        // Ключевые моменты
        item {
            KeyMomentsCard(
                keyMoments = keyMoments,
                onMomentClick = { moment ->
                    boardViewModel.goToMove(moment.moveIndex)
                    onReviewClick()
                }
            )
        }

        // Кнопка разбора
        item {
            Button(
                onClick = onReviewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Перейти к разбору партии", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SummaryHeader(game: Game) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 Сводка анализа",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Партия #${game.id ?: "?"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            // Индикатор цвета игрока
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (game.playerColor == ChessColor.WHITE) Color.White else Color.Black,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
private fun GameMetricsCard(game: Game) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📈 Статистика партии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Точность
                MetricItem(
                    label = "Точность",
                    value = "${game.accuracy?.toInt() ?: 0}%",
                    color = getAccuracyColor(game.accuracy ?: 0.0)
                )

                // Средняя потеря
                MetricItem(
                    label = "Ср. потеря",
                    value = "%.1f".format((game.averageEvaluationLoss ?: 0) / 100.0),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Всего ошибок
                MetricItem(
                    label = "Ошибок",
                    value = "${game.totalMistakes}",
                    color = when {
                        game.totalMistakes > 5 -> Color(0xFFE53935)
                        game.totalMistakes > 2 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Детализация ошибок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ErrorTypeItem(
                    emoji = "??",
                    label = "Грубые",
                    count = game.blundersCount,
                    color = Color(0xFFD32F2F)
                )
                ErrorTypeItem(
                    emoji = "?",
                    label = "Ошибки",
                    count = game.mistakesCount,
                    color = Color(0xFFF57C00)
                )
                ErrorTypeItem(
                    emoji = "?!",
                    label = "Неточности",
                    count = game.inaccuraciesCount,
                    color = Color(0xFFFBC02D)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorTypeItem(
    emoji: String,
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = emoji,
            fontSize = 20.sp
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else Color.Gray.copy(alpha = 0.5f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getAccuracyColor(accuracy: Double): Color {
    return when {
        accuracy >= 90 -> Color(0xFF4CAF50)
        accuracy >= 70 -> Color(0xFF8BC34A)
        accuracy >= 50 -> Color(0xFFFF9800)
        else -> Color(0xFFE53935)
    }
}

@Composable
fun KeyMomentsCard(
    keyMoments: List<KeyMoment>,
    onMomentClick: (KeyMoment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⭐ Ключевые моменты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Хорошие ходы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KeyMomentItemLarge(
                    quality = MoveQuality.BRILLIANT,
                    count = keyMoments.count { it.quality == MoveQuality.BRILLIANT }
                )
                KeyMomentItemLarge(
                    quality = MoveQuality.GREAT_MOVE,
                    count = keyMoments.count { it.quality == MoveQuality.GREAT_MOVE }
                )
                KeyMomentItemLarge(
                    quality = MoveQuality.BEST_MOVE,
                    count = keyMoments.count { it.quality == MoveQuality.BEST_MOVE }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Ошибки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KeyMomentItemLarge(
                    quality = MoveQuality.INACCURACY,
                    count = keyMoments.count { it.quality == MoveQuality.INACCURACY }
                )
                KeyMomentItemLarge(
                    quality = MoveQuality.MISTAKE,
                    count = keyMoments.count { it.quality == MoveQuality.MISTAKE }
                )
                KeyMomentItemLarge(
                    quality = MoveQuality.BLUNDER,
                    count = keyMoments.count { it.quality == MoveQuality.BLUNDER }
                )
            }

            // Список моментов
            if (keyMoments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Быстрый обзор:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                keyMoments
                    .sortedByDescending { abs(it.evaluationChange) }
                    .take(5)
                    .forEach { moment ->
                        KeyMomentRow(moment = moment, onClick = onMomentClick)
                    }
            }
        }
    }
}

@Composable
fun KeyMomentItemLarge(
    quality: MoveQuality,
    count: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = quality.getEmoji(),
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) quality.getColor() else Color.Gray.copy(alpha = 0.5f)
        )

        Text(
            text = quality.getDisplayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KeyMomentRow(
    moment: KeyMoment,
    onClick: (KeyMoment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick(moment) },
        colors = CardDefaults.cardColors(
            containerColor = moment.quality.getColor().copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Эмодзи качества
            Text(
                text = moment.quality.getEmoji(),
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Информация о ходе
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ход ${moment.moveIndex / 2 + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = moment.san,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Изменение оценки
            Text(
                text = formatEvaluationChange(moment.evaluationChange),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (moment.evaluationChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Форматирование изменения оценки
 */
private fun formatEvaluationChange(change: Int): String {
    val pawns = change / 100.0
    return when {
        change > 0 -> "+%.1f".format(pawns)
        change < 0 -> "%.1f".format(pawns)
        else -> "0.0"
    }
}