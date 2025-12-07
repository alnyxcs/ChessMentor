// domain/entity/MistakeType.kt
package com.example.chessmentor.domain.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class MistakeType {
    BLUNDER,
    MISTAKE,
    INACCURACY;

    fun getDisplayName(): String = when (this) {
        BLUNDER -> "Зевок"
        MISTAKE -> "Ошибка"
        INACCURACY -> "Неточность"
    }

    fun getEmoji(): String = when (this) {
        BLUNDER -> "??"
        MISTAKE -> "?"
        INACCURACY -> "?!"
    }

    fun getIcon(): ImageVector = when (this) {
        BLUNDER -> Icons.Default.Cancel
        MISTAKE -> Icons.Default.Error
        INACCURACY -> Icons.Default.Warning
    }

    fun getColor(): Color = when (this) {
        BLUNDER -> Color(0xFFD32F2F)
        MISTAKE -> Color(0xFFF57C00)
        INACCURACY -> Color(0xFFFBC02D)
    }
}