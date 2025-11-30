package com.example.chessmentor.domain.entity

/**
 * Уровень мастерства шахматиста
 * Определяется на основе рейтинга Elo
 */
enum class SkillLevel {
    BEGINNER,      // 600-1199
    INTERMEDIATE,  // 1200-1799
    ADVANCED,      // 1800-2199
    EXPERT;        // 2200+

    companion object {
        /**
         * Определить уровень по рейтингу
         */
        fun fromRating(rating: Int): SkillLevel = when {
            rating < 1200 -> BEGINNER
            rating < 1800 -> INTERMEDIATE
            rating < 2200 -> ADVANCED
            else -> EXPERT
        }
    }

    /**
     * Диапазон рейтингов для уровня
     */
    fun getRatingRange(): IntRange = when (this) {
        BEGINNER -> 600..1199
        INTERMEDIATE -> 1200..1799
        ADVANCED -> 1800..2199
        EXPERT -> 2200..2500
    }

    /**
     * Отображаемое название на русском
     */
    fun getDisplayName(): String = when (this) {
        BEGINNER -> "Новичок"
        INTERMEDIATE -> "Любитель"
        ADVANCED -> "Продвинутый"
        EXPERT -> "Эксперт"
    }
}