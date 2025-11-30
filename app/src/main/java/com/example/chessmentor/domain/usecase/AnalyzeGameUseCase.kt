package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository

/**
 * Use Case: Анализ шахматной партии
 *
 * Бизнес-логика:
 * 1. Парсинг PGN
 * 2. Анализ каждого хода движком
 * 3. Определение ошибок с учётом рейтинга игрока
 * 4. Классификация ошибок по темам
 * 5. Сохранение результатов
 */
class AnalyzeGameUseCase(
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository,
    private val userRepository: UserRepository
) {

    /**
     * Входные данные для анализа
     */
    data class Input(
        val gameId: Long
    )

    /**
     * Результат анализа
     */
    sealed class Result {
        data class Success(
            val game: Game,
            val mistakes: List<Mistake>
        ) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Результат анализа хода
     */
    private data class MoveAnalysis(
        val moveNumber: Int,
        val userMove: String,
        val bestMove: String,
        val evaluationLoss: Int,
        val fen: String
    )

    /**
     * Выполнить анализ партии
     */
    suspend fun execute(input: Input): Result {
        // Получение партии
        val game = gameRepository.findById(input.gameId)
            ?: return Result.Error("Партия не найдена")

        // Проверка статуса
        if (game.analysisStatus == AnalysisStatus.COMPLETED) {
            val mistakes = mistakeRepository.findByGameId(game.id!!)
            return Result.Success(game, mistakes)
        }

        if (game.analysisStatus == AnalysisStatus.IN_PROGRESS) {
            return Result.Error("Партия уже анализируется")
        }

        // Получение пользователя для определения уровня
        val user = userRepository.findById(game.userId)
            ?: return Result.Error("Пользователь не найден")

        // Обновление статуса на "В процессе"
        val gameInProgress = game.startAnalysis()
        gameRepository.update(gameInProgress)

        try {
            // Анализ партии
            val moveAnalyses = analyzeWithEngine(game.pgnData, game.playerColor)

            // Определение ошибок с учётом рейтинга
            val mistakes = mutableListOf<Mistake>()
            var totalEvaluationLoss = 0
            var blunders = 0
            var mistakesCount = 0
            var inaccuracies = 0

            for (analysis in moveAnalyses) {
                val mistakeType = MistakeClassifier.classify(
                    analysis.evaluationLoss,
                    user.rating
                )

                if (mistakeType != null) {
                    // Определение темы ошибки
                    val theme = detectTheme(analysis)

                    // Создание ошибки
                    val mistake = Mistake(
                        gameId = game.id!!,
                        themeId = theme.id ?: 1, // TODO: сохранить темы в БД
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

            // Расчёт точности игры
            val accuracy = calculateAccuracy(totalEvaluationLoss, moveAnalyses.size)
            val avgLoss = if (mistakes.isNotEmpty()) {
                totalEvaluationLoss / mistakes.size
            } else {
                0
            }

            // Сохранение ошибок
            val savedMistakes = mistakeRepository.saveAll(mistakes)

            // Обновление партии с результатами
            val analyzedGame = gameInProgress.completeAnalysis(
                accuracy = accuracy,
                avgLoss = avgLoss,
                blunders = blunders,
                mistakes = mistakesCount,
                inaccuracies = inaccuracies
            )

            val finalGame = gameRepository.update(analyzedGame)

            return Result.Success(finalGame, savedMistakes)

        } catch (e: Exception) {
            // В случае ошибки отмечаем анализ как проваленный
            val failedGame = gameInProgress.failAnalysis()
            gameRepository.update(failedGame)
            return Result.Error("Ошибка при анализе: ${e.message}")
        }
    }

    /**
     * Анализ партии движком
     * TODO: Интегрировать с реальным Stockfish
     */
    private  suspend fun analyzeWithEngine(pgn: String, playerColor: ChessColor): List<MoveAnalysis> {
        // Временная заглушка
        // В реальном приложении здесь будет интеграция со Stockfish

        return listOf(
            MoveAnalysis(
                moveNumber = 15,
                userMove = "Nf3",
                bestMove = "Qd2",
                evaluationLoss = 350,
                fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3"
            ),
            MoveAnalysis(
                moveNumber = 23,
                userMove = "Bxf7+",
                bestMove = "Qe2",
                evaluationLoss = 120,
                fen = "r1bqk2r/pppp1ppp/2n2n2/4p3/1bB1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
            )
        )
    }

    /**
     * Определение темы ошибки
     * TODO: Реализовать алгоритм определения темы
     */
    private  suspend fun detectTheme(analysis: MoveAnalysis): Theme {
        // Временная заглушка
        // В реальном приложении здесь будет логика определения темы

        return when {
            analysis.userMove.contains("x") -> PredefinedThemes.SACRIFICE
            analysis.moveNumber < 10 -> PredefinedThemes.OPENING_PRINCIPLES
            analysis.moveNumber > 40 -> PredefinedThemes.PAWN_ENDGAME
            else -> PredefinedThemes.PIECE_ACTIVITY
        }
    }

    /**
     * Генерация комментария к ошибке
     */
    private  suspend fun generateComment(mistakeType: MistakeType, theme: Theme): String {
        val typeComment = when (mistakeType) {
            MistakeType.BLUNDER -> "Грубая ошибка!"
            MistakeType.MISTAKE -> "Ошибка."
            MistakeType.INACCURACY -> "Неточность."
        }

        return "$typeComment ${theme.description}"
    }

    /**
     * Расчёт точности игры
     */
    private  suspend fun calculateAccuracy(totalLoss: Int, totalMoves: Int): Double {
        if (totalMoves == 0) return 100.0

        // Формула: точность = 100 - (средняя потеря / 2)
        val avgLossPerMove = totalLoss.toDouble() / totalMoves
        val accuracy = 100 - (avgLossPerMove / 2)

        return accuracy.coerceIn(0.0, 100.0)
    }
}