// domain/repository/EngineSettingsRepository.kt
package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.EngineSettings

/**
 * Репозиторий для настроек шахматного движка
 */
interface EngineSettingsRepository {

    /**
     * Получить текущие настройки
     */
    fun getSettings(): EngineSettings

    /**
     * Сохранить настройки
     */
    fun saveSettings(settings: EngineSettings)

    /**
     * Сбросить к настройкам по умолчанию
     */
    fun resetToDefault()

    /**
     * Обновить глубину анализа
     */
    fun updateDepth(depth: Int)

    /**
     * Обновить количество потоков
     */
    fun updateThreads(threads: Int)

    /**
     * Обновить размер хеша
     */
    fun updateHashSize(hashSizeMb: Int)
}