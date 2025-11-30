package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder

/**
 * ViewModel для управления шахматной доской и навигацией по ходам
 */
class BoardViewModel : ViewModel() {

    // Текущая доска
    val board = mutableStateOf(Board())

    // Список всех ходов партии
    val moves = mutableStateListOf<Move>()

    // Список всех ходов в нотации (для отображения)
    val moveNotations = mutableStateListOf<String>()

    // Текущий индекс хода
    val currentMoveIndex = mutableIntStateOf(-1)

    // Оценки позиций (индекс -> оценка в сантипешках)
    val evaluations = mutableStateListOf<Int>()

    // Ошибки в партии
    val mistakes = mutableStateListOf<Mistake>()

    // Подсвеченные поля
    val highlightedSquares = mutableStateOf<Set<Square>>(emptySet())

    // Последний ход (from -> to)
    val lastMove = mutableStateOf<Pair<Square, Square>?>(null)

    var soundManager: com.example.chessmentor.presentation.ui.components.SoundManager? = null
    /**
     * Загрузить партию из PGN
     */
    /**
     * Загрузить партию из PGN
     */
    /**
     * Загрузить партию из PGN
     */
    /**
     * Загрузить партию из PGN
     */
    /**
     * Загрузить партию из PGN
     */
    /**
     * Загрузить партию из PGN
     */
    fun loadGame(game: Game, gameMistakes: List<Mistake>) {
        try {
            println("=== LOADING GAME ===")

            // Очищаем предыдущее состояние
            moves.clear()
            moveNotations.clear()
            evaluations.clear()
            mistakes.clear()
            mistakes.addAll(gameMistakes)

            // Создаём временный файл для PGN
            val tempFile = java.io.File.createTempFile("chess_game", ".pgn")
            tempFile.writeText(game.pgnData)

            println("Temp file created: ${tempFile.absolutePath}")
            println("PGN content: ${game.pgnData}")

            // Создаём PgnHolder с путём к файлу
            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()

            // Удаляем временный файл
            tempFile.delete()

            if (pgnHolder.games.isEmpty()) {
                println("ERROR: No games found in PGN")
                return
            }

            val pgnGame = pgnHolder.games[0]

            // Получаем ходы напрямую из PGN игры
            val halfMoves = pgnGame.halfMoves

            println("Half moves from PGN: ${halfMoves.size}")

            // Добавляем ходы напрямую
            moves.addAll(halfMoves)

            // Создаём нотации для отображения
            var moveNumber = 1
            halfMoves.forEachIndexed { index, move ->
                val notation = if (index % 2 == 0) {
                    // Ход белых
                    "$moveNumber. ${move.san}"
                } else {
                    // Ход чёрных
                    move.san
                }
                moveNotations.add(notation)

                // Увеличиваем номер хода после хода чёрных
                if (index % 2 == 1) {
                    moveNumber++
                }

                println("Move $index: $notation (from=${move.from}, to=${move.to})")
            }

            println("Total moves loaded: ${moves.size}")
            println("Move notations count: ${moveNotations.size}")

            // Генерируем оценки
            generateEvaluations()

            // Возвращаемся в начальную позицию
            goToStart()

            println("=== GAME LOADED SUCCESSFULLY ===")

        } catch (e: Exception) {
            println("ERROR loading game: ${e.message}")
            e.printStackTrace()
        }
    }
    /**
     * Генерация оценок позиций (заглушка)
     * В реальном приложении здесь будет интеграция с Stockfish
     */

    private fun generateEvaluations() {
        evaluations.clear()
        evaluations.add(0) // Начальная позиция = равенство

        // Для каждого хода генерируем оценку на основе ошибок
        var currentEval = 0
        moves.forEachIndexed { index, _ ->
            // Проверяем есть ли ошибка на этом ходу
            val moveNumber = (index / 2) + 1
            val isWhiteMove = index % 2 == 0
            val mistake = mistakes.find { it.moveNumber == moveNumber && isWhiteMove }

            if (mistake != null) {
                // Если ход белых - оценка падает (минус), если черных - растёт (плюс)
                currentEval += if (isWhiteMove) {
                    -mistake.evaluationLoss
                } else {
                    mistake.evaluationLoss
                }
            } else {
                // Небольшое случайное изменение для реалистичности
                currentEval += if (isWhiteMove) {
                    (-10..10).random()
                } else {
                    (-10..10).random()
                }
            }

            // Ограничиваем диапазон
            currentEval = currentEval.coerceIn(-1500, 1500)
            evaluations.add(currentEval)
        }
    }

    /**
     * Определение критических моментов партии
     */

    /**
     * Перейти к началу партии
     */
    fun goToStart() {
        board.value = Board()
        currentMoveIndex.intValue = -1
        lastMove.value = null
        highlightedSquares.value = emptySet()
    }

    /**
     * Перейти к концу партии
     */
    fun goToEnd() {
        goToStart()

        try {
            for (i in moves.indices) {
                board.value.doMove(moves[i])
            }
            currentMoveIndex.intValue = moves.size - 1

            if (moves.isNotEmpty()) {
                val lastMoveObj = moves.last()
                lastMove.value = Pair(lastMoveObj.from, lastMoveObj.to)
            }

            updateHighlights()

            println("Went to end, total moves: ${moves.size}")
        } catch (e: Exception) {
            println("ERROR going to end: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Перейти на один ход вперёд
     */
    fun goToNextMove() {
        if (currentMoveIndex.intValue < moves.size - 1) {
            currentMoveIndex.intValue++
            val move = moves[currentMoveIndex.intValue]

            try {
                board.value.doMove(move)
                lastMove.value = Pair(move.from, move.to)
                updateHighlights()

                println("Next move: ${move.san}, index: ${currentMoveIndex.intValue}")
            } catch (e: Exception) {
                println("ERROR doing move: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Перейти на один ход назад
     */
    fun goToPreviousMove() {
        if (currentMoveIndex.intValue >= 0) {
            board.value.undoMove()
            currentMoveIndex.intValue--

            if (currentMoveIndex.intValue >= 0) {
                val move = moves[currentMoveIndex.intValue]
                lastMove.value = Pair(move.from, move.to)
            } else {
                lastMove.value = null
            }

            updateHighlights()
        }
    }

    /**
     * Обновить подсветку полей
     */
    private fun updateHighlights() {
        val highlights = mutableSetOf<Square>()

        // Подсвечиваем поле ошибочного хода
        if (currentMoveIndex.intValue >= 0) {
            val moveNumber = (currentMoveIndex.intValue / 2) + 1
            val mistake = mistakes.find { it.moveNumber == moveNumber }

            if (mistake != null && currentMoveIndex.intValue % 2 == 0) {
                // Это ход с ошибкой
                lastMove.value?.second?.let { highlights.add(it) }
            }
        }

        highlightedSquares.value = highlights
    }

    /**
     * Получить текущую оценку позиции
     */
    fun getCurrentEvaluation(): Int {
        val index = currentMoveIndex.intValue + 1
        return if (index in evaluations.indices) {
            evaluations[index]
        } else {
            0
        }
    }

    /**
     * Получить текущий ход в нотации
     */
    fun getCurrentMoveNotation(): String {
        return if (currentMoveIndex.intValue >= 0 && currentMoveIndex.intValue < moveNotations.size) {
            moveNotations[currentMoveIndex.intValue]
        } else {
            "Начальная позиция"
        }
    }

    /**
     * Получить информацию об ошибке на текущем ходу
     */
    fun getCurrentMistake(): Mistake? {
        if (currentMoveIndex.intValue >= 0) {
            val moveNumber = (currentMoveIndex.intValue / 2) + 1
            return mistakes.find { it.moveNumber == moveNumber }
        }
        return null
    }
    /**
     * Определение ключевых моментов партии
     */
    fun getKeyMoments(): List<com.example.chessmentor.presentation.ui.components.KeyMoment> {
        val keyMoments = mutableListOf<com.example.chessmentor.presentation.ui.components.KeyMoment>()

        evaluations.forEachIndexed { index, evaluation ->
            if (index == 0) return@forEachIndexed

            val previousEval = evaluations[index - 1]
            val change = evaluation - previousEval
            val absChange = kotlin.math.abs(change)

            // Определяем качество
            val quality = when {
                // Если ход за нас и оценка выросла
                (index % 2 == 0 && change > 500) || (index % 2 != 0 && change < -500) ->
                    com.example.chessmentor.presentation.ui.components.MoveQuality.BRILLIANT
                (index % 2 == 0 && change > 200) || (index % 2 != 0 && change < -200) ->
                    com.example.chessmentor.presentation.ui.components.MoveQuality.GREAT_MOVE

                // Ошибки
                absChange > 500 -> com.example.chessmentor.presentation.ui.components.MoveQuality.BLUNDER
                absChange > 200 -> com.example.chessmentor.presentation.ui.components.MoveQuality.MISTAKE
                absChange > 100 -> com.example.chessmentor.presentation.ui.components.MoveQuality.INACCURACY

                else -> null
            }

            if (quality != null && index - 1 < moves.size) {
                keyMoments.add(
                    com.example.chessmentor.presentation.ui.components.KeyMoment(
                        moveIndex = index - 1,
                        san = moves[index - 1].san,
                        quality = quality,
                        evaluation = evaluation,
                        evaluationChange = change
                    )
                )
            }
        }

        return keyMoments
    }
    /**
     * Перейти к конкретному ходу
     */
    fun goToMove(moveIndex: Int) {
        if (moveIndex < -1 || moveIndex >= moves.size) return

        // Возвращаемся в начало
        goToStart()

        // Проигрываем ходы до нужного
        for (i in 0..moveIndex) {
            goToNextMove()
        }
    }
}