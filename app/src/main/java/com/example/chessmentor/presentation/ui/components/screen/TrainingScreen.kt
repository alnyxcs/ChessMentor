package com.example.chessmentor.presentation.ui.components.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.presentation.ui.components.ChessBoard
import com.example.chessmentor.presentation.viewmodel.TrainingViewModel
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

    val exercise by viewModel.currentExercise
    val board by viewModel.boardState
    val message by viewModel.message
    val user by viewModel.currentUser
    val isSolved by viewModel.isSolved
    val isLoading by viewModel.isLoading
    val showHint by viewModel.showHint
    val sessionStats by viewModel.sessionStats

    var selectedSquare by remember(exercise?.id) { mutableStateOf<Square?>(null) }

    if (exercise == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator() else Text("Загрузка...")
        }
        return
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        bottomBar = {
            // НИЖНЯЯ ПАНЕЛЬ С КНОПКАМИ (Фиксирована)
            Surface(shadowElevation = 12.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ControlButtons(
                        isSolved = isSolved,
                        showHint = showHint,
                        onNextExercise = { selectedSquare = null; viewModel.loadNextExercise() },
                        onReset = { selectedSquare = null; viewModel.resetPosition() },
                        onHint = { viewModel.toggleHint() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // КОМПАКТНАЯ ШАПКА
            CompactHeader(
                user = user,
                stats = sessionStats,
                sideToMove = board.sideToMove,
                rating = exercise!!.rating
            )

            // Текст задания
            Text(
                text = exercise!!.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 2
            )

            // КОНТЕЙНЕР ДОСКИ И ВСПЛЫВАЮЩИХ СООБЩЕНИЙ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Занимаем всё доступное место
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Слой 1: Доска
                ChessBoard(
                    board = board,
                    onSquareClick = { square ->
                        if (isSolved) return@ChessBoard
                        if (selectedSquare == null) {
                            if (board.getPiece(square).pieceSide == board.sideToMove) selectedSquare = square
                        } else {
                            if (square == selectedSquare) selectedSquare = null
                            else {
                                viewModel.onMoveMade(Move(selectedSquare!!, square))
                                selectedSquare = null
                            }
                        }
                    },
                    highlightedSquares = if (selectedSquare != null) setOf(selectedSquare!!) else emptySet(),
                    flipped = viewModel.playerSide.value == Side.BLACK,
                    modifier = Modifier.fillMaxSize()
                )

                // Слой 2: Всплывающее сообщение поверх доски
                if (message != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter) // Прибиваем к низу доски
                            .padding(16.dp)
                            .fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = message!!,
                            modifier = Modifier.padding(12.dp),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactHeader(
    user: User?,
    stats: TrainingViewModel.SessionStats,
    sideToMove: Side,
    rating: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Рейтинг: ${user?.rating ?: 0}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row {
                Text("✔ ${stats.solved} ", fontSize = 12.sp, color = Color(0xFF4CAF50))
                Text("✖ ${stats.failed}", fontSize = 12.sp, color = Color(0xFFF44336))
            }
        }

        Surface(
            color = if(sideToMove == Side.WHITE) Color.White else Color.Black,
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
        ) {
            Text(
                text = if (sideToMove == Side.WHITE) " Ход Белых " else " Ход Черных ",
                color = if(sideToMove == Side.WHITE) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(4.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("Задача", fontSize = 10.sp, color = Color.Gray)
            Text("$rating", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ControlButtons(
    isSolved: Boolean,
    showHint: Boolean,
    onNextExercise: () -> Unit,
    onReset: () -> Unit,
    onHint: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (isSolved) {
            Button(
                onClick = onNextExercise,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Следующая задача", fontSize = 16.sp)
            }
        } else {
            // Кнопка Сброс удалена
            Button(
                onClick = onHint,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showHint) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (showHint) Icons.Default.VisibilityOff else Icons.Default.Lightbulb, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (showHint) "Скрыть подсказку" else "Подсказка", fontSize = 16.sp)
            }
        }
    }
}