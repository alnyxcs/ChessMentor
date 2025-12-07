// presentation/ui/screen/GamesListScreen.kt
package com.example.chessmentor.presentation.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.presentation.ui.components.MiniEvaluationGraph
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import kotlin.collections.get

@Composable
fun GamesListScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    container: AppContainer,
    onGameClick: (Game) -> Unit
) {
    val games = gameViewModel.userGames
    val message by gameViewModel.message

    val mistakesByGame = remember { mutableStateMapOf<Long, List<Mistake>>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Заголовок
        item {
            GamesListHeader(gamesCount = games.size)
        }

        // Сообщение
        if (message.isNotEmpty()) {
            item {
                GamesListMessage(
                    message = message,
                    onDismiss = { gameViewModel.clearMessage() }
                )
            }
        }

        // Список партий
        if (games.isEmpty()) {
            item {
                EmptyGamesPlaceholder()
            }
        } else {
            items(games) { game ->
                val gameMistakes = mistakesByGame[game.id] ?: emptyList()
                var isLoading by remember { mutableStateOf(game.id !in mistakesByGame) }

                LaunchedEffect(game.id) {
                    if (game.id != null && game.id !in mistakesByGame) {
                        isLoading = true
                        try {
                            val mistakes = container.mistakeRepository.findByGameId(game.id)
                            mistakesByGame[game.id] = mistakes
                        } catch (e: Exception) {
                            // Обработка ошибки
                        } finally {
                            isLoading = false
                        }
                    }
                }

                GameCard(
                    game = game,
                    mistakes = gameMistakes,
                    onClick = { onGameClick(game) },
                    onDelete = { gameViewModel.deleteGame(game.id!!) }
                )
            }
        }
    }
}

@Composable
private fun GamesListHeader(gamesCount: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Мои партии",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Всего партий: $gamesCount")
        }
    }
}

@Composable
private fun GamesListMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("✕")
            }
        }
    }
}

@Composable
private fun EmptyGamesPlaceholder() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📭", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Партий пока нет",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Загрузите свою первую партию!")
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    mistakes: List<Mistake>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (game.playerColor == ChessColor.WHITE) Color.White else Color.Black,
                                MaterialTheme.shapes.extraSmall
                            )
                            .border(1.dp, Color.Gray, MaterialTheme.shapes.extraSmall)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Партия #${game.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusChip(game.analysisStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Метаданные
            Text(
                text = "Контроль: ${game.timeControl ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Результаты анализа
            if (game.isAnalyzed()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                MiniEvaluationGraph(
                    game = game,
                    mistakes = mistakes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccuracyBadge(accuracy = game.accuracy?.toInt() ?: 0)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MistakeCountSmall(MistakeType.BLUNDER, game.blundersCount)
                        MistakeCountSmall(MistakeType.MISTAKE, game.mistakesCount)
                        MistakeCountSmall(MistakeType.INACCURACY, game.inaccuraciesCount)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: AnalysisStatus) {
    val (color, text) = when (status) {
        AnalysisStatus.COMPLETED -> Color(0xFF81B64C) to "Готово"
        AnalysisStatus.IN_PROGRESS -> Color(0xFFFFA726) to "Анализ..."
        AnalysisStatus.PENDING -> Color.Gray to "Очередь"
        AnalysisStatus.FAILED -> Color(0xFFFA412D) to "Ошибка"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AccuracyBadge(accuracy: Int) {
    Column {
        Text(
            text = "Точность",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Text(
            text = "$accuracy%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                accuracy >= 90 -> Color(0xFF81B64C)
                accuracy >= 70 -> Color(0xFFFFA726)
                else -> Color.Gray
            }
        )
    }
}


@Composable
fun MistakeCountSmall(type: MistakeType, count: Int) {
    if (count > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.getEmoji(), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}