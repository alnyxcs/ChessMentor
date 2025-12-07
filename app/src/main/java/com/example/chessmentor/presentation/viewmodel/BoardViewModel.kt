// presentation/viewmodel/BoardViewModel.kt
package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.KeyMoment
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MoveQuality
import com.example.chessmentor.presentation.ui.components.SoundManager
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import java.io.File
import kotlin.math.abs

class BoardViewModel : ViewModel() {

    val board = mutableStateOf(Board())
    val moves = mutableStateListOf<Move>()
    val moveNotations = mutableStateListOf<String>()
    val currentMoveIndex = mutableIntStateOf(-1)
    val evaluations = mutableStateListOf<Int>()
    val mistakes = mutableStateListOf<Mistake>()
    val highlightedSquares = mutableStateOf<Set<Square>>(emptySet())
    val lastMove = mutableStateOf<Pair<Square, Square>?>(null)

    private var playerColor: ChessColor = ChessColor.WHITE

    var soundManager: SoundManager? = null

    companion object {
        private const val TAG = "BoardViewModel"

        // Пороги для определения качества ходов (в сантипешках)
        // Эти значения откалиброваны под типичную игру
        private const val BRILLIANT_THRESHOLD = 200    // Улучшение на 2+ пешки в плохой позиции
        private const val GREAT_MOVE_THRESHOLD = 100   // Улучшение на 1+ пешку
        private const val GOOD_MOVE_THRESHOLD = 30     // Небольшое улучшение

        private const val INACCURACY_THRESHOLD = 50    // Потеря 0.5 пешки
        private const val MISTAKE_THRESHOLD = 100      // Потеря 1 пешки
        private const val BLUNDER_THRESHOLD = 200      // Потеря 2+ пешек
        private const val MISSED_WIN_THRESHOLD = 300   // Упустил выигрыш 3+ пешек

        // Порог "плохой позиции" для определения блестящих ходов
        private const val BAD_POSITION_THRESHOLD = 150 // -1.5 пешки и хуже
    }

    fun loadGame(game: Game, gameMistakes: List<Mistake>, realEvaluations: List<Int> = emptyList()) {
        try {
            println("=== LOADING GAME ===")

            playerColor = game.playerColor

            moves.clear()
            moveNotations.clear()
            evaluations.clear()
            mistakes.clear()
            mistakes.addAll(gameMistakes)

            val tempFile = File.createTempFile("chess_game", ".pgn")
            tempFile.writeText(game.pgnData)

            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()
            tempFile.delete()

            if (pgnHolder.games.isEmpty()) {
                println("ERROR: No games found in PGN")
                return
            }

            val pgnGame = pgnHolder.games[0]
            val halfMoves = pgnGame.halfMoves

            moves.addAll(halfMoves)

            var moveNumber = 1
            halfMoves.forEachIndexed { index, move ->
                val notation = if (index % 2 == 0) {
                    "$moveNumber. ${move.san}"
                } else {
                    "${moveNumber}... ${move.san}"
                }
                moveNotations.add(notation)

                if (index % 2 == 1) {
                    moveNumber++
                }
            }

            if (realEvaluations.isNotEmpty()) {
                evaluations.addAll(realEvaluations)
                println("✅ Loaded ${realEvaluations.size} REAL evaluations")

                // Отладка: выводим первые 10 оценок
                println("First evaluations: ${realEvaluations.take(10)}")
            } else {
                generateEvaluations()
                println("⚠️ Generated ${evaluations.size} MOCK evaluations")
            }

            goToStart()

            println("=== GAME LOADED: ${moves.size} moves, ${evaluations.size} evals ===")

        } catch (e: Exception) {
            println("ERROR loading game: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun generateEvaluations() {
        evaluations.clear()
        evaluations.add(0)

        var currentEval = 0
        moves.forEachIndexed { index, _ ->
            val moveNumber = (index / 2) + 1
            val isWhiteMove = index % 2 == 0
            val currentColor = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK
            val mistake = mistakes.find { it.moveNumber == moveNumber && it.color == currentColor }

            if (mistake != null) {
                currentEval += if (isWhiteMove) {
                    -mistake.evaluationLoss
                } else {
                    mistake.evaluationLoss
                }
            } else {
                currentEval += (-10..10).random()
            }

            currentEval = currentEval.coerceIn(-1500, 1500)
            evaluations.add(currentEval)
        }
    }

    fun goToStart() {
        board.value = Board()
        currentMoveIndex.intValue = -1
        lastMove.value = null
        highlightedSquares.value = emptySet()
    }

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
        } catch (e: Exception) {
            println("ERROR going to end: ${e.message}")
            e.printStackTrace()
        }
    }

    fun goToNextMove() {
        if (currentMoveIndex.intValue < moves.size - 1) {
            currentMoveIndex.intValue++
            val move = moves[currentMoveIndex.intValue]

            try {
                board.value.doMove(move)
                lastMove.value = Pair(move.from, move.to)
                updateHighlights()
            } catch (e: Exception) {
                println("ERROR doing move: ${e.message}")
                e.printStackTrace()
            }
        }
    }

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

    fun goToMove(moveIndex: Int) {
        if (moveIndex < -1 || moveIndex >= moves.size) return

        goToStart()

        for (i in 0..moveIndex) {
            if (i < moves.size) {
                currentMoveIndex.intValue = i
                board.value.doMove(moves[i])
            }
        }

        if (moveIndex >= 0 && moveIndex < moves.size) {
            val move = moves[moveIndex]
            lastMove.value = Pair(move.from, move.to)
        }

        updateHighlights()
    }

    private fun updateHighlights() {
        val highlights = mutableSetOf<Square>()

        val currentMistake = getCurrentMistake()
        if (currentMistake != null) {
            lastMove.value?.second?.let { highlights.add(it) }
        }

        highlightedSquares.value = highlights
    }

    fun getCurrentEvaluation(): Int {
        val index = currentMoveIndex.intValue + 1
        return if (index in evaluations.indices) {
            evaluations[index]
        } else {
            0
        }
    }

    fun getCurrentMoveNotation(): String {
        return if (currentMoveIndex.intValue >= 0 && currentMoveIndex.intValue < moveNotations.size) {
            moveNotations[currentMoveIndex.intValue]
        } else {
            "Начальная позиция"
        }
    }

    fun getCurrentMistake(): Mistake? {
        if (currentMoveIndex.intValue >= 0) {
            val moveNumber = (currentMoveIndex.intValue / 2) + 1
            val isWhiteMove = currentMoveIndex.intValue % 2 == 0
            val currentColor = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK

            return mistakes.find {
                it.moveNumber == moveNumber && it.color == currentColor
            }
        }
        return null
    }

    /**
     * Определение ключевых моментов партии
     *
     * Логика:
     * - evaluations[0] = начальная позиция
     * - evaluations[i] = оценка ПОСЛЕ хода с индексом (i-1)
     * - Все оценки с точки зрения БЕЛЫХ (+ = хорошо белым)
     *
     * Для определения качества хода:
     * - Смотрим изменение оценки с точки зрения ИГРОКА (не стороны, делающей ход)
     * - Если игрок белый: улучшение = оценка выросла
     * - Если игрок чёрный: улучшение = оценка упала (стало лучше чёрным)
     */
    fun getKeyMoments(): List<KeyMoment> {
        val keyMoments = mutableListOf<KeyMoment>()

        if (evaluations.size < 2 || moves.isEmpty()) {
            println("getKeyMoments: Not enough data (evals=${evaluations.size}, moves=${moves.size})")
            return keyMoments
        }

        println("=== CALCULATING KEY MOMENTS ===")
        println("Player color: $playerColor")
        println("Total evaluations: ${evaluations.size}, Total moves: ${moves.size}")

        for (moveIndex in 0 until moves.size) {
            // Проверяем что есть оценки до и после хода
            if (moveIndex + 1 >= evaluations.size) break

            val evalBefore = evaluations[moveIndex]      // Оценка ДО хода
            val evalAfter = evaluations[moveIndex + 1]   // Оценка ПОСЛЕ хода

            val isWhiteMove = moveIndex % 2 == 0
            val movingSide = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK
            val isPlayerMove = (movingSide == playerColor)

            // Изменение оценки (с точки зрения белых)
            val rawChange = evalAfter - evalBefore

            // Изменение с точки зрения ИГРОКА
            // Если игрок белый: + = хорошо
            // Если игрок чёрный: - = хорошо (инвертируем)
            val changeForPlayer = if (playerColor == ChessColor.WHITE) rawChange else -rawChange

            // Для ходов игрока: положительное изменение = улучшение
            // Для ходов соперника: отрицательное изменение (для игрока) = соперник улучшил свою позицию
            val effectiveChange = if (isPlayerMove) changeForPlayer else -changeForPlayer

            // Позиция игрока ДО хода (для определения "сложной позиции")
            val playerPositionBefore = if (playerColor == ChessColor.WHITE) evalBefore else -evalBefore

            // Определяем качество хода
            val quality = classifyMoveQuality(
                effectiveChange = effectiveChange,
                isPlayerMove = isPlayerMove,
                playerPositionBefore = playerPositionBefore
            )

            // Логируем только значимые ходы для отладки
            if (quality != null || abs(rawChange) > 30) {
                val moveNum = (moveIndex / 2) + 1
                val side = if (isWhiteMove) "W" else "B"
                println("Move $moveNum$side (${moves[moveIndex].san}): " +
                        "eval $evalBefore → $evalAfter (raw: $rawChange, forPlayer: $changeForPlayer) " +
                        "isPlayer=$isPlayerMove quality=$quality")
            }

            if (quality != null) {
                keyMoments.add(
                    KeyMoment(
                        moveIndex = moveIndex,
                        san = moves[moveIndex].san,
                        quality = quality,
                        evaluationBefore = evalBefore,
                        evaluationAfter = evalAfter,
                        evaluationChange = rawChange,
                        isPlayerMove = isPlayerMove,
                        comment = generateMoveComment(quality, effectiveChange)
                    )
                )
            }
        }

        println("=== FOUND ${keyMoments.size} KEY MOMENTS ===")
        keyMoments.forEach { m ->
            println("  ${m.quality}: move ${m.moveIndex} (${m.san}), change=${m.evaluationChange}")
        }

        // Сортируем по значимости (абсолютное изменение), но ограничиваем количество
        return keyMoments
            .sortedByDescending { abs(it.evaluationChange) }
            .take(15) // Не больше 15 ключевых моментов
    }

    /**
     * Классификация качества хода
     *
     * @param effectiveChange - изменение оценки с точки зрения того, хорошо это или плохо
     *                         Положительное = хороший ход, отрицательное = плохой
     * @param isPlayerMove - это ход игрока или соперника
     * @param playerPositionBefore - позиция игрока до хода (для определения "блестящих" ходов)
     */
    private fun classifyMoveQuality(
        effectiveChange: Int,
        isPlayerMove: Boolean,
        playerPositionBefore: Int
    ): MoveQuality? {

        // Для ходов ИГРОКА
        if (isPlayerMove) {
            return when {
                // Блестящий ход: значительное улучшение в плохой/равной позиции
                effectiveChange >= BRILLIANT_THRESHOLD &&
                        playerPositionBefore < BAD_POSITION_THRESHOLD -> MoveQuality.BRILLIANT

                // Отличный ход: хорошее улучшение позиции
                effectiveChange >= GREAT_MOVE_THRESHOLD -> MoveQuality.GREAT_MOVE

                // Зевок: потеря 2+ пешек
                effectiveChange <= -BLUNDER_THRESHOLD -> MoveQuality.BLUNDER

                // Ошибка: потеря 1-2 пешек
                effectiveChange <= -MISTAKE_THRESHOLD -> MoveQuality.MISTAKE

                // Неточность: потеря 0.5-1 пешки
                effectiveChange <= -INACCURACY_THRESHOLD -> MoveQuality.INACCURACY

                // Остальные ходы не показываем как ключевые
                else -> null
            }
        }
        // Для ходов СОПЕРНИКА (показываем только очень значимые)
        else {
            return when {
                // Соперник сделал отличный ход (сильно ухудшил нашу позицию)
                effectiveChange >= BLUNDER_THRESHOLD -> MoveQuality.GREAT_MOVE

                // Соперник зевнул (мы можем воспользоваться)
                effectiveChange <= -BLUNDER_THRESHOLD -> MoveQuality.BLUNDER

                // Остальные ходы соперника не показываем
                else -> null
            }
        }
    }

    private fun generateMoveComment(quality: MoveQuality, change: Int): String {
        val pawns = abs(change) / 100.0
        return when (quality) {
            MoveQuality.BRILLIANT -> "Блестящий ход! Улучшение на %.1f пешки".format(pawns)
            MoveQuality.GREAT_MOVE -> "Отличный ход"
            MoveQuality.BEST_MOVE -> "Лучший ход в позиции"
            MoveQuality.EXCELLENT -> "Превосходно"
            MoveQuality.GOOD -> "Хороший ход"
            MoveQuality.BOOK -> "Теоретический ход"
            MoveQuality.INACCURACY -> "Неточность (−%.1f)".format(pawns)
            MoveQuality.MISTAKE -> "Ошибка (−%.1f)".format(pawns)
            MoveQuality.BLUNDER -> "Зевок! Потеря %.1f пешки".format(pawns)
            MoveQuality.MISSED_WIN -> "Упущена победа"
        }
    }
}