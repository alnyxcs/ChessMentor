package com.example.chessmentor.presentation.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Менеджер звуковых эффектов для шахматной доски
 */
class SoundManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var isEnabled = true

    init {
        try {
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_MUSIC,
                ToneGenerator.MAX_VOLUME / 2 // 50% громкости
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Включить/выключить звуки
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Звук обычного хода
     */
    suspend fun playMove() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Звук взятия фигуры (более громкий)
     */
    suspend fun playCapture() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Звук шаха (предупреждающий)
     */
    suspend fun playCheck() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Звук мата (победный)
     */
    suspend fun playCheckmate() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            // Двойной звук для мата
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
            Thread.sleep(200)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Освободить ресурсы
     */
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}