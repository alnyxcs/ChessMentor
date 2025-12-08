// presentation/ui/screen/GameViewScreen.kt
package com.example.chessmentor.presentation.ui.components.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chessmentor.domain.entity.AnalyzedMove
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MoveQuality
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.presentation.ui.components.*
import com.example.chessmentor.presentation.viewmodel.BoardViewModel
import com.example.chessmentor.presentation.viewmodel.GameViewModel
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "GameViewScreen"

@Composable
fun GameViewScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    user: User?
) {
    val game by gameViewModel.selectedGame

    if (game == null) {
        NoGameSelectedPlaceholder(paddingValues = paddingValues)
        return
    }

    key(game!!.id) {
        GameViewScreenContent(
            paddingValues = paddingValues,
            game = game!!,
            gameViewModel = gameViewModel,
            user = user
        )
    }
}

@Composable
private fun NoGameSelectedPlaceholder(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Игра не выбрана",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GameViewScreenContent(
    paddingValues: PaddingValues,
    game: Game,
    gameViewModel: GameViewModel,
    user: User?
) {
    val context = LocalContext.current
    val mistakes = gameViewModel.selectedGameMistakes
    val analyzedMoves = gameViewModel.selectedGameAnalyzedMoves
    val gameEvaluations = gameViewModel.selectedGameEvaluations

    val boardViewModel = remember {
        Log.d(TAG, "Creating new BoardViewModel for game ${game.id}")
        BoardViewModel().apply {
            soundManager = SoundManager(context)
        }
    }

    val selectedTheme = remember(user?.preferredTheme) {
        if (user != null) {
            BoardThemes.getAll().find { it.name == user.preferredTheme } ?: BoardThemes.Classic
        } else {
            BoardThemes.Classic
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Disposing BoardViewModel")
            boardViewModel.soundManager?.release()
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Loading game ${game.id}")
        boardViewModel.loadGame(
            game = game,
            gameMistakes = mistakes.toList(),
            gameAnalyzedMoves = analyzedMoves.toList(),
            gameEvaluations = gameEvaluations.toList()
        )
    }

    val board by boardViewModel.board
    val currentMoveIndex by boardViewModel.currentMoveIndex
    val highlightedSquares by boardViewModel.highlightedSquares
    val lastMove by boardViewModel.lastMove

    val currentEvaluation = boardViewModel.getCurrentEvaluation()
    val currentMistake = boardViewModel.getCurrentMistake()
    val currentAnalyzedMove = boardViewModel.getCurrentAnalyzedMove()
    val moves = boardViewModel.moves
    val totalMoves = moves.size
    val hasRealEvaluations = boardViewModel.hasRealEvaluations()

    // ✅ Автоматически показываем стрелку для ошибок
    val bestMoveArrow = remember(currentAnalyzedMove) {
        if (currentAnalyzedMove?.isMistake() == true && currentAnalyzedMove.bestMove != null) {
            parseBestMoveToArrow(currentAnalyzedMove.bestMove)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Evaluation Bar
        EvaluationBarWithIndicator(
            evaluation = currentEvaluation,
            hasRealData = hasRealEvaluations,
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 4.dp
            )
        )

        // 2. Доска со значком качества и стрелкой
        GameBoardWithBadgeAndArrow(
            board = board,
            currentMistake = currentMistake,
            currentAnalyzedMove = currentAnalyzedMove,
            highlightedSquares = highlightedSquares,
            lastMove = lastMove,
            bestMoveArrow = bestMoveArrow,
            isFlipped = game.playerColor == ChessColor.BLACK,
            theme = selectedTheme,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Карточка анализа
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 12.dp)
        ) {
            MoveAnalysisCard(
                currentMistake = currentMistake,
                currentAnalyzedMove = currentAnalyzedMove,
                currentEvaluation = currentEvaluation,
                playerColor = game.playerColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4. Список ходов
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalMoveList(
                moves = moves,
                currentMoveIndex = currentMoveIndex,
                mistakes = mistakes.toList(),
                onMoveClick = { index ->
                    Log.d(TAG, "Move clicked: $index")
                    boardViewModel.goToMove(index)
                },
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .height(36.dp)
            )
        }

        // 5. Навигация
        NavigationBar(
            currentMoveIndex = currentMoveIndex,
            totalMoves = totalMoves,
            onGoToStart = { boardViewModel.goToStart() },
            onGoToPrevious = { boardViewModel.goToPreviousMove() },
            onGoToNext = { boardViewModel.goToNextMove() },
            onGoToEnd = { boardViewModel.goToEnd() }
        )
    }
}

// ==================== КОМПОНЕНТЫ ====================

@Composable
private fun EvaluationBarWithIndicator(
    evaluation: Int,
    hasRealData: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        EvaluationBar(
            evaluation = evaluation,
            modifier = Modifier.fillMaxWidth()
        )

        if (!hasRealData) {
            Text(
                text = "⚠ Приблизительные данные",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun GameBoardWithBadgeAndArrow(
    board: Board,
    currentMistake: Mistake?,
    currentAnalyzedMove: AnalyzedMove?,
    highlightedSquares: Set<Square>,
    lastMove: Pair<Square, Square>?,
    bestMoveArrow: BoardArrow?,
    isFlipped: Boolean,
    theme: BoardTheme,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val boardSize = this.maxWidth

        // Основная доска
        ChessBoard(
            board = board,
            highlightedSquares = highlightedSquares,
            lastMove = lastMove,
            modifier = Modifier.fillMaxSize(),
            flipped = isFlipped,
            theme = theme,
            animateMove = true
        )

        // ✅ Стрелка лучшего хода (показывается автоматически)
        if (bestMoveArrow != null) {
            ChessBoardWithArrows(
                arrows = listOf(bestMoveArrow),
                boardSize = boardSize,
                isFlipped = isFlipped,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Значок качества хода
        if (lastMove != null) {
            val quality = currentAnalyzedMove?.quality
            val shouldShowBadge = quality != null && quality in listOf(
                MoveQuality.BRILLIANT,
                MoveQuality.GREAT_MOVE,
                MoveQuality.BEST_MOVE,
                MoveQuality.BLUNDER,
                MoveQuality.MISTAKE,
                MoveQuality.INACCURACY
            )

            if (shouldShowBadge) {
                val targetSquare = lastMove.second
                val badgeOffset = calculateBadgeOffset(
                    square = targetSquare,
                    isFlipped = isFlipped,
                    boardSize = boardSize
                )

                MoveQualityBadge(
                    mistakeType = currentMistake?.mistakeType,
                    moveQuality = quality,
                    size = 28.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = badgeOffset.first, y = badgeOffset.second)
                )
            }
        }
    }
}

private fun calculateBadgeOffset(
    square: Square,
    isFlipped: Boolean,
    boardSize: Dp
): Pair<Dp, Dp> {
    val file = square.file.ordinal
    val rank = square.rank.ordinal

    val (displayFile, displayRank) = if (isFlipped) {
        Pair(7 - file, rank)
    } else {
        Pair(file, 7 - rank)
    }

    val squareSize = boardSize / 8
    val badgeSize = 28.dp

    val offsetX = squareSize * displayFile + squareSize - badgeSize - 2.dp
    val offsetY = squareSize * displayRank + 2.dp

    return Pair(offsetX, offsetY)
}

@Composable
private fun MoveAnalysisCard(
    currentMistake: Mistake?,
    currentAnalyzedMove: AnalyzedMove?,
    currentEvaluation: Int,
    playerColor: ChessColor,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        when {
            // Ошибка
            currentMistake != null -> {
                CompactMistakeCard(mistake = currentMistake)
            }

            // Хороший ход
            currentAnalyzedMove != null && currentAnalyzedMove.isGoodMove() -> {
                GoodMoveCard(analyzedMove = currentAnalyzedMove)
            }

            // Обычная позиция
            else -> {
                CompactPositionCard(
                    evaluation = currentEvaluation,
                    playerColor = playerColor
                )
            }
        }
    }
}

@Composable
private fun GoodMoveCard(
    analyzedMove: AnalyzedMove,
    modifier: Modifier = Modifier
) {
    val quality = analyzedMove.quality
    val backgroundColor = quality.getColor().copy(alpha = 0.1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quality.getEmoji(),
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quality.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = quality.getColor()
                )

                Text(
                    text = analyzedMove.san,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                analyzedMove.comment?.let { comment ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val evalChange = analyzedMove.evalChange
            if (evalChange != 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (evalChange > 0) "+%.1f".format(evalChange / 100.0) else "%.1f".format(evalChange / 100.0),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (evalChange > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        text = "пешек",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationBar(
    currentMoveIndex: Int,
    totalMoves: Int,
    onGoToStart: () -> Unit,
    onGoToPrevious: () -> Unit,
    onGoToNext: () -> Unit,
    onGoToEnd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onGoToStart,
                enabled = currentMoveIndex > -1
            ) {
                Icon(
                    imageVector = Icons.Default.FirstPage,
                    contentDescription = "В начало",
                    modifier = Modifier.size(28.dp),
                    tint = if (currentMoveIndex > -1) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            IconButton(
                onClick = onGoToPrevious,
                enabled = currentMoveIndex > -1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    modifier = Modifier.size(28.dp),
                    tint = if (currentMoveIndex > -1) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${currentMoveIndex + 1}/$totalMoves",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButton(
                onClick = onGoToNext,
                enabled = currentMoveIndex < totalMoves - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Вперёд",
                    modifier = Modifier.size(28.dp),
                    tint = if (currentMoveIndex < totalMoves - 1) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            IconButton(
                onClick = onGoToEnd,
                enabled = currentMoveIndex < totalMoves - 1
            ) {
                Icon(
                    imageVector = Icons.Default.LastPage,
                    contentDescription = "В конец",
                    modifier = Modifier.size(28.dp),
                    tint = if (currentMoveIndex < totalMoves - 1) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}