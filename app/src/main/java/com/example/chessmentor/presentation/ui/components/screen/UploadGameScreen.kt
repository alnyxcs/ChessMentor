// presentation/ui/screen/UploadGameScreen.kt
package com.example.chessmentor.presentation.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.example.chessmentor.presentation.viewmodel.GameViewModel

@Composable
fun UploadGameScreen(
    paddingValues: PaddingValues,
    gameViewModel: GameViewModel,
    onGameUploaded: () -> Unit
) {
    val pgnInput by gameViewModel.pgnInput
    val selectedColor by gameViewModel.selectedColor
    val timeControl by gameViewModel.timeControl
    val message by gameViewModel.message
    val isLoading by gameViewModel.isLoading

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        item {
            UploadHeader()
        }

        // Сообщение
        if (message.isNotEmpty()) {
            item {
                UploadMessage(
                    message = message,
                    isLoading = isLoading,
                    onDismiss = { gameViewModel.clearMessage() }
                )
            }
        }

        // Форма
        item {
            UploadForm(
                pgnInput = pgnInput,
                selectedColor = selectedColor,
                timeControl = timeControl,
                isLoading = isLoading,
                onPgnChange = { gameViewModel.pgnInput.value = it },
                onColorChange = { gameViewModel.selectedColor.value = it },
                onTimeControlChange = { gameViewModel.timeControl.value = it },
                onUpload = { gameViewModel.uploadGame() }
            )
        }
    }
}

@Composable
private fun UploadHeader() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Загрузить партию",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Вставьте PGN для анализа",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UploadMessage(
    message: String,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val isError = message.startsWith("❌") || message.startsWith("Ошибка")
    val cleanMessage = message.replace("✅ ", "").replace("❌ ", "")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = cleanMessage,
                modifier = Modifier.weight(1f),
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onTertiaryContainer
            )
            if (!isLoading) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
        }
    }
}

@Composable
private fun UploadForm(
    pgnInput: String,
    selectedColor: ChessColor,
    timeControl: String,
    isLoading: Boolean,
    onPgnChange: (String) -> Unit,
    onColorChange: (ChessColor) -> Unit,
    onTimeControlChange: (String) -> Unit,
    onUpload: () -> Unit
) {
    // ОТЛАДКА
    println("UploadForm: selectedColor = $selectedColor")

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PGN поле
            OutlinedTextField(
                value = pgnInput,
                onValueChange = onPgnChange,
                label = { Text("PGN партии") },
                placeholder = { Text("1. e4 e5 2. Nf3 Nc6 ...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10,
                enabled = !isLoading,
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
            )

            // Выбор цвета
            Text("Ваш цвет:", fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // БЕЛЫЕ
                FilterChip(
                    selected = selectedColor == ChessColor.BLACK,
                    onClick = {
                        println("WHITE clicked, current: $selectedColor")
                        onColorChange(ChessColor.WHITE)
                    },
                    label = {
                        Text("Белые ${if (selectedColor == ChessColor.WHITE) "✓" else ""}")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.border(1.dp, Color.Gray, CircleShape)
                        )
                    },
                    enabled = !isLoading
                )

                Spacer(Modifier.width(8.dp))

                // ЧЁРНЫЕ
                FilterChip(
                    selected = selectedColor == ChessColor.WHITE,
                    onClick = {
                        println("BLACK clicked, current: $selectedColor")
                        onColorChange(ChessColor.BLACK)
                    },
                    label = {
                        Text("Чёрные ${if (selectedColor == ChessColor.BLACK) "✓" else ""}")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    },
                    enabled = !isLoading
                )
            }

            // Контроль времени
            OutlinedTextField(
                value = timeControl,
                onValueChange = onTimeControlChange,
                label = { Text("Контроль времени (опционально)") },
                placeholder = { Text("5+3, 10+0, 15+10") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
            )

            // Кнопка загрузки
            Button(
                onClick = onUpload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading && pgnInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Анализируем...")
                } else {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить и анализировать")
                }
            }

            HorizontalDivider()

            // Подсказка
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Пример PGN:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
