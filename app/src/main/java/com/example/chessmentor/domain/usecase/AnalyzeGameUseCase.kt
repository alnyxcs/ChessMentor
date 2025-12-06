// domain/usecase/AnalyzeGameUseCase.kt
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

/**
 * Use Case: Анализ шахматной партии с использованием локального Stockfish движка
 */
class AnalyzeGameUseCase(
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository,
    private val userRepository: UserRepository,
    private val chessEngine: ChessEngine
) {
    companion object {
        private const val TAG = "AnalyzeGameUseCase"
        private const val ANALYSIS_DEPTH = 15  // Глубина для локального движка
    }

    data class Input(val gameId: Long)

    sealed class Result {
        data class Success(val game: Game, val mistakes: List<Mistake>) : Result()
        data class Error(val message: String) : Result()
    }

    private data class MoveAnalysis(
        val moveNumber: Int,
        val color: ChessColor,
        val userMove: String,
        val bestMove: String,
        val evaluationLoss: Int,
        val fen: String
    )

    suspend fun execute(input: Input): Result = withContext(Dispatchers.Default) {
        // Получаем данные
        val game = gameRepository.findById(input.gameId)
            ?: return@withContext Result.Error("Партия не найдена")

        if (game.analysisStatus == AnalysisStatus.COMPLETED) {
            return@withContext Result.Success(game, mistakeRepository.findByGameId(game.id!!))
        }

        if (game.analysisStatus == AnalysisStatus.IN_PROGRESS) {
            return@withContext Result.Error("Партия уже анализируется")
        }

        val user = userRepository.findById(game.userId)
            ?: return@withContext Result.Error("Пользователь не найден")

        // Начинаем анализ
        val gameInProgress = game.startAnalysis()
        gameRepository.update(gameInProgress)

        try {
            // === ИНИЦИАЛИЗАЦИЯ ДВИЖКА ===
            val engineReady = chessEngine.init()

            if (!engineReady) {
                return@withContext Result.Error(
                    "Не удалось запустить шахматный движок. Попробуйте перезапустить приложение."
                )
            }

            Log.i(TAG, "Engine initialized, starting analysis...")

            // Анализируем партию
            val moveAnalyses = analyzeWithEngine(game.pgnData, game.playerColor)

            // Обрабатываем результаты
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

            // Вычисляем статистику
            val accuracy = calculateAccuracy(totalEvaluationLoss, moveAnalyses.size)
            val avgLoss = if (mistakes.isNotEmpty()) totalEvaluationLoss / mistakes.size else 0

            // Сохраняем результаты
            val savedMistakes = mistakeRepository.saveAll(mistakes)

            val analyzedGame = gameInProgress.completeAnalysis(
                accuracy = accuracy,
                avgLoss = avgLoss,
                blunders = blunders,
                mistakes = mistakesCount,
                inaccuracies = inaccuracies
            )

            val finalGame = gameRepository.update(analyzedGame)

            Log.i(TAG, "Analysis complete: accuracy=$accuracy%, mistakes=${mistakes.size}")

            Result.Success(finalGame, savedMistakes)

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)

            // Отмечаем ошибку анализа
            val failedGame = gameInProgress.failAnalysis()
            gameRepository.update(failedGame)

            // Определяем тип ошибки для пользователя
            val userMessage = when {
                e.message?.contains("Process") == true ->
                    "Ошибка запуска движка. Перезапустите приложение."
                e.message?.contains("timeout") == true ->
                    "Анализ занял слишком много времени. Попробуйте ещё раз."
                e.message?.contains("PGN") == true ->
                    "Ошибка в формате партии. Проверьте PGN."
                else ->
                    "Неизвестная ошибка анализа: ${e.message}"
            }

            Result.Error(userMessage)
        } finally {
            // Освобождаем ресурсы движка после анализа
            try {
                chessEngine.destroy()
                Log.d(TAG, "Engine resources cleaned up")
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up engine", e)
            }
        }
    }

    private suspend fun analyzeWithEngine(
        pgn: String,
        playerColor: ChessColor
    ): List<MoveAnalysis> = withContext(Dispatchers.IO) {
        val moveAnalyses = mutableListOf<MoveAnalysis>()

        try {
            Log.d(TAG, "Raw PGN input length: ${pgn.length}")

            val finalPgn = if (pgn.trim().startsWith("[")) {
                Log.d(TAG, "PGN has tags. Using as is.")
                pgn
            } else {
                Log.d(TAG, "PGN has no tags. Wrapping...")
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

            // Сохраняем во временный файл
            val tempFile = File.createTempFile("analyze", ".pgn")
            tempFile.writeText(finalPgn)
            Log.d(TAG, "Saved PGN to ${tempFile.absolutePath}")

            // Парсим
            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()
            tempFile.delete()

            if (pgnHolder.games.isEmpty()) {
                Log.e(TAG, "No games found in PGN! Check format.")
                Log.d(TAG, "Failed PGN content: ${finalPgn.take(500)}...")
                return@withContext emptyList()
            }

            val game = pgnHolder.games[0]
            val board = Board()
            val halfMoves = game.halfMoves

            Log.i(TAG, "PGN Parsed successfully. Moves found: ${halfMoves.size}")

            // Анализируем каждый ход
            halfMoves.forEachIndexed { index, move ->
                val moveNumber = (index / 2) + 1
                val isWhiteMove = index % 2 == 0
                val isPlayerMove = if (playerColor == ChessColor.WHITE) isWhiteMove else !isWhiteMove

                val fenBefore = board.fen

                if (isPlayerMove) {
                    try {
                        // 1. Оценка ДО хода
                        val rawEvalBefore = chessEngine.evaluate(fenBefore, depthLimit = ANALYSIS_DEPTH)
                        val evalBeforeWhite = normalizeScore(rawEvalBefore, isWhiteMove)

                        // 2. Лучший ход
                        val bestMoveUci = chessEngine.getBestMove(fenBefore, depthLimit = ANALYSIS_DEPTH) ?: ""

                        // 3. Ход игрока
                        board.doMove(move)

                        // 4. Оценка ПОСЛЕ хода
                        val fenAfter = board.fen
                        val rawEvalAfter = chessEngine.evaluate(fenAfter, depthLimit = ANALYSIS_DEPTH)
                        val evalAfterWhite = normalizeScore(rawEvalAfter, !isWhiteMove)

                        // 5. Вычисляем потерю оценки
                        val diff = evalBeforeWhite - evalAfterWhite
                        val evaluationLoss = if (playerColor == ChessColor.WHITE) diff else -diff
                        val finalLoss = evaluationLoss.coerceAtLeast(0)

                        val playerMoveUci = move.toString()

                        Log.v(TAG, "Move #$moveNumber (${if(isWhiteMove) "W" else "B"}): " +
                                "Player=$playerMoveUci Best=$bestMoveUci")
                        Log.v(TAG, "   Eval (White): $evalBeforeWhite -> $evalAfterWhite (Loss: $finalLoss)")

                        // === ОБРАБОТКА МАТОВЫХ ПОЗИЦИЙ ===
                        val isMatePositionBefore = abs(evalBeforeWhite) > 5000
                        val isMatePositionAfter = abs(evalAfterWhite) > 5000

                        if ((isMatePositionBefore || isMatePositionAfter) && finalLoss > 1000) {
                            // Пропущен мат или упущена матовая атака
                            Log.d(TAG, "   --> MATE POSITION! Critical blunder!")

                            val mateInMoves = if (isMatePositionBefore) {
                                abs((evalBeforeWhite - 10000) / 10)
                            } else {
                                abs((evalAfterWhite - 10000) / 10)
                            }

                            moveAnalyses.add(MoveAnalysis(
                                moveNumber = moveNumber,
                                color = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK,
                                userMove = playerMoveUci,
                                bestMove = if (bestMoveUci.isNotEmpty()) bestMoveUci else "Мат в $mateInMoves",
                                evaluationLoss = 3000,  // Фиксированная большая потеря
                                fen = fenBefore
                            ))
                        } else {
                            // === ОБЫЧНАЯ ДЕТЕКЦИЯ ОШИБОК ===
                            val hasDifferentBestMove = bestMoveUci.isNotEmpty() && bestMoveUci != playerMoveUci
                            val isSignificantLoss = finalLoss > 50      // >0.5 пешки
                            val isLargeLoss = finalLoss > 80            // >0.8 пешки
                            val isHugeLoss = finalLoss > 150            // >1.5 пешки
                            val isNotOpening = moveNumber > 5           // После дебюта

                            val shouldDetectMistake = when {
                                isHugeLoss -> true
                                isLargeLoss && isNotOpening -> true
                                isSignificantLoss && hasDifferentBestMove && isNotOpening -> true
                                else -> false
                            }

                            if (shouldDetectMistake) {
                                Log.d(TAG, "   --> MISTAKE DETECTED! Loss: $finalLoss cp")
                                moveAnalyses.add(MoveAnalysis(
                                    moveNumber = moveNumber,
                                    color = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK,
                                    userMove = playerMoveUci,
                                    bestMove = if (bestMoveUci.isNotEmpty()) bestMoveUci else "?",
                                    evaluationLoss = finalLoss,
                                    fen = fenBefore
                                ))
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error analyzing move $moveNumber: ${e.message}")
                    }
                } else {
                    // Ход соперника - просто делаем его на доске
                    board.doMove(move)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in analyzeWithEngine: ${e.message}", e)
        }

        Log.i(TAG, "Analysis finished. Mistakes found: ${moveAnalyses.size}")
        moveAnalyses
    }

    /**
     * Приводит оценку движка к абсолютной шкале (с точки зрения Белых).
     */
    private fun normalizeScore(score: Int, isWhiteTurn: Boolean): Int {
        return if (isWhiteTurn) score else -score
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

    private fun calculateAccuracy(totalLoss: Int, totalMoves: Int): Double {
        if (totalMoves == 0) return 100.0
        val avgLossPerMove = totalLoss.toDouble() / totalMoves
        val accuracy = 100 - (avgLossPerMove / 2)
        return accuracy.coerceIn(0.0, 100.0)
    }
}

/**
 * Классификатор ошибок на основе потери оценки и рейтинга игрока
 */
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