// domain/entity/EngineSettings.kt
package com.example.chessmentor.domain.entity

/**
 * Настройки шахматного движка Stockfish
 */
data class EngineSettings(
    /** Глубина анализа (количество полуходов) */
    val depth: Int = DEFAULT_DEPTH,

    /** Количество потоков CPU */
    val threads: Int = DEFAULT_THREADS,

    /** Размер хеш-таблицы в MB */
    val hashSizeMb: Int = DEFAULT_HASH_SIZE
) {
    companion object {
        // Значения по умолчанию
        const val DEFAULT_DEPTH = 15
        const val DEFAULT_THREADS = 2
        const val DEFAULT_HASH_SIZE = 32

        // Минимальные значения
        const val MIN_DEPTH = 10
        const val MIN_THREADS = 1
        const val MIN_HASH_SIZE = 16

        // Максимальные значения
        const val MAX_DEPTH = 25
        const val MAX_THREADS = 8
        const val MAX_HASH_SIZE = 256

        /**
         * Настройки по умолчанию
         */
        fun default() = EngineSettings()

        /**
         * Быстрый анализ (для слабых устройств)
         */
        fun fast() = EngineSettings(
            depth = 12,
            threads = 1,
            hashSizeMb = 16
        )

        /**
         * Глубокий анализ (для мощных устройств)
         */
        fun deep() = EngineSettings(
            depth = 22,
            threads = 4,
            hashSizeMb = 128
        )
    }

    /**
     * Валидация настроек
     */
    fun validate(): EngineSettings {
        return copy(
            depth = depth.coerceIn(MIN_DEPTH, MAX_DEPTH),
            threads = threads.coerceIn(MIN_THREADS, MAX_THREADS),
            hashSizeMb = hashSizeMb.coerceIn(MIN_HASH_SIZE, MAX_HASH_SIZE)
        )
    }

    /**
     * Примерное время анализа одного хода (в секундах)
     */
    fun estimatedTimePerMove(): Double {
        return when {
            depth <= 12 -> 0.3
            depth <= 15 -> 0.8
            depth <= 18 -> 1.5
            depth <= 22 -> 3.0
            else -> 5.0
        }
    }

    /**
     * Описание профиля для UI
     */
    fun getProfileName(): String {
        return when {
            depth <= 12 -> "Быстрый"
            depth <= 16 -> "Стандартный"
            depth <= 20 -> "Глубокий"
            else -> "Максимальный"
        }
    }
}