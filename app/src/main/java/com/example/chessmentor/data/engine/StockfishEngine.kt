// app/src/main/java/com/example/chessmentor/data/engine/StockfishEngine.kt
package com.example.chessmentor.data.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Реализация ChessEngine на базе локального Stockfish
 */
class StockfishEngine(context: Context) : ChessEngine {

    private val process = StockfishProcess(context)
    private val mutex = Mutex()

    private var isInitialized = false
    private var threads = 2
    private var hashSizeMB = 32

    // Кэш для ускорения повторных запросов
    private val evalCache = object : LinkedHashMap<String, Int>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Int>): Boolean {
            return size > 5000
        }
    }

    private val moveCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean {
            return size > 5000
        }
    }

    companion object {
        private const val TAG = "StockfishEngine"
        private const val DEFAULT_DEPTH = 18

        // Матовые константы
        const val MATE_SCORE = 100000
        const val MATE_THRESHOLD = 90000  // Всё что >= этого — мат
    }

    override suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isInitialized && process.isRunning()) {
                Log.d(TAG, "Already initialized")
                return@withContext true
            }

            Log.d(TAG, "Initializing Stockfish engine...")

            val started = process.start()
            if (!started) {
                Log.e(TAG, "Failed to start Stockfish process")
                return@withContext false
            }

            try {
                process.sendCommand("setoption name Threads value $threads")
                process.sendCommand("setoption name Hash value $hashSizeMB")
                process.sendCommand("setoption name MultiPV value 1")

                process.sendCommand("isready")
                val ready = process.waitForResponse("readyok", 5000)

                if (ready != null) {
                    isInitialized = true
                    Log.d(TAG, "Stockfish initialized successfully")
                    return@withContext true
                } else {
                    Log.e(TAG, "Stockfish not ready")
                    process.stop()
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization", e)
                process.stop()
                return@withContext false
            }
        }
    }

    override suspend fun evaluate(fen: String, depthLimit: Int): Int = withContext(Dispatchers.IO) {
        evalCache[fen]?.let {
            Log.d(TAG, "Cache hit for evaluation: $fen -> $it")
            return@withContext it
        }

        mutex.withLock {
            if (!ensureReady()) {
                Log.e(TAG, "Engine not ready for evaluation")
                return@withContext 0
            }

            try {
                process.sendCommand("position fen $fen")
                process.sendCommand("go depth $depthLimit")

                val result = parseAnalysisResult(depthLimit)

                evalCache[fen] = result.score

                Log.d(TAG, "Evaluation complete: $fen -> ${result.score} (mate=${result.mate})")
                result.score

            } catch (e: Exception) {
                Log.e(TAG, "Error during evaluation", e)
                0
            }
        }
    }

    override suspend fun getBestMove(fen: String, depthLimit: Int): String? = withContext(Dispatchers.IO) {
        moveCache[fen]?.let {
            Log.d(TAG, "Cache hit for best move: $fen -> $it")
            return@withContext it
        }

        mutex.withLock {
            if (!ensureReady()) {
                Log.e(TAG, "Engine not ready for best move")
                return@withContext null
            }

            try {
                process.sendCommand("position fen $fen")
                process.sendCommand("go depth $depthLimit")

                val result = parseAnalysisResult(depthLimit)

                result.move?.let { moveCache[fen] = it }

                Log.d(TAG, "Best move found: $fen -> ${result.move}")
                result.move

            } catch (e: Exception) {
                Log.e(TAG, "Error finding best move", e)
                null
            }
        }
    }

    override fun destroy() {
        Log.d(TAG, "Destroying engine...")

        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                process.stop()
                isInitialized = false
                evalCache.clear()
                moveCache.clear()
            }
        }

        Log.d(TAG, "Engine destroyed")
    }

    private suspend fun ensureReady(): Boolean {
        if (!isInitialized || !process.isRunning()) {
            Log.d(TAG, "Engine not ready, initializing...")
            return init()
        }

        if (!process.testConnection()) {
            Log.w(TAG, "Engine not responding, restarting...")
            process.stop()
            return init()
        }

        return true
    }

    private suspend fun parseAnalysisResult(targetDepth: Int): AnalysisResult {
        var bestMove: String? = null
        var score = 0
        var mate: Int? = null
        var currentDepth = 0

        val startTime = System.currentTimeMillis()
        val timeout = 30000L

        while (System.currentTimeMillis() - startTime < timeout) {
            val line = process.readLine(100)

            if (line != null) {
                when {
                    line.startsWith("info depth") && !line.contains("currmove") -> {
                        parseInfoLine(line)?.let { info ->
                            if (info.depth >= currentDepth) {
                                currentDepth = info.depth
                                score = info.score
                                mate = info.mate
                            }
                        }
                    }

                    line.startsWith("bestmove") -> {
                        bestMove = line
                            .substringAfter("bestmove ")
                            .substringBefore(" ")
                            .trim()

                        Log.d(TAG, "Analysis complete: depth=$currentDepth, score=$score, mate=$mate, move=$bestMove")
                        return AnalysisResult(bestMove, score, mate)
                    }
                }
            }

            if (currentDepth >= targetDepth) {
                kotlinx.coroutines.delay(100)
            }
        }

        Log.w(TAG, "Analysis timeout reached")
        return AnalysisResult(bestMove, score, mate)
    }

    /**
     * Парсинг строки info от Stockfish
     *
     * Формат матовых оценок:
     * - "score mate 3" = мат в 3 хода за сторону которая ходит
     * - "score mate -3" = мат в 3 хода против стороны которая ходит
     *
     * Преобразование в score:
     * - mate 3 → +99700 (MATE_SCORE - 3*100)
     * - mate -3 → -99700 (-MATE_SCORE + 3*100)
     */
    private fun parseInfoLine(line: String): InfoLine? {
        try {
            val depth = Regex("""depth (\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null

            var score = 0
            var mate: Int? = null

            when {
                line.contains("score mate") -> {
                    // Парсим мат
                    mate = Regex("""score mate (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()

                    if (mate != null) {
                        // Преобразуем мат в числовую оценку
                        // Чем меньше ходов до мата, тем ближе к MATE_SCORE
                        score = if (mate > 0) {
                            // Мат за нас: +99700, +99800, +99900...
                            MATE_SCORE - abs(mate) * 100
                        } else {
                            // Мат против нас: -99700, -99800, -99900...
                            -MATE_SCORE + abs(mate) * 100
                        }

                        Log.d(TAG, "Mate parsed: mate in $mate moves -> score=$score")
                    }
                }

                line.contains("score cp") -> {
                    // Обычная оценка в сантипешках
                    score = Regex("""score cp (-?\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
            }

            return InfoLine(depth, score, mate)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse info line: $line", e)
            return null
        }
    }

    fun setThreads(threads: Int) {
        this.threads = threads.coerceIn(1, 8)
        if (isInitialized && process.isRunning()) {
            process.sendCommand("setoption name Threads value ${this.threads}")
        }
    }

    fun setHashSize(sizeMB: Int) {
        this.hashSizeMB = sizeMB.coerceIn(1, 1024)
        if (isInitialized && process.isRunning()) {
            process.sendCommand("setoption name Hash value ${this.hashSizeMB}")
        }
    }

    private data class AnalysisResult(
        val move: String?,
        val score: Int,
        val mate: Int?
    )

    private data class InfoLine(
        val depth: Int,
        val score: Int,
        val mate: Int?
    )
}