// domain/entity/ProblemTheme.kt
package com.example.chessmentor.domain.entity

/**
 * Проблемная тема для статистики
 */
data class ProblemTheme(
    val themeName: String,
    val category: String,
    val mistakeCount: Int,
    val percentage: Double = 0.0
)