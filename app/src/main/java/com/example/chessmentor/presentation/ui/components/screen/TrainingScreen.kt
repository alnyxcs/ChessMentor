package com.example.chessmentor.presentation.ui.components.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.analysis.MistakeAnalysis
import com.example.chessmentor.presentation.ui.components.ChessBoard
import com.example.chessmentor.presentation.viewmodel.TrainingViewModel
import com.example.chessmentor.presentation.viewmodel.SessionStats
import com.example.chessmentor.presentation.viewmodel.TrainingUiState
import com.example.chessmentor.presentation.viewmodel.HintLevel
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

@Composable
fun TrainingScreen(
    paddingValues: PaddingValues,
    container: AppContainer,
    userId: Long
) {
    val viewModel = remember { TrainingViewModel(container) }

    LaunchedEffect(userId) {
        val user = container.userRepository.findById(userId)
        if (user != null) {
            viewModel.init(user)
        }
    }

    val uiState by viewModel.uiState
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.isErrorShake) {
        if (uiState.isErrorShake) {
            for (i in 0..2) {
                shakeOffset.animateTo(10f, animationSpec = tween(50))
                shakeOffset.animateTo(-10f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    LaunchedEffect(uiState.board.fen) {
        selectedSquare = null
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            SessionHeader(stats = uiState.sessionStats, sideToMove = uiState.board.sideToMove)
        },
        bottomBar = {
            BottomControlPanel(
                uiState = uiState,
                onNextExercise = {
                    selectedSquare = null
                    viewModel.loadNextExercise()
                },
                onHint = { viewModel.onHintClicked() },
                onShowBetterMove = { viewModel.onShowBetterMoveClicked() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. КОНТЕЙНЕР ДОСКИ (Адаптивный)
            // weight(1f) означает: "Займи всё вертикальное место, которое осталось"
            // Внутри мы центрируем доску и сохраняем её квадратной.
            // Если места мало -> доска уменьшится. Если много -> увеличится до ширины экрана.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value },
                contentAlignment = Alignment.Center
            ) {
                // Сама доска
                Box(
                    modifier = Modifier
                        .aspectRatio(1f) // Всегда квадрат
                        .fillMaxSize()   // Пытается заполнить родителя (но ограничено квадратом)
                ) {
                    val combinedHighlights = remember(selectedSquare, uiState.highlights) {
                        val set = uiState.highlights.mapNotNull {
                            try { Square.fromValue(it.uppercase()) } catch (e: Exception) { null }
                        }.toMutableSet()
                        selectedSquare?.let { set.add(it) }
                        set
                    }

                    ChessBoard(
                        board = uiState.board,
                        arrows = uiState.arrows,
                        highlightedSquares = combinedHighlights,
                        onSquareClick = { square ->
                            if (uiState.isSolved || uiState.isInputLocked || uiState.isAnimatingPunishment) return@ChessBoard
                            if (selectedSquare == null) {
                                if (uiState.board.getPiece(square).pieceSide == uiState.playerSide) selectedSquare = square
                            } else {
                                if (square == selectedSquare) selectedSquare = null
                                else {
                                    viewModel.onMoveMade(Move(selectedSquare!!, square))
                                    selectedSquare = null
                                }
                            }
                        },
                        flipped = uiState.playerSide == Side.BLACK,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (uiState.isInputLocked || uiState.isAnimatingPunishment) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable(enabled = false) {}
                        )
                    }
                }
            }

            // 2. ЕДИНАЯ ОБЛАСТЬ ОБРАТНОЙ СВЯЗИ
            // Ограничиваем высоту (heightIn), чтобы текст не съедал всю доску.
            // verticalScroll позволяет прокрутить текст, если он длинный.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp) // Максимальная высота панели сообщений
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                UnifiedFeedbackPanel(uiState = uiState)
            }
        }
    }
}

// ================================================================
// ЕДИНАЯ ПАНЕЛЬ СТАТУСА (Без наложений и перекрытий)
// ================================================================

@Composable
private fun UnifiedFeedbackPanel(uiState: TrainingUiState) {
    val analysis = uiState.currentMistakeAnalysis
    val message = uiState.feedbackMessage

    // Анимация смены контента
    AnimatedContent(
        targetState = Triple(message, analysis, uiState.isSolved),
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "FeedbackTransition"
    ) { (msg, errAnalysis, isSolved) ->

        // Показываем карточку, если есть сообщение или ошибка
        if (msg != null || errAnalysis != null) {
            val isError = !isSolved && errAnalysis != null

            val (containerColor, contentColor, icon) = when {
                isSolved -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.EmojiEvents)
                isError -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Warning)
                else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // Скролл внутри карточки
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Текст сообщения
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = msg ?: "Ошибка",
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Детали ошибки (если есть)
                    if (isError && errAnalysis?.tacticalMotif != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = contentColor.copy(alpha = 0.2f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp), tint = contentColor.copy(alpha = 0.8f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Мотив: ${errAnalysis.tacticalMotif.russianName}",
                                color = contentColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// ОСТАЛЬНЫЕ КОМПОНЕНТЫ
// ================================================================

@Composable
private fun SessionHeader(stats: SessionStats, sideToMove: Side) {
    Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✔ ${stats.solved}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 16.sp)
                Spacer(Modifier.width(16.dp))
                Text("✖ ${stats.failed}", fontWeight = FontWeight.Bold, color = Color(0xFFE57373), fontSize = 16.sp)
            }
            Surface(
                color = if (sideToMove == Side.WHITE) Color.White else Color.Black,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Text(
                    text = if (sideToMove == Side.WHITE) " Белые " else " Чёрные ",
                    color = if (sideToMove == Side.WHITE) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomControlPanel(
    uiState: TrainingUiState,
    onNextExercise: () -> Unit,
    onHint: () -> Unit,
    onShowBetterMove: () -> Unit
) {
    Surface(
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Кнопка "Показать правильный ход"
            if (uiState.showBetterMoveHint && !uiState.betterMoveShown && !uiState.isSolved && !uiState.isAnimatingPunishment) {
                OutlinedButton(
                    onClick = onShowBetterMove,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Показать правильный ход")
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.isSolved) {
                Button(
                    onClick = onNextExercise,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Далее", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null)
                }
            } else {
                // Если нет ошибки, показываем Подсказку
                // Если ошибка есть, скрываем (как вы просили)
                if (uiState.currentMistakeAnalysis == null) {
                    Button(
                        onClick = onHint,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !uiState.isAnimatingPunishment && !uiState.isInputLocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (uiState.hintLevel) {
                                HintLevel.NONE -> MaterialTheme.colorScheme.primary
                                HintLevel.PIECE_HIGHLIGHT -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (uiState.hintLevel) {
                                HintLevel.NONE -> "Подсказка"
                                HintLevel.PIECE_HIGHLIGHT -> "Куда ходить?"
                                HintLevel.FULL_SOLUTION -> "Решение"
                            },
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (uiState.similarExercisesQueued > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Серия задач: ${uiState.similarExercisesQueued} шт.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}