package com.example.chessmentor.domain.usecase

import com.example.chessmentor.data.engine.StockfishProcess // <-- ИСПОЛЬЗУЕМ ЭТОТ КЛАСС
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.pgn.PgnHolder

/**
 * Use Case: Анализ шахматной партии
 */
class AnalyzeGameUseCase(
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository,
    private val userRepository: UserRepository,
    private val engine: StockfishProcess // <-- ИЗМЕНЕН ТИП
) {

    data class Input(val gameId: Long)

    sealed class Result {
        data class Success(val game: Game, val mistakes: List<Mistake>) : Result()
        data class Error(val message: String) : Result()
    }

    private data class MoveAnalysis(
        val moveNumber: Int,
        val userMove: String,
        val bestMove: String,
        val evaluationLoss: Int,
        val fen: String
    )

    suspend fun execute(input: Input): Result {
        // ... (код получения игры и проверок - без изменений)
        val game = gameRepository.findById(input.gameId) ?: return Result.Error("Партия не найдена")
        if (game.analysisStatus == AnalysisStatus.COMPLETED) return Result.Success(game, mistakeRepository.findByGameId(game.id!!))
        if (game.analysisStatus == AnalysisStatus.IN_PROGRESS) return Result.Error("Партия уже анализируется")
        val user = userRepository.findById(game.userId) ?: return Result.Error("Пользователь не найден")
        val gameInProgress = game.startAnalysis()
        gameRepository.update(gameInProgress)

        try {
            // Реальный анализ
            val moveAnalyses = analyzeWithEngine(game.pgnData, game.playerColor)

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

            val accuracy = calculateAccuracy(totalEvaluationLoss, moveAnalyses.size)
            val avgLoss = if (mistakes.isNotEmpty()) totalEvaluationLoss / mistakes.size else 0

            val savedMistakes = mistakeRepository.saveAll(mistakes)

            val analyzedGame = gameInProgress.completeAnalysis(
                accuracy = accuracy,
                avgLoss = avgLoss,
                blunders = blunders,
                mistakes = mistakesCount,
                inaccuracies = inaccuracies
            )

            val finalGame = gameRepository.update(analyzedGame)

            // Не забываем закрыть движок
            engine.close()

            return Result.Success(finalGame, savedMistakes)

        } catch (e: Exception) {
            val failedGame = gameInProgress.failAnalysis()
            gameRepository.update(failedGame)
            engine.close()
            return Result.Error("Ошибка при анализе: ${e.message}")
        }
    }

    private suspend fun analyzeWithEngine(pgn: String, playerColor: ChessColor): List<MoveAnalysis> {
        val moveAnalyses = mutableListOf<MoveAnalysis>()

        try {
            // Парсим PGN (как в BoardViewModel)
            // Создаём временный файл, так как PgnHolder требует файл
            val tempFile = java.io.File.createTempFile("analyze", ".pgn")
            tempFile.writeText(pgn)

            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()
            tempFile.delete()

            if (pgnHolder.games.isEmpty()) return emptyList()

            val game = pgnHolder.games[0]
            val board = Board()
            val halfMoves = game.halfMoves

            halfMoves.forEachIndexed { index, move ->
                val fenBefore = board.fen
                board.doMove(move)
                val moveNumber = (index / 2) + 1

                val isPlayerMove = if (playerColor == ChessColor.WHITE) index % 2 == 0 else index % 2 != 0

                if (isPlayerMove) {
                    // Запрос к движку (возвращает строку "e2e4")
                    val bestMoveUci = engine.getBestMove(fenBefore, depth = 10)

                    // Сравниваем (Move.toString() возвращает UCI, например "e2e4")
                    if (bestMoveUci.isNotEmpty() && bestMoveUci != move.toString()) {

                        // Попытка конвертации UCI в SAN для отображения
                        // (Для простоты пока оставим UCI или используем board для конвертации)

                        moveAnalyses.add(MoveAnalysis(
                            moveNumber = moveNumber,
                            userMove = move.san,
                            bestMove = bestMoveUci,
                            evaluationLoss = 100, // Заглушка оценки
                            fen = fenBefore
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return moveAnalyses
    }

    // ... (detectTheme, generateComment, calculateAccuracy - без изменений) ...
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