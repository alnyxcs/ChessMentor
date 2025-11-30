package com.example.chessmentor.domain.entity

/**
 * Статус анализа партии
 */
enum class AnalysisStatus {
    PENDING,       // Ожидает анализа
    IN_PROGRESS,   // Анализируется
    COMPLETED,     // Завершён
    FAILED;        // Ошибка при анализе

    /**
     * Название на русском
     */
    fun getDisplayName(): String = when (this) {
        PENDING -> "В очереди"
        IN_PROGRESS -> "Анализируется"
        COMPLETED -> "Готово"
        FAILED -> "Ошибка"
    }

    /**
     * Иконка статуса
     */
    fun getIcon(): String = when (this) {
        PENDING -> "⏳"
        IN_PROGRESS -> "⚙️"
        COMPLETED -> "✅"
        FAILED -> "❌"
    }

    /**
     * Можно ли просматривать результаты
     */
    fun isViewable(): Boolean = this == COMPLETED

    /**
     * Завершён ли анализ (успешно или с ошибкой)
     */
    fun isFinished(): Boolean = this == COMPLETED || this == FAILED
}