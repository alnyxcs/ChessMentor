// app/src/main/java/com/example/chessmentor/data/engine/StockfishProcess.kt
package com.example.chessmentor.data.engine

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Низкоуровневое управление процессом Stockfish
 */
class StockfishProcess(private val context: Context) {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    companion object {
        private const val TAG = "StockfishProcess"
        private const val STOCKFISH_FILENAME = "libstockfish.so"
        private const val DEFAULT_TIMEOUT_MS = 5000L
    }

    /**
     * Запуск процесса Stockfish
     */
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isRunning()) {
                Log.d(TAG, "Process already running")
                return@withContext true
            }

            // Получаем путь к нативной библиотеке
            val stockfishPath = getStockfishPath()

            if (!File(stockfishPath).exists()) {
                Log.e(TAG, "Stockfish binary not found at: $stockfishPath")
                return@withContext false
            }

            Log.d(TAG, "Starting Stockfish from: $stockfishPath")

            // Запускаем процесс
            val processBuilder = ProcessBuilder(stockfishPath)
            processBuilder.redirectErrorStream(true)

            process = processBuilder.start()

            // Инициализируем потоки
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = PrintWriter(process!!.outputStream, true)

            // Проверяем что процесс запустился
            if (!isRunning()) {
                Log.e(TAG, "Process failed to start")
                return@withContext false
            }

            // Отправляем команду UCI и ждём ответа
            sendCommand("uci")
            val response = waitForResponse("uciok", 3000)

            if (response != null) {
                Log.d(TAG, "Stockfish started successfully")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to get UCI response")
                stop()
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting process", e)
            stop()
            return@withContext false
        }
    }

    /**
     * Остановка процесса
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping Stockfish process")

            // Пытаемся корректно завершить
            sendCommand("quit")

            // Даём время на завершение
            kotlinx.coroutines.delay(100)

            // Закрываем потоки
            writer?.close()
            reader?.close()

            // Уничтожаем процесс если всё ещё живой
            process?.destroy()

            // Ждём завершения
            process?.waitFor()

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping process", e)
        } finally {
            process = null
            reader = null
            writer = null
        }
    }

    /**
     * Проверка состояния процесса
     */
    fun isRunning(): Boolean {
        return process?.isAlive == true
    }

    /**
     * Отправка команды движку
     */
    fun sendCommand(command: String) {
        if (!isRunning()) {
            Log.w(TAG, "Cannot send command - process not running")
            return
        }

        Log.d(TAG, "→ $command")
        writer?.println(command)
        writer?.flush()
    }

    /**
     * Чтение одной строки с таймаутом
     */
    suspend fun readLine(timeoutMs: Long = DEFAULT_TIMEOUT_MS): String? = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                suspendCoroutine<String?> { continuation ->
                    val line = reader?.readLine()
                    if (line != null) {
                        Log.d(TAG, "← $line")
                    }
                    continuation.resume(line)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Read timeout or error", e)
            null
        }
    }

    /**
     * Ожидание конкретной строки в ответе
     */
    suspend fun waitForResponse(
        expected: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): String? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val response = StringBuilder()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val line = readLine(500) // Читаем с небольшим таймаутом

            if (line != null) {
                response.appendLine(line)

                if (line.contains(expected)) {
                    return@withContext response.toString()
                }
            }

            // Проверяем что процесс ещё жив
            if (!isRunning()) {
                Log.e(TAG, "Process died while waiting for response")
                return@withContext null
            }
        }

        Log.w(TAG, "Timeout waiting for: $expected")
        return@withContext null
    }

    /**
     * Чтение всех доступных строк
     */
    suspend fun readAllAvailable(maxLines: Int = 100): List<String> = withContext(Dispatchers.IO) {
        val lines = mutableListOf<String>()

        try {
            while (lines.size < maxLines && reader?.ready() == true) {
                val line = reader?.readLine()
                if (line != null) {
                    Log.d(TAG, "← $line")
                    lines.add(line)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading available lines", e)
        }

        lines
    }

    /**
     * Получение пути к бинарнику Stockfish
     */
    private fun getStockfishPath(): String {
        // Android размещает нативные библиотеки здесь
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val stockfishPath = "$nativeLibDir/$STOCKFISH_FILENAME"

        Log.d(TAG, "Native library directory: $nativeLibDir")
        Log.d(TAG, "Stockfish path: $stockfishPath")

        // Проверяем существование файла
        val file = File(stockfishPath)
        if (file.exists()) {
            Log.d(TAG, "Stockfish binary found: ${file.length()} bytes")
            Log.d(TAG, "Can execute: ${file.canExecute()}")
        } else {
            Log.e(TAG, "Stockfish binary NOT found at: $stockfishPath")

            // Пытаемся найти в альтернативных местах
            val altPaths = listOf(
                "${context.filesDir}/stockfish",
                "${context.cacheDir}/stockfish",
                "${context.applicationInfo.dataDir}/lib/$STOCKFISH_FILENAME"
            )

            for (altPath in altPaths) {
                if (File(altPath).exists()) {
                    Log.d(TAG, "Found at alternative path: $altPath")
                    return altPath
                }
            }
        }

        return stockfishPath
    }

    /**
     * Тест работоспособности
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isRunning() && !start()) {
                return@withContext false
            }

            sendCommand("isready")
            val response = waitForResponse("readyok", 2000)

            return@withContext response != null

        } catch (e: Exception) {
            Log.e(TAG, "Test failed", e)
            false
        }
    }
}