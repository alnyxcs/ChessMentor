// presentation/ui/screen/TrainingScreen.kt
package com.example.chessmentor.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmentor.di.AppContainer
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

    var selectedSquare by remember { mutableStateOf<Square?>(null) }

    if (exercise == null) {
        LoadingOrMessageState(
            paddingValues = paddingValues,
            message = message
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Рейтинг пользователя
        if (user != null) {
            Text(
                text = "Ваш рейтинг: ${user!!.rating}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Информация о задаче
        ExerciseInfoCard(
            exerciseId = exercise!!.id,
            prompt = exercise!!.prompt,
            sideToMove = board.sideToMove
        )

        // Доска
        ChessBoard(
            board = board,
            onSquareClick = { square ->
                if (selectedSquare == null) {
                    if (board.getPiece(square).pieceSide == board.sideToMove) {
                        selectedSquare = square
                    }
                } else {
                    if (square == selectedSquare) {
                        selectedSquare = null
                    } else {
                        val move = Move(selectedSquare!!, square)
                        viewModel.onMoveMade(move)
                        selectedSquare = null
                    }
                }
            },
            highlightedSquares = if (selectedSquare != null) setOf(selectedSquare!!) else emptySet(),
            flipped = board.sideToMove == Side.BLACK
        )

        // Сообщение о результате
        if (message != null) {
            ResultMessageCard(
                message = message!!,
                isSolved = isSolved
            )
        }

        // Кнопки управления
        ControlButtons(
            isSolved = isSolved,
            onNextExercise = { viewModel.loadNextExercise() },
            onReset = { viewModel.resetPosition() },
            onHint = { /* TODO */ }
        )
    }
}

@Composable
private fun LoadingOrMessageState(
    paddingValues: PaddingValues,
    message: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        if (message != null) {
            Text(message, modifier = Modifier.padding(16.dp))
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ExerciseInfoCard(
    exerciseId: Long?,
    prompt: String,
    sideToMove: Side
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Задача #$exerciseId",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = prompt)

            Spacer(modifier = Modifier.height(8.dp))

            val sideText = if (sideToMove == Side.WHITE) {
                "⚪ Белых"
            } else {
                "⚫ Черных"
            }
            Text("Ход: $sideText", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultMessageCard(
    message: String,
    isSolved: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ControlButtons(
    isSolved: Boolean,
    onNextExercise: () -> Unit,
    onReset: () -> Unit,
    onHint: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (isSolved) {
            Button(onClick = onNextExercise) {
                Text("Следующая задача ➡️")
            }
        } else {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Сбросить")
            }

            Button(onClick = onHint) {
                Text("Подсказка")
            }
        }
    }
}