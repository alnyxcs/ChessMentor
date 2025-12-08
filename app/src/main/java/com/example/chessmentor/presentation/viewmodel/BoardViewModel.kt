// presentation/viewmodel/BoardViewModel.kt
package com.example.chessmentor.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chessmentor.domain.entity.*
import com.example.chessmentor.presentation.ui.components.SoundManager
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import java.io.File
import kotlin.math.abs

class BoardViewModel : ViewModel() {

    companion object {
        private const val TAG = "BoardViewModel"
    }

    // ==================== СОСТОЯНИЯ ДОСКИ ====================

    val board = mutableStateOf(Board())
    val moves = mutableStateListOf<Move>()
    val moveNotations = mutableStateListOf<String>()
    val currentMoveIndex = mutableIntStateOf(-1)
    val highlightedSquares = mutableStateOf<Set<Square>>(emptySet())
    val lastMove = mutableStateOf<Pair<Square, Square>?>(null)

    // ==================== ДАННЫЕ АНАЛИЗА ====================

    val evaluations = mutableStateListOf<Int>()
    val analyzedMoves = mutableStateListOf<AnalyzedMove>()
    val mistakes = mutableStateListOf<Mistake>()

    // ==================== НАСТРОЙКИ ====================

    private var playerColor: ChessColor = ChessColor.WHITE
    private var currentGame: Game? = null

    var soundManager: SoundManager? = null

    // ==================== ЗАГРУЗКА ПАРТИИ ====================

    /**
     * Загрузить партию для просмотра
     * ✅ ОБНОВЛЕНО: Принимает evaluations из Game.evaluationsJson
     */
    fun loadGame(
        game: Game,
        gameMistakes: List<Mistake>,
        gameAnalyzedMoves: List<AnalyzedMove> = emptyList(),
        gameEvaluations: List<Int> = emptyList()  // ✅ Теперь загружается из БД
    ) {
        try {
            Log.d(TAG, "=== LOADING GAME ${game.id} ===")
            Log.d(TAG, "PGN length: ${game.pgnData.length}")
            Log.d(TAG, "Player color: ${game.playerColor}")

            // Сохраняем текущую игру
            currentGame = game
            playerColor = game.playerColor

            // Очищаем все состояния
            clearState()

            // Загружаем данные анализа
            mistakes.addAll(gameMistakes)
            analyzedMoves.addAll(gameAnalyzedMoves)

            Log.d(TAG, "Loaded analysis data:")
            Log.d(TAG, "  - mistakes: ${gameMistakes.size}")
            Log.d(TAG, "  - analyzedMoves: ${gameAnalyzedMoves.size}")
            Log.d(TAG, "  - evaluations from param: ${gameEvaluations.size}")
            Log.d(TAG, "  - game.hasEvaluations(): ${game.hasEvaluations()}")

            // Парсим PGN
            val normalizedPgn = normalizePgn(game.pgnData)
            val halfMoves = parsePgn(normalizedPgn)

            if (halfMoves.isEmpty()) {
                Log.e(TAG, "ERROR: No moves parsed from PGN")
                return
            }

            Log.d(TAG, "Parsed ${halfMoves.size} half-moves from PGN")

            // Добавляем ходы и создаём нотации
            loadMoves(halfMoves)

            // ✅ ОБНОВЛЕНО: Загружаем evaluations с приоритетом
            loadEvaluations(gameEvaluations, game)

            // Переходим в начальную позицию
            goToStart()

            Log.d(TAG, "=== GAME LOADED SUCCESSFULLY ===")
            Log.d(TAG, "Final state:")
            Log.d(TAG, "  - moves.size: ${moves.size}")
            Log.d(TAG, "  - moveNotations.size: ${moveNotations.size}")
            Log.d(TAG, "  - evaluations.size: ${evaluations.size}")
            Log.d(TAG, "  - analyzedMoves.size: ${analyzedMoves.size}")

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR loading game", e)
            e.printStackTrace()
        }
    }

    /**
     * Очистить все состояния
     */
    private fun clearState() {
        board.value = Board()
        moves.clear()
        moveNotations.clear()
        evaluations.clear()
        mistakes.clear()
        analyzedMoves.clear()
        currentMoveIndex.intValue = -1
        lastMove.value = null
        highlightedSquares.value = emptySet()
    }

    /**
     * Нормализация PGN для парсинга
     */
    private fun normalizePgn(pgn: String): String {
        val trimmed = pgn.trim()

        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("PGN пустой")
        }

        // Извлекаем только ходы, удаляя все теги, комментарии и NAG
        val movesOnly = trimmed
            .replace(Regex("""\[[^\]]*\]"""), " ")       // Теги [...]
            .replace(Regex("""\{[^}]*\}"""), " ")        // Комментарии {...}
            .replace(Regex("""\([^)]*\)"""), " ")        // Вариации (...)
            .replace(Regex("""\$\d+"""), " ")            // NAG ($1, $2, ...)
            .replace(Regex("""(1-0|0-1|1/2-1/2|\*)\s*$"""), "") // Результат
            .replace(Regex("""\s+"""), " ")              // Множественные пробелы
            .trim()

        if (movesOnly.isEmpty()) {
            throw IllegalArgumentException("В PGN нет ходов")
        }

        // Собираем корректный PGN с обязательными тегами
        return buildString {
            appendLine("""[Event "Imported Game"]""")
            appendLine("""[Site "ChessMentor"]""")
            appendLine("""[Date "????.??.??"]""")
            appendLine("""[Round "1"]""")
            appendLine("""[White "Player"]""")
            appendLine("""[Black "Opponent"]""")
            appendLine("""[Result "*"]""")
            appendLine()
            append(movesOnly)
            append(" *")  // Обязательный результат в конце
            appendLine()
        }
    }

    /**
     * Парсинг PGN через временный файл
     */
    private fun parsePgn(normalizedPgn: String): List<Move> {
        val tempFile = File.createTempFile("chess_game", ".pgn")
        return try {
            tempFile.writeText(normalizedPgn)

            val pgnHolder = PgnHolder(tempFile.absolutePath)
            pgnHolder.loadPgn()

            if (pgnHolder.games.isEmpty()) {
                Log.e(TAG, "PgnHolder returned empty games list")
                emptyList()
            } else {
                pgnHolder.games[0].halfMoves.toList()
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Загрузить ходы и создать нотации
     */
    private fun loadMoves(halfMoves: List<Move>) {
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

        Log.d(TAG, "Loaded ${moves.size} moves, ${moveNotations.size} notations")
    }

    /**
     * ✅ ОБНОВЛЕНО: Загрузить evaluations с приоритетом источников
     *
     * Приоритет:
     * 1. gameEvaluations (переданные параметром — свежие данные после анализа)
     * 2. game.getEvaluations() (из БД — для ранее проанализированных партий)
     * 3. Генерация mock-данных (fallback)
     */
    private fun loadEvaluations(gameEvaluations: List<Int>, game: Game) {
        evaluations.clear()

        when {
            // Приоритет 1: Переданные evaluations (после свежего анализа)
            gameEvaluations.isNotEmpty() -> {
                evaluations.addAll(gameEvaluations)
                Log.d(TAG, "Loaded ${gameEvaluations.size} evaluations from parameter")
            }

            // Приоритет 2: Evaluations из БД (Game.evaluationsJson)
            game.hasEvaluations() -> {
                val savedEvaluations = game.getEvaluations()
                evaluations.addAll(savedEvaluations)
                Log.d(TAG, "Loaded ${savedEvaluations.size} evaluations from Game.evaluationsJson")
            }

            // Приоритет 3: Генерация на основе AnalyzedMoves
            analyzedMoves.isNotEmpty() -> {
                generateEvaluationsFromAnalyzedMoves()
                Log.d(TAG, "Generated ${evaluations.size} evaluations from AnalyzedMoves")
            }

            // Fallback: Mock-данные
            else -> {
                generateMockEvaluations()
                Log.w(TAG, "Generated ${evaluations.size} mock evaluations (no real data)")
            }
        }
    }

    /**
     * Генерация evaluations на основе AnalyzedMoves
     * Используется когда есть analyzedMoves, но нет сохранённых evaluations
     */
    private fun generateEvaluationsFromAnalyzedMoves() {
        // Начальная оценка
        evaluations.add(0)

        var lastKnownEval = 0

        for (index in moves.indices) {
            val analyzed = analyzedMoves.find { it.moveIndex == index }

            if (analyzed != null) {
                // Используем реальные данные из AnalyzedMove
                lastKnownEval = analyzed.evalAfter
            }
            // Если нет данных для этого хода, используем последнюю известную оценку

            evaluations.add(lastKnownEval)
        }
    }

    /**
     * Генерация mock-данных (только для fallback)
     */
    private fun generateMockEvaluations() {
        evaluations.add(0)

        var currentEval = 0
        for (index in moves.indices) {
            // Случайное изменение для визуализации
            currentEval += (-15..15).random()
            currentEval = currentEval.coerceIn(-500, 500)
            evaluations.add(currentEval)
        }
    }

    // ==================== НАВИГАЦИЯ ====================

    fun goToStart() {
        Log.d(TAG, "goToStart()")
        board.value = Board()
        currentMoveIndex.intValue = -1
        lastMove.value = null
        highlightedSquares.value = emptySet()

        // Воспроизводим звук (опционально)
        // soundManager?.playMoveSound()
    }

    fun goToEnd() {
        Log.d(TAG, "goToEnd() - total moves: ${moves.size}")

        if (moves.isEmpty()) return

        goToStart()

        try {
            for (move in moves) {
                board.value.doMove(move)
            }
            currentMoveIndex.intValue = moves.size - 1

            val lastMoveObj = moves.last()
            lastMove.value = Pair(lastMoveObj.from, lastMoveObj.to)

            updateHighlights()
            //playMoveSound(moves.last())

            Log.d(TAG, "goToEnd() - moved to index ${currentMoveIndex.intValue}")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR in goToEnd()", e)
        }
    }

    fun goToNextMove() {
        if (currentMoveIndex.intValue >= moves.size - 1) {
            Log.d(TAG, "goToNextMove() - already at end")
            return
        }

        currentMoveIndex.intValue++
        val move = moves[currentMoveIndex.intValue]

        Log.d(TAG, "goToNextMove() - index: ${currentMoveIndex.intValue}, move: ${move.san}")

        try {
            board.value.doMove(move)
            lastMove.value = Pair(move.from, move.to)
            updateHighlights()
            //playMoveSound(move)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR in goToNextMove()", e)
        }
    }

    fun goToPreviousMove() {
        if (currentMoveIndex.intValue < 0) {
            Log.d(TAG, "goToPreviousMove() - already at start")
            return
        }

        Log.d(TAG, "goToPreviousMove() - index: ${currentMoveIndex.intValue}")

        try {
            board.value.undoMove()
            currentMoveIndex.intValue--

            lastMove.value = if (currentMoveIndex.intValue >= 0) {
                val move = moves[currentMoveIndex.intValue]
                Pair(move.from, move.to)
            } else {
                null
            }

            updateHighlights()
        } catch (e: Exception) {
            Log.e(TAG, "ERROR in goToPreviousMove()", e)
        }
    }

    fun goToMove(moveIndex: Int) {
        Log.d(TAG, "goToMove($moveIndex) - total moves: ${moves.size}")

        if (moveIndex < -1 || moveIndex >= moves.size) {
            Log.w(TAG, "goToMove() - invalid index: $moveIndex")
            return
        }

        // Оптимизация: если идём вперёд на 1 ход
        if (moveIndex == currentMoveIndex.intValue + 1) {
            goToNextMove()
            return
        }

        // Оптимизация: если идём назад на 1 ход
        if (moveIndex == currentMoveIndex.intValue - 1) {
            goToPreviousMove()
            return
        }

        // Полная перезагрузка позиции
        goToStart()

        try {
            for (i in 0..moveIndex) {
                if (i < moves.size) {
                    board.value.doMove(moves[i])
                    currentMoveIndex.intValue = i
                }
            }

            if (moveIndex >= 0 && moveIndex < moves.size) {
                val move = moves[moveIndex]
                lastMove.value = Pair(move.from, move.to)
               // playMoveSound(move)
            }

            updateHighlights()

            Log.d(TAG, "goToMove() - moved to index ${currentMoveIndex.intValue}")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR in goToMove()", e)
        }
    }

    /**
     * Обновить подсветку полей
     */
    private fun updateHighlights() {
        val highlights = mutableSetOf<Square>()

        // Подсвечиваем поле назначения если это ошибка
        val currentAnalyzed = getCurrentAnalyzedMove()
        if (currentAnalyzed?.isMistake() == true) {
            lastMove.value?.second?.let { highlights.add(it) }
        }

        highlightedSquares.value = highlights
    }

    /**
     * Воспроизвести звук хода
     */
    /*private fun playMoveSound(move: Move) {
        soundManager?.let { sm ->
            val isCapture = move.san.contains("x")
            val isCheck = move.san.contains("+") || move.san.contains("#")

            when {
                isCheck -> sm.playCheckSound()
                isCapture -> sm.playCaptureSound()
                else -> sm.playMoveSound()
            }
        }
    }
*/
    // ==================== ПОЛУЧЕНИЕ ДАННЫХ ====================

    /**
     * Текущая оценка позиции
     */
    fun getCurrentEvaluation(): Int {
        val index = currentMoveIndex.intValue + 1
        return evaluations.getOrElse(index) { 0 }
    }

    /**
     * Текущая нотация хода
     */
    fun getCurrentMoveNotation(): String {
        return if (currentMoveIndex.intValue in moveNotations.indices) {
            moveNotations[currentMoveIndex.intValue]
        } else {
            "Начальная позиция"
        }
    }

    /**
     * Текущий проанализированный ход
     */
    fun getCurrentAnalyzedMove(): AnalyzedMove? {
        if (currentMoveIndex.intValue < 0) return null
        return analyzedMoves.find { it.moveIndex == currentMoveIndex.intValue }
    }

    /**
     * Качество текущего хода
     */
    fun getCurrentMoveQuality(): MoveQuality? {
        return getCurrentAnalyzedMove()?.quality
    }

    /**
     * Текущая ошибка (для совместимости)
     */
    fun getCurrentMistake(): Mistake? {
        val analyzed = getCurrentAnalyzedMove() ?: return null
        if (!analyzed.isMistake()) return null

        return mistakes.find {
            it.moveNumber == analyzed.moveNumber && it.color == analyzed.color
        }
    }

    /**
     * ✅ НОВОЕ: Проверить, есть ли реальные evaluations
     */
    fun hasRealEvaluations(): Boolean {
        // Проверяем, есть ли данные и не являются ли они mock
        return evaluations.size > 1 && currentGame?.hasEvaluations() == true
    }

    /**
     * Получить ключевые моменты партии
     * Сортировка: сначала ошибки (по серьёзности), затем хорошие ходы
     */
    fun getKeyMoments(): List<KeyMoment> {
        return analyzedMoves
            .map { it.toKeyMoment() }
            .sortedWith(
                compareBy(
                    { moment ->
                        when (moment.quality) {
                            MoveQuality.BLUNDER -> 0
                            MoveQuality.MISTAKE -> 1
                            MoveQuality.INACCURACY -> 2
                            MoveQuality.MISSED_WIN -> 3
                            MoveQuality.BRILLIANT -> 4
                            MoveQuality.GREAT_MOVE -> 5
                            MoveQuality.BEST_MOVE -> 6
                            MoveQuality.EXCELLENT -> 7
                            MoveQuality.GOOD -> 8
                            MoveQuality.BOOK -> 9
                        }
                    },
                    { -abs(it.evaluationChange) }
                )
            )
    }

    /**
     * Статистика по качеству ходов
     */
    fun getMoveQualityStats(): Map<MoveQuality, Int> {
        return analyzedMoves
            .groupBy { it.quality }
            .mapValues { it.value.size }
    }

    /**
     * ✅ НОВОЕ: Получить цвет игрока
     */
    fun getPlayerColor(): ChessColor {
        return playerColor
    }

    /**
     * ✅ НОВОЕ: Общее количество ходов
     */
    fun getTotalMoves(): Int {
        return moves.size
    }

    /**
     * ✅ НОВОЕ: Проверить, является ли текущий ход ходом игрока
     */
    fun isCurrentMovePlayerMove(): Boolean {
        if (currentMoveIndex.intValue < 0) return false

        val isWhiteMove = currentMoveIndex.intValue % 2 == 0
        return (playerColor == ChessColor.WHITE && isWhiteMove) ||
                (playerColor == ChessColor.BLACK && !isWhiteMove)
    }
}