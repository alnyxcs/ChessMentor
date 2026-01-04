package com.example.chessmentor.data.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Реализация ChessEngine на базе локального Stockfish.
 * Адаптировано под ваш StockfishProcess (suspend функции).
 */
class StockfishEngine(context: Context) : ChessEngine {

    private val process = StockfishProcess(context)
    private val mutex = Mutex()

    private var isInitialized = false
    private var threads = 2
    private var hashSizeMB = 32

    // Кэши
    private val evalCache = object : LinkedHashMap<String, Int>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Int>): Boolean = size > 5000
    }

    private val moveCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean = size > 5000
    }

    private val lineCache = object : LinkedHashMap<String, AnalysisLine>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, AnalysisLine>): Boolean = size > 1000
    }

    companion object {
        private const val TAG = "StockfishEngine"
        const val MATE_SCORE = 100000
    }

    override suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isInitialized && process.isRunning()) {
                Log.d(TAG, "Already initialized")
                return@withContext true
            }

            Log.d(TAG, "Initializing Stockfish engine...")

            // Ваш метод process.start() возвращает Boolean
            val started = process.start()
            if (!started) {
                Log.e(TAG, "Failed to start Stockfish process")
                return@withContext false
            }

            try {
                // Отправляем настройки
                process.sendCommand("setoption name Threads value $threads")
                process.sendCommand("setoption name Hash value $hashSizeMB")
                process.sendCommand("setoption name MultiPV value 1")

                process.sendCommand("isready")
                // waitForResponse - это suspend функция в вашем классе
                val ready = process.waitForResponse("readyok", 5000)

                if (ready != null) {
                    isInitialized = true
                    Log.d(TAG, "Stockfish initialized successfully")
                    return@withContext true
                } else {
                    Log.e(TAG, "Stockfish not ready")
                    process.stop() // suspend
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization", e)
                process.stop() // suspend
                return@withContext false
            }
        }
    }

    override suspend fun evaluate(fen: String, depthLimit: Int): Int = withContext(Dispatchers.IO) {
        evalCache[fen]?.let { return@withContext it }

        mutex.withLock {
            if (!ensureReady()) return@withContext 0

            try {
                process.sendCommand("position fen $fen")
                process.sendCommand("go depth $depthLimit")

                val result = parseAnalysisResult(depthLimit)
                evalCache[fen] = result.score
                result.score

            } catch (e: Exception) {
                Log.e(TAG, "Error during evaluation", e)
                0
            }
        }
    }

    override suspend fun getBestMove(fen: String, depthLimit: Int): String? = withContext(Dispatchers.IO) {
        moveCache[fen]?.let { return@withContext it }

        mutex.withLock {
            if (!ensureReady()) return@withContext null

            try {
                process.sendCommand("position fen $fen")
                process.sendCommand("go depth $depthLimit")

                val result = parseAnalysisResult(depthLimit)
                result.move?.let { moveCache[fen] = it }
                result.move

            } catch (e: Exception) {
                Log.e(TAG, "Error finding best move", e)
                null
            }
        }
    }

    override suspend fun getBestMoveWithLine(
        fen: String,
        depthLimit: Int
    ): AnalysisLine? = withContext(Dispatchers.IO) {

        val cacheKey = "$fen:$depthLimit"
        lineCache[cacheKey]?.let { return@withContext it }

        mutex.withLock {
            if (!ensureReady()) return@withContext null

            try {
                process.sendCommand("position fen $fen")
                process.sendCommand("go depth $depthLimit")

                val result = parseAnalysisWithPV(depthLimit)
                result?.let { lineCache[cacheKey] = it }
                result

            } catch (e: Exception) {
                Log.e(TAG, "Error getting line", e)
                null
            }
        }
    }

    override fun destroy() {
        // process.stop() в вашем коде - suspend функция.
        // Вызывать её из синхронного destroy() нужно осторожно.
        // Здесь используем GlobalScope или runBlocking, так как destroy обычно вызывается при закрытии
        kotlinx.coroutines.runBlocking {
            try {
                mutex.withLock {
                    process.stop()
                    isInitialized = false
                    evalCache.clear()
                    moveCache.clear()
                    lineCache.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying engine", e)
            }
        }
    }

    override suspend fun setOption(name: String, value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (name.lowercase()) {
                "threads" -> threads = value.toIntOrNull()?.coerceIn(1, 8) ?: threads
                "hash" -> hashSizeMB = value.toIntOrNull()?.coerceIn(1, 1024) ?: hashSizeMB
            }

            if (isInitialized && process.isRunning()) {
                process.sendCommand("setoption name $name value $value")
                process.sendCommand("isready")
                process.waitForResponse("readyok", 2000)
            }
        }
    }

    private suspend fun ensureReady(): Boolean {
        if (!isInitialized || !process.isRunning()) {
            return init()
        }
        if (!process.testConnection()) { // suspend в вашем коде
            process.stop()
            return init()
        }
        return true
    }

    // --- Парсинг ---

    private suspend fun parseAnalysisResult(targetDepth: Int): SimpleAnalysisResult {
        var bestMove: String? = null
        var score = 0
        var mate: Int? = null
        var currentDepth = 0

        val startTime = System.currentTimeMillis()
        val timeout = 15000L

        // Используем readLine из вашего процесса (с таймаутом)
        while (System.currentTimeMillis() - startTime < timeout) {
            val line = process.readLine(100) ?: continue // suspend вызов

            if (line.startsWith("info depth") && !line.contains("currmove")) {
                parseInfoLine(line)?.let { info ->
                    if (info.depth >= currentDepth) {
                        currentDepth = info.depth
                        score = info.score
                        mate = info.mate
                    }
                }
            } else if (line.startsWith("bestmove")) {
                bestMove = line.substringAfter("bestmove ").substringBefore(" ").trim()
                break
            }
        }

        return SimpleAnalysisResult(bestMove, score, mate)
    }

    private suspend fun parseAnalysisWithPV(targetDepth: Int): AnalysisLine? {
        var bestMove: String? = null
        var score = 0
        var mate: Int? = null
        var pv = listOf<String>()
        var currentDepth = 0

        val startTime = System.currentTimeMillis()
        val timeout = 15000L

        while (System.currentTimeMillis() - startTime < timeout) {
            val line = process.readLine(100) ?: continue // suspend вызов

            if (line.startsWith("info") && line.contains(" pv ")) {
                val depth = Regex("""depth (\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                if (depth >= currentDepth) {
                    currentDepth = depth

                    if (line.contains("score mate")) {
                        mate = Regex("""score mate (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()
                        score = if (mate != null) {
                            if (mate > 0) MATE_SCORE - (mate * 100) else -MATE_SCORE + (abs(mate) * 100)
                        } else 0
                    } else if (line.contains("score cp")) {
                        mate = null
                        score = Regex("""score cp (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }

                    val pvString = line.substringAfter(" pv ")
                    pv = pvString.split(" ").filter { it.length in 4..5 }.take(6)
                }
            } else if (line.startsWith("bestmove")) {
                bestMove = line.substringAfter("bestmove ").substringBefore(" ").trim()
                break
            }
        }

        return if (bestMove != null) {
            AnalysisLine(
                bestMove = bestMove,
                score = score,
                mateIn = mate,
                principalVariation = if (pv.isNotEmpty()) pv else listOf(bestMove)
            )
        } else null
    }

    private fun parseInfoLine(line: String): InfoLine? {
        try {
            val depth = Regex("""depth (\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            var score = 0
            var mate: Int? = null

            if (line.contains("score mate")) {
                mate = Regex("""score mate (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()
                if (mate != null) {
                    score = if (mate > 0) MATE_SCORE - abs(mate) * 100 else -MATE_SCORE + abs(mate) * 100
                }
            } else if (line.contains("score cp")) {
                score = Regex("""score cp (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            return InfoLine(depth, score, mate)
        } catch (e: Exception) {
            return null
        }
    }

    private data class SimpleAnalysisResult(val move: String?, val score: Int, val mate: Int?)
    private data class InfoLine(val depth: Int, val score: Int, val mate: Int?)
}