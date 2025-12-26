// presentation/ui/screen/TrainingScreen.kt
package com.example.chessmentor.presentation.ui.components.screen

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

    var selectedSquare by remember { mutableStateOf<Square?>(null) }

    if (exercise == null) {
        LoadingOrMessageState(
            paddingValues = paddingValues,
            message = message,
            isLoading = isLoading
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Статистика сессии и рейтинг
        SessionStatsCard(
            user = user,
            stats = sessionStats
        )

        // Информация о задаче
        ExerciseInfoCard(
            exerciseId = exercise!!.id,
            prompt = exercise!!.prompt,
            sideToMove = board.sideToMove,
            rating = exercise!!.rating
        )

        // Доска
        ChessBoard(
            board = board,
            onSquareClick = { square ->
                if (isSolved) return@ChessBoard
                
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
                isSolved = isSolved,
                showHint = showHint
            )
        }

        // Кнопки управления
        ControlButtons(
            isSolved = isSolved,
            showHint = showHint,
            onNextExercise = { 
                selectedSquare = null
                viewModel.loadNextExercise() 
            },
            onReset = { 
                selectedSquare = null
                viewModel.resetPosition() 
            },
            onHint = { viewModel.toggleHint() }
        )
    }
}

@Composable
private fun LoadingOrMessageState(
    paddingValues: PaddingValues,
    message: String?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Загрузка задачи...")
                }
            }
            message != null -> {
                Card(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun SessionStatsCard(
    user: User?,
    stats: TrainingViewModel.SessionStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Рейтинг
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Рейтинг",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${user?.rating ?: 0}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )
            
            // Решено
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Решено",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.solved}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Провалено
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ошибок",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.failed}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Среднее время
            if (stats.solved > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ср. время",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stats.averageTime}с",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseInfoCard(
    exerciseId: Long?,
    prompt: String,
    sideToMove: Side,
    rating: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Задача #$exerciseId",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Сложность
                Surface(
                    color = when {
                        rating < 1400 -> Color(0xFF4CAF50)
                        rating < 1700 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when {
                            rating < 1400 -> "Легко"
                            rating < 1700 -> "Средне"
                            else -> "Сложно"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sideText = if (sideToMove == Side.WHITE) "⚪ Белых" else "⚫ Черных"
                Text(
                    text = "Ход: $sideText",
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "• Рейтинг: $rating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultMessageCard(
    message: String,
    isSolved: Boolean,
    showHint: Boolean
) {
    val backgroundColor = when {
        isSolved -> Color(0xFFE8F5E9)
        showHint -> Color(0xFFFFF9C4)
        else -> Color(0xFFFFEBEE)
    }
    
    val icon = when {
        isSolved -> Icons.Default.CheckCircle
        showHint -> Icons.Default.Lightbulb
        else -> Icons.Default.Error
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSolved) {
            Button(
                onClick = onNextExercise,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Следующая задача")
            }
        } else {
            Button(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сброс")
            }

            Button(
                onClick = onHint,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showHint) 
                        MaterialTheme.colorScheme.tertiary 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (showHint) Icons.Default.VisibilityOff else Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showHint) "Скрыть" else "Подсказка")
            }
        }
    }
}

