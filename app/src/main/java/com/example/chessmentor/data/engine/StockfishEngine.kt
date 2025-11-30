package com.example.chessmentor.data.engine

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockfishProcess(context: Context) {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null

    init {
        initialize(context)
    }

    private fun initialize(context: Context) {
        try {
            // Имя файла во внутренней памяти (куда скопируем)
            val execFile = File(context.filesDir, "stockfish_exec")

            // Копируем из assets, если файла нет или он старый (можно добавить проверку версий)
            if (!execFile.exists()) {
                context.assets.open("stockfish/stockfish").use { input -> // Путь в assets
                    FileOutputStream(execFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // ВАЖНО: Делаем файл исполняемым
                execFile.setExecutable(true)
            }

            // Запускаем процесс
            val processBuilder = ProcessBuilder(execFile.absolutePath)
            process = processBuilder.start()

            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = OutputStreamWriter(process!!.outputStream)

            // Инициализация UCI
            sendCommand("uci")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendCommand(command: String) {
        try {
            writer?.write("$command\n")
            writer?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getBestMove(fen: String, depth: Int = 10): String = withContext(Dispatchers.IO) {
        try {
            // Очистка буфера
            while (reader?.ready() == true) { reader?.readLine() }

            sendCommand("position fen $fen")
            sendCommand("go depth $depth")

            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line!!.startsWith("bestmove")) {
                    // Возвращает: "e2e4" (без "bestmove ")
                    return@withContext line!!.split(" ")[1]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext ""
    }

    fun close() {
        try {
            sendCommand("quit")
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}