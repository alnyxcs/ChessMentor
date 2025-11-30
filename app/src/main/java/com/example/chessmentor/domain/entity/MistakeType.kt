package com.example.chessmentor.domain.entity

/**
 * Тип ошибки в шахматной партии
 */
enum class MistakeType {
    BLUNDER,      // Грубая ошибка
    MISTAKE,      // Ошибка
    INACCURACY;   // Неточность

    /**
     * Описание на русском
     */
    fun getDescription(): String = when (this) {
        BLUNDER -> "Грубая ошибка"
        MISTAKE -> "Ошибка"
        INACCURACY -> "Неточность"
    }

    /**
     * Цвет для отображения (hex)
     */
    fun getColor(): String = when (this) {
        BLUNDER -> "#D32F2F"    // Красный
        MISTAKE -> "#F57C00"    // Оранжевый
        INACCURACY -> "#FBC02D"  // Жёлтый
    }

    /**
     * Эмодзи для отображения
     */
    fun getEmoji(): String = when (this) {
        BLUNDER -> "❌"
        MISTAKE -> "⚠️"
        INACCURACY -> "⚡"
    }

    /**
     * Минимальная потеря оценки для данного типа (в сантипешках)
     * Значения зависят от рейтинга игрока (позже добавим адаптивность)
     */
    fun getMinEvaluationLoss(): Int = when (this) {
        BLUNDER -> 200      // >= 200 сантипешек
        MISTAKE -> 75       // >= 75 сантипешек
        INACCURACY -> 30    // >= 30 сантипешек
    }
}