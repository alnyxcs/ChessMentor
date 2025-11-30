package com.example.chessmentor.presentation.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.presentation.ui.components.MoveQuality
import androidx.compose.ui.graphics.Color

// Маппинг ошибок
fun MistakeType.getIcon(): ImageVector = when(this) {
    MistakeType.BLUNDER -> Icons.Default.Cancel // Крестик
    MistakeType.MISTAKE -> Icons.Default.Error // Восклицательный знак
    MistakeType.INACCURACY -> Icons.Default.Help // Вопрос (или Bolt для молнии)
}

// Маппинг качества хода
fun MoveQuality.getIcon(): ImageVector = when(this) {
    MoveQuality.BRILLIANT -> Icons.Default.Diamond // Бриллиант
    MoveQuality.GREAT_MOVE -> Icons.Default.ThumbUp // Палец вверх
    MoveQuality.BEST_MOVE -> Icons.Default.Star // Звезда
    MoveQuality.GOOD -> Icons.Default.CheckCircle // Галочка
    MoveQuality.INACCURACY -> Icons.Default.HelpOutline
    MoveQuality.MISTAKE -> Icons.Default.ErrorOutline
    MoveQuality.BLUNDER -> Icons.Default.Cancel
}

fun MistakeType.getComposeColor(): Color {
    return Color(android.graphics.Color.parseColor(this.getColor()))
}