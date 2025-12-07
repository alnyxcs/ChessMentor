package com.example.chessmentor.domain.usecase

import android.util.Log
import com.example.chessmentor.data.engine.ChessEngine
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class AnalyzeGameUseCase(
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository,
    private val userRepository: UserRepository,
    private val chessEngine: ChessEngine
) {
    companion object {
        private const val TAG = "AnalyzeGameUseCase"
        private const val ANALYSIS_DEPTH = 15
    }

    data class Input(val gameId: Long)

    sealed class Result {
        data class Success(
            val game: Game,
            val mistakes: List<Mistake>,
            val evaluations: List<Int>
        ) : Result()
        data class Error(val message: String) : Result()
    }

    private data class MoveAnalysis(
        val moveNumber: Int,
        val color: ChessColor,
        val userMove: String,
        val bestMove: String,
        val evaluationLoss: Int,
        val evaluationAfter: Int,
        val fen: String
    )

    suspend fun execute(input: Input): Result = withContext(Dispatchers.Default) {
        val game = gameRepository.findById(input.gameId)
            ?: return@withContext Result.Error("Партия не найдена")

        if (game.analysisStatus == AnalysisStatus.COMPLETED) {
            val savedMistakes = mistakeRepository.findByGameId(game.id!!)
            return@withContext Result.Success(game, savedMistakes, emptyList())
        }

        if (game.analysisStatus == AnalysisStatus.IN_PROGRESS) {
            return@withContext Result.Error("Партия уже анализируется")
        }

        val user = userRepository.findById(game.userId)
            ?: return@withContext Result.Error("Пользователь не найден")

        val gameInProgress = game.startAnalysis()
        gameRepository.update(gameInProgress)

        try {
            val engineReady = chessEngine.init()
            if (!engineReady) {
                return@withContext Result.Error("Не удалось запустить шахматный движок")
            }

            Log.i(TAG, "Engine initialized, starting analysis...")

            val (moveAnalyses, evaluations) = analyzeWithEngine(game.pgnData, game.playerColor)

            Log.i(TAG, "Analysis complete: ${moveAnalyses.size} move analyses, ${evaluations.size} evaluations")

            val mistakes = mutableListOf<Mistake>()
            var totalEvaluationLoss = 0
            var blunders = 0
            var mistakesCount = 0
            var inaccuracies = 0

            for (analysis in moveAnalyses) {
                val mistakeType = MistakeClassifier.classify(analysis.evaluationLoss, user.rating)
                if (mistakeType != null) {
                    val theme = detectTheme(analysis)
                    val mistake = Mistake(
                        gameId = game.id!!,
                        themeId = theme.id ?: 1,
                        moveNumber = analysis.moveNumber,
                        color = analysis.color,
                        evaluationLoss = analysis.evaluationLoss,
                        mistakeType = mistakeType,
                        bestMove = analysis.bestMove,
                        userMove = analysis.userMove,
                        comment = generateComment(mistakeType, theme),
                        fenBefore = analysis.fen
                    )
                    mistakes.add(mistake)
                    totalEvaluationLoss += analysis.evaluationLoss

                    when (mistakeType) {
                        MistakeType.BLUNDER -> blunders++
                        MistakeType.MISTAKE -> mistakesCount++
                        MistakeType.INACCURACY -> inaccuracies++
                    }
                }
            }

            val totalHalfMoves = evaluations.size - 1
            val playerMovesCount = (totalHalfMoves + 1) / 2

            val accuracy = calculateAccuracy(totalEvaluationLoss, playerMovesCount)
            val avgLoss = if (playerMovesCount > 0) totalEvaluationLoss / playerMovesCount else 0

            val savedMistakes = mistakeRepository.saveAll(mistakes)

            val analyzedGame = gameInProgress.completeAnalysis(
                accuracy = accuracy,
                avgLoss = avgLoss,
                blunders = blunders,
                mistakes = mistakesCount,
                inaccuracies = inaccuracies
            )

            val finalGame = gameRepository.update(analyzedGame)

            Log.i(TAG, "Analysis complete: accuracy=${"%.1f".format(accuracy)}%, " +
                    "mistakes=${mistakes.size}, playerMoves=$playerMovesCount, totalLoss=$totalEvaluationLoss")

            Result.Success(finalGame, savedMistakes, evaluations)

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            val failedGame = gameInProgress.failAnalysis()
            gameRepository.update(failedGame)
            Result.Error("Ошибка анализа: ${e.message}")
        } finally {
            try {
                chessEngine.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up engine", e)
            }
        }
    }

    private fun calculateAccuracy(totalLoss: Int, playerMoves: Int): Double {
        if (playerMoves == 0) return 100.0

        val avgLossPerMove = totalLoss.toDouble() / playerMoves

        val accuracy = when {
            avgLossPerMove <= 0 -> 100.0
            avgLossPerMove <= 25 -> 100.0 - avgLossPerMove * 0.6
            avgLossPerMove <= 50 -> 85.0 - (avgLossPerMove - 25) * 0.6
            avgLossPerMove <= 100 -> 70.0 - (avgLossPerMove - 50) * 0.4
            avgLossPerMove <= 200 -> 50.0 - (avgLossPerMove - 100) * 0.25
            else -> 25.0 - (avgLossPerMove - 200) * 0.1
        }

        return accuracy.coerceIn(0.0, 100.0)
    }

    /**
     * Анализ партии с ПРАВИЛЬНОЙ нормализацией оценок
     *
     * ВАЖНО: Stockfish возвращает оценку с точки зрения СТОРОНЫ, КОТОРАЯ ХОДИТ СЛЕДУЮЩЕЙ!
     *
     * После хода белых → оценка с точки зрения чёрных
     * После хода чёрных → оценка с точки зрения белых
     *
     * Нам нужна единая шкала "с точки зрения белых", поэтому:
     * - После хода белых: инвертируем знак (было для чёрных, делаем для белых)
     * - После хода чёрных: оставляем как есть (уже для белых)
     */
    private suspend fun analyzeWithEngine(
        pgn: String,
        playerColor: ChessColor
    ): Pair<List<MoveAnalysis>, List<Int>> = withContext(Dispatchers.IO) {

        val moveAnalyses = mutableListOf<MoveAnalysis>()
        val evaluations = mutableListOf<Int>()

        try {
            val finalPgn = if (pgn.trim().startsWith("[")) {
                pgn
            } else {
                val moves = pgn.replace(Regex("(1-0|0-1|1/2-1/2)\\s*$"), "").trim()
                """
[Event "Imported Game"]
[Site "ChessMentor"]
[Date "????.??.??"]
[Round "?"]
[White "?"]
[Black "?"]
[Result "*"]

$moves
            """.trimIndent()
            }

            val tempFile = File.createTempFile("analyze", ".pgn")
            tempFile.writeText(finalPgn)

            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()
            tempFile.delete()

            if (pgnHolder.games.isEmpty()) {
                return@withContext Pair(emptyList(), emptyList())
            }

            val game = pgnHolder.games[0]
            val board = Board()
            val halfMoves = game.halfMoves

            // Стартовая оценка
            val initialFen = board.fen
            val initialEvalRaw = chessEngine.evaluate(initialFen, depthLimit = ANALYSIS_DEPTH)
            evaluations.add(initialEvalRaw)  // Начальная позиция — ход белых, не инвертируем

            Log.i(TAG, "=== START ANALYSIS ===")
            Log.i(TAG, "Player color: $playerColor")
            Log.i(TAG, "Starting FEN: $initialFen")
            Log.i(TAG, "Starting eval: $initialEvalRaw (white to move, no inversion)")

            halfMoves.forEachIndexed { index, move ->
                val moveNumber = (index / 2) + 1
                val isWhiteMove = index % 2 == 0
                val isPlayerMove = (playerColor == ChessColor.WHITE && isWhiteMove) ||
                        (playerColor == ChessColor.BLACK && !isWhiteMove)

                val fenBefore = board.fen

                // Чей сейчас ход (до выполнения move)?
                val sideToMoveBefore = if (fenBefore.contains(" w ")) "WHITE" else "BLACK"

                // Делаем ход
                board.doMove(move)
                val fenAfter = board.fen

                // Чей ход после?
                val sideToMoveAfter = if (fenAfter.contains(" w ")) "WHITE" else "BLACK"

                try {
                    val evalAfterRaw = chessEngine.evaluate(fenAfter, depthLimit = ANALYSIS_DEPTH)

                    // Stockfish даёт оценку с точки зрения стороны, которая ХОДИТ СЛЕДУЮЩЕЙ
                    // После хода белых → ходят чёрные → оценка с т.з. чёрных → инвертируем
                    // После хода чёрных → ходят белые → оценка с т.з. белых → не инвертируем
                    val needsInversion = sideToMoveAfter == "BLACK"
                    val evalAfterNormalized = if (needsInversion) -evalAfterRaw else evalAfterRaw

                    evaluations.add(evalAfterNormalized)

                    // Подробный лог для первых 10 ходов
                    if (index < 10) {
                        Log.d(TAG, "--- Move ${index + 1}: ${move.san} ---")
                        Log.d(TAG, "  Before: $sideToMoveBefore to move")
                        Log.d(TAG, "  After: $sideToMoveAfter to move")
                        Log.d(TAG, "  Raw eval: $evalAfterRaw")
                        Log.d(TAG, "  Needs inversion: $needsInversion")
                        Log.d(TAG, "  Normalized: $evalAfterNormalized")
                    }

                    if (isPlayerMove) {
                        val evalBefore = evaluations[evaluations.size - 2]
                        val evalAfter = evalAfterNormalized

                        val loss = calculateEvaluationLoss(evalBefore, evalAfter, playerColor)

                        val bestMoveUci = chessEngine.getBestMove(fenBefore, depthLimit = ANALYSIS_DEPTH) ?: ""

                        Log.v(TAG, "Move #$moveNumber (${if (isWhiteMove) "White" else "Black"}): " +
                                "eval $evalBefore → $evalAfter, loss=$loss")

                        val isMateBefore = ChessEngine.isMateScore(evalBefore)
                        val isMateAfter = ChessEngine.isMateScore(evalAfter)

                        var finalLoss = loss

                        if ((isMateBefore || isMateAfter) && finalLoss < 3000) {
                            val wasWinning = (playerColor == ChessColor.WHITE && evalBefore > 0) ||
                                    (playerColor == ChessColor.BLACK && evalBefore < 0)

                            if (isMateBefore && wasWinning && !isMateAfter) {
                                finalLoss = 3000
                            }
                        }

                        val hasDifferentBestMove = bestMoveUci.isNotEmpty() && bestMoveUci != move.toString()
                        val isSignificantLoss = finalLoss > 50
                        val isLargeLoss = finalLoss > 80
                        val isHugeLoss = finalLoss > 150
                        val isNotOpening = moveNumber > 5

                        val shouldDetectMistake = when {
                            isHugeLoss -> true
                            isLargeLoss && isNotOpening -> true
                            isSignificantLoss && hasDifferentBestMove && isNotOpening -> true
                            else -> false
                        }

                        if (shouldDetectMistake) {
                            moveAnalyses.add(
                                MoveAnalysis(
                                    moveNumber = moveNumber,
                                    color = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK,
                                    userMove = move.toString(),
                                    bestMove = bestMoveUci,
                                    evaluationLoss = finalLoss,
                                    evaluationAfter = evalAfter,
                                    fen = fenBefore
                                )
                            )
                            Log.d(TAG, "⚠️ Mistake: move #$moveNumber ${move.san}, loss=$finalLoss")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing move $moveNumber: ${e.message}")
                    evaluations.add(evaluations.lastOrNull() ?: 0)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error: ${e.message}", e)
        }

        // Итоговая линия
        Log.i(TAG, "=== FINAL EVALUATION LINE ===")
        evaluations.forEachIndexed { idx, eval ->
            if (idx <= 10 || idx == evaluations.size - 1) {
                Log.d(TAG, "[$idx]: $eval cp (${ChessEngine.formatEvaluation(eval)})")
            }
        }

        Log.i(TAG, "Analysis finished: ${moveAnalyses.size} mistakes, ${evaluations.size} evaluations")

        Pair(moveAnalyses, evaluations)
    }

    /**
     * Потеря оценки для игрока (используем уже нормализованные оценки)
     *
     * Все оценки уже с точки зрения БЕЛЫХ:
     * + = хорошо белым, - = хорошо чёрным
     */
    private fun calculateEvaluationLoss(
        evalBefore: Int,
        evalAfter: Int,
        playerColor: ChessColor
    ): Int {
        val loss = when (playerColor) {
            ChessColor.WHITE -> {
                // Для белых: потеря = насколько уменьшилась оценка
                evalBefore - evalAfter
            }
            ChessColor.BLACK -> {
                // Для чёрных: потеря = насколько увеличилась оценка (стало лучше белым)
                evalAfter - evalBefore
            }
        }
        return loss.coerceAtLeast(0)
    }

    private fun detectTheme(analysis: MoveAnalysis): Theme {
        return when {
            analysis.userMove.contains("x") -> PredefinedThemes.SACRIFICE
            analysis.moveNumber < 10 -> PredefinedThemes.OPENING_PRINCIPLES
            analysis.moveNumber > 40 -> PredefinedThemes.PAWN_ENDGAME
            else -> PredefinedThemes.PIECE_ACTIVITY
        }
    }

    private fun generateComment(mistakeType: MistakeType, theme: Theme): String {
        val typeComment = when (mistakeType) {
            MistakeType.BLUNDER -> "Грубая ошибка!"
            MistakeType.MISTAKE -> "Ошибка."
            MistakeType.INACCURACY -> "Неточность."
        }
        return "$typeComment ${theme.description}"
    }
}

object MistakeClassifier {
    fun classify(evaluationLoss: Int, userRating: Int): MistakeType? {
        val factor = when {
            userRating < 1200 -> 1.5
            userRating < 1800 -> 1.0
            else -> 0.7
        }

        return when {
            evaluationLoss >= (250 * factor).toInt() -> MistakeType.BLUNDER
            evaluationLoss >= (100 * factor).toInt() -> MistakeType.MISTAKE
            evaluationLoss >= (50 * factor).toInt() -> MistakeType.INACCURACY
            else -> null
        }
    }
}