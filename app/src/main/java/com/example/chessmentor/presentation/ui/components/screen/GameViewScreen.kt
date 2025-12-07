// presentation/ui/screen/GameViewScreen.kt
package com.example.chessmentor.presentation.ui.components.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.presentation.ui.components.*
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.abs

@Composable
fun GameViewScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    user: User?
) {
    val game by gameViewModel.selectedGame
    val mistakes = gameViewModel.selectedGameMistakes

    val context = LocalContext.current
    val boardViewModel = remember {
        BoardViewModel().apply { soundManager = SoundManager(context) }
    }

    val selectedTheme = remember(user?.preferredTheme) {
        if (user != null) {
            BoardThemes.getAll().find { it.name == user.preferredTheme } ?: BoardThemes.Classic
        } else {
            BoardThemes.Classic
        }
    }

    DisposableEffect(Unit) {
        onDispose { boardViewModel.soundManager?.release() }
    }

    LaunchedEffect(game?.id) {
        game?.let {
            val realEvaluations = gameViewModel.getLatestEvaluations()
            boardViewModel.loadGame(it, mistakes, realEvaluations)
        }
    }

    if (game == null) return

    val currentGame = game!!
    val board by boardViewModel.board
    val currentMoveIndex by boardViewModel.currentMoveIndex
    val currentEvaluation = boardViewModel.getCurrentEvaluation()
    val currentMoveNotation = boardViewModel.getCurrentMoveNotation()
    val highlightedSquares by boardViewModel.highlightedSquares
    val lastMove by boardViewModel.lastMove
    val currentMistake = boardViewModel.getCurrentMistake()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Доска с шкалой оценки
        GameBoard(
            board = board,
            currentEvaluation = currentEvaluation,
            playerColor = currentGame.playerColor,
            currentMistake = currentMistake,
            highlightedSquares = highlightedSquares,
            lastMove = lastMove,
            isFlipped = currentGame.playerColor == ChessColor.BLACK,
            theme = selectedTheme
        )

        // Панель управления
        ControlPanel(
            currentMoveNotation = currentMoveNotation,
            currentMoveIndex = currentMoveIndex,
            totalMoves = boardViewModel.moves.size,
            onGoToStart = { boardViewModel.goToStart() },
            onGoToPrevious = { boardViewModel.goToPreviousMove() },
            onGoToNext = { boardViewModel.goToNextMove() },
            onGoToEnd = { boardViewModel.goToEnd() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Анализ текущего хода
        MoveAnalysisSection(
            currentMistake = currentMistake,
            currentEvaluation = currentEvaluation,
            playerColor = currentGame.playerColor,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GameBoard(
    board: Board,
    currentEvaluation: Int,
    playerColor: ChessColor,
    currentMistake: Mistake?,
    highlightedSquares: Set<Square>,
    lastMove: Pair<Square, Square>?,
    isFlipped: Boolean,
    theme: BoardTheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Используем EvaluationBar из компонентов
            EvaluationBar(
                evaluation = currentEvaluation,
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp)
            )

            ChessBoard(
                board = board,
                highlightedSquares = highlightedSquares,
                lastMove = lastMove,
                modifier = Modifier.fillMaxSize(),
                flipped = isFlipped,
                theme = theme,
                animateMove = true
            )
        }
    }
}

@Composable
private fun ControlPanel(
    currentMoveNotation: String,
    currentMoveIndex: Int,
    totalMoves: Int,
    onGoToStart: () -> Unit,
    onGoToPrevious: () -> Unit,
    onGoToNext: () -> Unit,
    onGoToEnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentMoveIndex >= 0) {
                    Text(
                        text = "${(currentMoveIndex / 2) + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = currentMoveNotation,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (totalMoves > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ((currentMoveIndex + 1).toFloat() / totalMoves).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onGoToStart, enabled = currentMoveIndex > -1) {
                    Icon(Icons.Default.FirstPage, "В начало", Modifier.size(28.dp))
                }
                IconButton(onClick = onGoToPrevious, enabled = currentMoveIndex > -1) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", Modifier.size(28.dp))
                }
                IconButton(onClick = onGoToNext, enabled = currentMoveIndex < totalMoves - 1) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Вперёд", Modifier.size(28.dp))
                }
                IconButton(onClick = onGoToEnd, enabled = currentMoveIndex < totalMoves - 1) {
                    Icon(Icons.Default.LastPage, "В конец", Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun MoveAnalysisSection(
    currentMistake: Mistake?,
    currentEvaluation: Int,
    playerColor: ChessColor,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (currentMistake != null) {
            MistakeCard(mistake = currentMistake, playerColor = playerColor)
        } else {
            GoodMoveCard(currentEvaluation = currentEvaluation, playerColor = playerColor)
        }
    }
}

@Composable
private fun GoodMoveCard(
    currentEvaluation: Int,
    playerColor: ChessColor
) {
    val playerEval = if (playerColor == ChessColor.WHITE) currentEvaluation else -currentEvaluation
    val evalText = formatEvaluation(playerEval)
    val cardInfo = getGoodMoveCardInfo(playerEval)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardInfo.backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = cardInfo.icon,
                    contentDescription = null,
                    tint = cardInfo.contentColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = cardInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardInfo.contentColor
                    )
                    Text(
                        text = "Ваша оценка позиции",
                        style = MaterialTheme.typography.bodySmall,
                        color = cardInfo.contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = evalText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cardInfo.contentColor
            )
        }
    }
}

private data class GoodMoveCardInfo(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val title: String
)

@Composable
private fun getGoodMoveCardInfo(playerEval: Int): GoodMoveCardInfo {
    return when {
        playerEval > 200 -> GoodMoveCardInfo(
            Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32),
            Icons.Default.CheckCircle, "Отличная позиция"
        )
        playerEval > 50 -> GoodMoveCardInfo(
            Color(0xFF8BC34A).copy(alpha = 0.15f), Color(0xFF558B2F),
            Icons.Default.Check, "Хороший ход"
        )
        playerEval < -200 -> GoodMoveCardInfo(
            Color(0xFFFF5722).copy(alpha = 0.15f), Color(0xFFD84315),
            Icons.Default.Warning, "Тяжёлая позиция"
        )
        playerEval < -50 -> GoodMoveCardInfo(
            Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFE65100),
            Icons.Default.Info, "Сложная позиция"
        )
        else -> GoodMoveCardInfo(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Balance, "Равная позиция"
        )
    }
}

private fun formatEvaluation(centipawns: Int): String {
    return when {
        centipawns > 50000 -> "+M${(100000 - centipawns + 1) / 2}"
        centipawns < -50000 -> "-M${(100000 + centipawns + 1) / 2}"
        else -> {
            val pawns = centipawns / 100.0
            if (pawns >= 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
        }
    }
}

@Composable
fun MistakeCard(mistake: Mistake, playerColor: ChessColor = mistake.color) {
    val backgroundColor = when (mistake.mistakeType) {
        MistakeType.BLUNDER -> Color(0xFFFFEBEE)
        MistakeType.MISTAKE -> Color(0xFFFFF3E0)
        MistakeType.INACCURACY -> Color(0xFFFFFDE7)
    }
    val accentColor = mistake.mistakeType.getColor()
    val lossText = if (mistake.evaluationLoss > 50000) "Упущен мат!"
    else "-${String.format("%.1f", mistake.getEvaluationLossInPawns())} пешки"

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(mistake.mistakeType.getIcon(), null, tint = accentColor, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(16.dp)
                            .background(if (mistake.color == ChessColor.WHITE) Color.White else Color.Black, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(mistake.mistakeType.getDisplayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
                        Text("Ход ${mistake.moveNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)), shape = RoundedCornerShape(8.dp)) {
                    Text(lossText, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Сыграно", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(mistake.userMove, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
                Icon(Icons.Default.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Лучше было", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(mistake.bestMove, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            if (!mistake.comment.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(mistake.comment, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }
        }
    }
}