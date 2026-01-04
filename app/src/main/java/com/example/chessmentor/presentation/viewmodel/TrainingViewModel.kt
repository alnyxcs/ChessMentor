// presentation/viewmodel/TrainingViewModel.kt
package com.example.chessmentor.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessmentor.di.AppContainer
// ✅ Используем ваш класс AnalysisLine из data/engine
import com.example.chessmentor.data.engine.AnalysisLine
import com.example.chessmentor.domain.analysis.MistakeAnalysis
import com.example.chessmentor.domain.analysis.SemanticMistakeAnalyzer
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.entity.TacticalPattern
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.usecase.GenerateExercisesFromMistakesUseCase
import com.example.chessmentor.domain.usecase.GetTrainingExerciseUseCase
import com.example.chessmentor.presentation.ui.components.BoardArrow
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Единое состояние экрана тренировки
 */
data class TrainingUiState(
    val board: Board = Board(),
    val arrows: List<BoardArrow> = emptyList(),
    val highlights: List<String> = emptyList(),
    val feedbackMessage: String? = null,
    val playerSide: Side = Side.WHITE,
    val isInputLocked: Boolean = false,
    val isSolved: Boolean = false,
    val isLoading: Boolean = false,
    val hintLevel: HintLevel = HintLevel.NONE,
    val sessionStats: SessionStats = SessionStats(),

    // Ошибки и анимации
    val currentMistakeAnalysis: MistakeAnalysis? = null,
    val showBetterMoveHint: Boolean = false,
    val betterMoveShown: Boolean = false,
    val punishmentAnimationStep: Int = 0,
    val isAnimatingPunishment: Boolean = false,
    val isErrorShake: Boolean = false, // Тряска экрана при ошибке

    // Кластеризация и очередь
    val currentPattern: String? = null,
    val similarExercisesQueued: Int = 0
)

data class SessionStats(
    val solved: Int = 0,
    val failed: Int = 0
)

enum class HintLevel { NONE, PIECE_HIGHLIGHT, FULL_SOLUTION }
enum class MoveQuality { BEST, EXCELLENT, MISTAKE }

class TrainingViewModel(
    private val container: AppContainer
) : ViewModel() {

    companion object {
        private const val TAG = "TrainingVM"
        private const val HUMAN_REACTION_DELAY_MS = 700L
        private const val ANIMATION_DELAY_MS = 600L
        private const val PUNISHMENT_DISPLAY_MS = 2500L
        private const val SIMILAR_EXERCISES_COUNT = 2
    }

    private val semanticAnalyzer = SemanticMistakeAnalyzer(container.chessEngine)

    private val _uiState = mutableStateOf(TrainingUiState())
    val uiState: State<TrainingUiState> = _uiState

    private var currentUser: User? = null
    private var currentExercise: Exercise? = null
    private var targetSequence = mutableListOf<String>()
    private var currentMoveIndex = 0
    private var startTime: Long = 0
    private var attemptFailed = false
    private var currentJob: Job? = null
    private var positionBeforePlayerMove: String = ""

    private val sessionQueue = mutableListOf<Exercise>()
    private var lastMistakePattern: String? = null

    // ================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ================================================================

    fun init(user: User) {
        if (currentUser?.id != user.id) {
            currentUser = user
            sessionQueue.clear()
            lastMistakePattern = null
            generateExercisesIfNeeded()
            loadNextExercise()
        }
    }

    private fun generateExercisesIfNeeded() {
        val userId = currentUser?.id ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                container.generateExercisesFromMistakesUseCase.execute(
                    GenerateExercisesFromMistakesUseCase.Input(userId, maxExercises = 50)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating exercises", e)
            }
        }
    }

    // ================================================================
    // ЗАГРУЗКА ЗАДАЧИ
    // ================================================================

    fun loadNextExercise() {
        val userId = currentUser?.id ?: 0L
        val excludeId = currentExercise?.id ?: -1L

        currentJob?.cancel()

        // 1. Проверяем очередь
        if (sessionQueue.isNotEmpty()) {
            val nextFromQueue = sessionQueue.removeFirst()
            viewModelScope.launch {
                resetStateForNewExercise()
                setupExercise(nextFromQueue)
                _uiState.value = _uiState.value.copy(
                    similarExercisesQueued = sessionQueue.size,
                    currentPattern = nextFromQueue.tacticalPattern.name
                )
            }
            return
        }

        // 2. Загружаем из БД
        _uiState.value = TrainingUiState(
            isLoading = true,
            sessionStats = _uiState.value.sessionStats
        )

        resetStateForNewExercise()

        currentJob = viewModelScope.launch {
            try {
                var result: GetTrainingExerciseUseCase.Result? = null
                // 3 попытки загрузки (чтобы не показать ту же задачу подряд)
                repeat(3) {
                    if (result !is GetTrainingExerciseUseCase.Result.Success) {
                        result = container.getTrainingExerciseUseCase.execute(
                            GetTrainingExerciseUseCase.Input(
                                userId = userId,
                                excludeExerciseId = excludeId,
                                preferredPattern = lastMistakePattern
                            )
                        )
                        if (result is GetTrainingExerciseUseCase.Result.Success &&
                            (result as GetTrainingExerciseUseCase.Result.Success).exercise.id == excludeId) {
                            delay(300)
                            result = null
                        }
                    }
                }

                when (val res = result) {
                    is GetTrainingExerciseUseCase.Result.Success -> {
                        setupExercise(res.exercise)
                        _uiState.value = _uiState.value.copy(
                            currentPattern = res.exercise.tacticalPattern.name,
                            similarExercisesQueued = 0
                        )
                    }
                    is GetTrainingExerciseUseCase.Result.NoSuitableExercises -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            feedbackMessage = "Нет новых задач. Загрузите партии!"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            feedbackMessage = "Ошибка загрузки задачи"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercise", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    feedbackMessage = "Сбой: ${e.message}"
                )
            }
        }
    }

    private fun resetStateForNewExercise() {
        targetSequence.clear()
        currentMoveIndex = 0
        attemptFailed = false
        startTime = System.currentTimeMillis()
        positionBeforePlayerMove = ""
    }

    private suspend fun setupExercise(exercise: Exercise) {
        currentExercise = exercise
        val newBoard = Board()
        newBoard.loadFromFen(exercise.fenPosition)
        positionBeforePlayerMove = exercise.fenPosition

        if (exercise.solutionMoves.size < 3) {
            extendSolutionWithEngine(exercise, newBoard)
        } else {
            targetSequence.addAll(exercise.solutionMoves.map { it.lowercase() })
        }

        _uiState.value = _uiState.value.copy(
            board = newBoard,
            playerSide = newBoard.sideToMove,
            isLoading = false,
            feedbackMessage = null
        )

        exercise.id?.let {
            container.exerciseRepository.incrementTimesShown(it)
        }
    }

    private suspend fun extendSolutionWithEngine(exercise: Exercise, board: Board) {
        withContext(Dispatchers.IO) {
            try {
                val engine = container.chessEngine
                if (engine.init()) {
                    // Используем getBestMoveWithLine для более точного PV
                    val line = engine.getBestMoveWithLine(board.fen, 12)
                    if (line != null && line.principalVariation.isNotEmpty()) {
                        targetSequence.addAll(line.principalVariation.map { it.lowercase() })
                    } else {
                        val bestMove = engine.getBestMove(board.fen, 12)
                        if (bestMove != null) {
                            targetSequence.add(bestMove.lowercase())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extending solution", e)
                targetSequence.addAll(exercise.solutionMoves.map { it.lowercase() })
            }
        }
    }

    // ================================================================
    // ОБРАБОТКА ХОДА ИГРОКА
    // ================================================================

    fun onMoveMade(move: Move) {
        val state = _uiState.value
        if (state.isInputLocked || state.isSolved || state.isAnimatingPunishment) return
        if (state.board.sideToMove != state.playerSide) return

        // 1. ВАЛИДАЦИЯ: Проверка правил шахмат перед ходом
        if (!state.board.isMoveLegal(move, true)) {
            triggerShakeEffect()
            _uiState.value = state.copy(feedbackMessage = "Невозможный ход")
            viewModelScope.launch {
                delay(1000)
                if (_uiState.value.feedbackMessage == "Невозможный ход") {
                    _uiState.value = _uiState.value.copy(feedbackMessage = null)
                }
            }
            return
        }

        positionBeforePlayerMove = state.board.fen

        _uiState.value = state.copy(
            arrows = emptyList(),
            highlights = emptyList(),
            hintLevel = HintLevel.NONE,
            feedbackMessage = null,
            currentMistakeAnalysis = null,
            showBetterMoveHint = false,
            betterMoveShown = false
        )

        val newBoard = Board()
        newBoard.loadFromFen(positionBeforePlayerMove)
        newBoard.doMove(move)

        _uiState.value = _uiState.value.copy(board = newBoard)

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            processUserMove(positionBeforePlayerMove, move)
        }
    }

    private fun triggerShakeEffect() {
        _uiState.value = _uiState.value.copy(isErrorShake = true)
        viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isErrorShake = false)
        }
    }

    private suspend fun processUserMove(fenBefore: String, move: Move) {
        val userUci = "${move.from}${move.to}".lowercase()
        var isCorrect = false

        if (currentMoveIndex < targetSequence.size) {
            val expected = targetSequence[currentMoveIndex]
            if (userUci.startsWith(expected.take(4))) {
                isCorrect = true
            }
        }

        if (!isCorrect) {
            _uiState.value = _uiState.value.copy(
                isInputLocked = true,
                feedbackMessage = "Анализирую..."
            )

            val quality = checkMoveQuality(fenBefore, move)

            if (quality == MoveQuality.BEST || quality == MoveQuality.EXCELLENT) {
                isCorrect = true
                targetSequence.clear()
                handleSuccess("✅ Отличный альтернативный ход!")
                return
            } else {
                handleMistakeComprehensive(fenBefore, move)
                return
            }
        }

        currentMoveIndex++

        if (targetSequence.isNotEmpty() && currentMoveIndex < targetSequence.size) {
            // 2. ЗАДЕРЖКА: Имитация размышления противника
            delay(HUMAN_REACTION_DELAY_MS)

            val opponentMoveUci = targetSequence[currentMoveIndex]
            makeOpponentMove(opponentMoveUci)
            currentMoveIndex++

            if (currentMoveIndex >= targetSequence.size) {
                handleSuccess("🏆 Отлично!")
            }
        } else {
            handleSuccess("✅ Задача решена!")
        }
    }

    private suspend fun makeOpponentMove(uci: String) {
        val board = Board()
        board.loadFromFen(_uiState.value.board.fen)
        applyUciMove(board, uci)

        _uiState.value = _uiState.value.copy(
            board = board,
            isInputLocked = false
        )
    }

    // ================================================================
    // АНАЛИЗ ОШИБКИ (С ИСПОЛЬЗОВАНИЕМ AnalysisLine)
    // ================================================================

    private suspend fun handleMistakeComprehensive(fenBefore: String, userMove: Move) {
        attemptFailed = true
        val stats = _uiState.value.sessionStats

        _uiState.value = _uiState.value.copy(
            isInputLocked = true,
            isAnimatingPunishment = true,
            sessionStats = stats.copy(failed = stats.failed + 1),
            feedbackMessage = "Анализ ошибки..."
        )

        try {
            val engine = container.chessEngine
            if (!engine.init()) {
                fallbackMistakeHandling(fenBefore, userMove)
                return
            }

            val boardAfterPlayer = Board()
            boardAfterPlayer.loadFromFen(fenBefore)
            if (!boardAfterPlayer.doMove(userMove)) {
                fallbackMistakeHandling(fenBefore, userMove)
                return
            }

            // Запрашиваем ПОЛНУЮ линию анализа для наказания
            val punishmentLineObj = withContext(Dispatchers.IO) {
                engine.getBestMoveWithLine(boardAfterPlayer.fen, 10)
            }

            // Запрашиваем ПОЛНУЮ линию для того хода, который нужно было сделать
            val bestMoveLineObj = withContext(Dispatchers.IO) {
                engine.getBestMoveWithLine(fenBefore, 10)
            }

            if (punishmentLineObj == null || bestMoveLineObj == null) {
                Log.w(TAG, "Engine returned null lines")
                fallbackMistakeHandling(fenBefore, userMove)
                return
            }

            val boardBefore = Board()
            boardBefore.loadFromFen(fenBefore)

            // Семантический анализ
            val analysis = try {
                semanticAnalyzer.analyzeComprehensive(
                    boardBefore = boardBefore,
                    playerMove = userMove,
                    bestMoveLine = bestMoveLineObj,
                    punishmentLine = punishmentLineObj
                )
            } catch (e: Exception) {
                Log.e(TAG, "Semantic analysis crash", e)
                fallbackMistakeHandling(fenBefore, userMove)
                return
            }

            // Кластеризация и очередь
            loadSimilarExercisesIntoQueue(analysis)

            // Показ результатов
            displayMistakeAnalysis(boardAfterPlayer, analysis, punishmentLineObj.principalVariation)

        } catch (e: Exception) {
            Log.e(TAG, "CRASH in handleMistakeComprehensive", e)
            fallbackMistakeHandling(fenBefore, userMove)
        }
    }

    private suspend fun displayMistakeAnalysis(
        boardAfterPlayer: Board,
        analysis: MistakeAnalysis,
        punishmentMoves: List<String>
    ) {
        val queueInfo = if (sessionQueue.isNotEmpty()) {
            "\n\n📚 +${sessionQueue.size} похожих задач добавлено"
        } else ""

        // Очищаем текст от мусорных цифр
        val cleanExplanation = cleanEvaluationText(analysis.explanation)

        _uiState.value = _uiState.value.copy(
            feedbackMessage = cleanExplanation + queueInfo,
            currentMistakeAnalysis = analysis,
            showBetterMoveHint = analysis.shouldShowBetterMove()
        )

        // Запускаем анимацию наказания
        if (punishmentMoves.isNotEmpty()) {
            animatePunishmentLine(boardAfterPlayer, punishmentMoves)
        }

        delay(PUNISHMENT_DISPLAY_MS)
        resetToPosition(positionBeforePlayerMove)
    }

    /**
     * Анимация всей цепочки наказания (PV)
     */
    private suspend fun animatePunishmentLine(startBoard: Board, moves: List<String>) {
        var currentBoard = startBoard.clone()

        for ((index, moveUci) in moves.withIndex()) {
            if (index >= 4) break // Не показываем слишком длинные варианты

            _uiState.value = _uiState.value.copy(
                punishmentAnimationStep = index + 1
            )

            val arrow = createArrowFromUci(moveUci, Color(0xAAF44336))

            _uiState.value = _uiState.value.copy(
                arrows = listOf(arrow)
            )

            delay(ANIMATION_DELAY_MS)

            applyUciMove(currentBoard, moveUci)

            _uiState.value = _uiState.value.copy(
                board = currentBoard.clone(),
                arrows = emptyList()
            )

            delay(ANIMATION_DELAY_MS / 2)
        }
    }

    /**
     * Robust Fallback: Если движок не выдал полную линию, пробуем простой метод
     */
    private suspend fun fallbackMistakeHandling(fenBefore: String, userMove: Move) {
        Log.w(TAG, "Using fallback mistake handling")

        try {
            val engine = container.chessEngine
            if (engine.init()) {
                val boardAfterPlayer = Board()
                boardAfterPlayer.loadFromFen(fenBefore)
                boardAfterPlayer.doMove(userMove)

                // Просим хотя бы один лучший ход (наказание)
                val punishmentUci = engine.getBestMove(boardAfterPlayer.fen, 10)

                if (punishmentUci != null) {
                    // Рисуем хотя бы одну стрелку
                    val arrow = createArrowFromUci(punishmentUci, Color(0xAAF44336))

                    _uiState.value = _uiState.value.copy(
                        arrows = listOf(arrow),
                        feedbackMessage = "Неточный ход. Посмотрите ответ соперника.",
                        currentMistakeAnalysis = null // Нет детального анализа
                    )

                    delay(ANIMATION_DELAY_MS)
                    applyUciMove(boardAfterPlayer, punishmentUci)
                    _uiState.value = _uiState.value.copy(board = boardAfterPlayer)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback also failed", e)
            _uiState.value = _uiState.value.copy(feedbackMessage = "❌ Ошибка")
        }

        delay(PUNISHMENT_DISPLAY_MS)
        resetToPosition(fenBefore)
    }

    private fun resetToPosition(fen: String) {
        val board = Board()
        board.loadFromFen(fen)

        _uiState.value = _uiState.value.copy(
            board = board,
            isInputLocked = false,
            isAnimatingPunishment = false,
            arrows = emptyList(),
            punishmentAnimationStep = 0,
            feedbackMessage = "Попробуйте ещё раз!"
        )
    }

    // ================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ================================================================

    private suspend fun loadSimilarExercisesIntoQueue(analysis: MistakeAnalysis) {
        val motif = analysis.tacticalMotif ?: return
        val pattern = TacticalPattern.fromMotif(motif)
        val patternName = pattern.name

        lastMistakePattern = patternName

        try {
            val currentId = currentExercise?.id ?: -1L
            val currentDifficulty = currentExercise?.difficultyRating ?: 50

            val similarExercises = container.exerciseRepository.findSimilarByPattern(
                pattern = patternName,
                excludeId = currentId,
                minDifficulty = (currentDifficulty - 20).coerceAtLeast(0),
                maxDifficulty = (currentDifficulty + 20).coerceAtMost(100),
                limit = SIMILAR_EXERCISES_COUNT
            )

            if (similarExercises.isNotEmpty()) {
                sessionQueue.addAll(similarExercises)
                _uiState.value = _uiState.value.copy(
                    similarExercisesQueued = sessionQueue.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading similar exercises", e)
        }
    }

    private suspend fun checkMoveQuality(fen: String, userMove: Move): MoveQuality {
        return withContext(Dispatchers.IO) {
            try {
                val engine = container.chessEngine
                if (!engine.init()) return@withContext MoveQuality.MISTAKE

                val bestMoveStr = engine.getBestMove(fen, 10) ?: return@withContext MoveQuality.MISTAKE

                // Для упрощения, если движок вернул простой ход, сравниваем строки
                val userUci = "${userMove.from}${userMove.to}".lowercase()
                if (bestMoveStr.startsWith(userUci)) {
                    return@withContext MoveQuality.BEST
                }

                // Более сложная проверка по оценке (если есть время)
                val bestBoard = Board()
                bestBoard.loadFromFen(fen)
                applyUciMove(bestBoard, bestMoveStr)
                val bestScore = engine.evaluate(bestBoard.fen, 10)

                val userBoard = Board()
                userBoard.loadFromFen(fen)
                userBoard.doMove(userMove)
                val userScore = engine.evaluate(userBoard.fen, 10)

                val diff = abs(bestScore - userScore)

                return@withContext when {
                    diff <= 15 -> MoveQuality.BEST
                    diff <= 40 -> MoveQuality.EXCELLENT
                    else -> MoveQuality.MISTAKE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking move quality", e)
                MoveQuality.MISTAKE
            }
        }
    }

    private fun cleanEvaluationText(text: String): String {
        val textWithoutBrackets = text.replace(Regex("\\s*\\([+-]?\\d+\\.?\\d*\\)"), "")
        val cleanText = textWithoutBrackets.replace(Regex("[+-]?\\d+\\.\\d+"), "")
        return cleanText.trim()
    }

    private fun applyUciMove(board: Board, uci: String) {
        try {
            val from = Square.fromValue(uci.substring(0, 2).uppercase())
            val to = Square.fromValue(uci.substring(2, 4).uppercase())
            board.doMove(Move(from, to))
        } catch (e: Exception) {
            Log.e(TAG, "Error applying move: $uci", e)
        }
    }

    private fun createArrowFromUci(uci: String, color: Color): BoardArrow {
        val from = Square.fromValue(uci.substring(0, 2).uppercase())
        val to = Square.fromValue(uci.substring(2, 4).uppercase())
        return BoardArrow(from, to, color)
    }

    private suspend fun updateRating(change: Int) {
        val user = currentUser ?: return
        val newRating = (user.rating + change).coerceIn(100, 3500)
        currentUser = user.copy(rating = newRating)
        container.userRepository.update(currentUser!!)
    }

    private suspend fun saveAttempt(solved: Boolean) {
        currentExercise?.let { ex ->
            val attempt = ExerciseAttempt(
                id = null,
                userId = currentUser?.id ?: 0L,
                exerciseId = ex.id ?: 0L,
                solved = solved,
                timeSpentSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt(),
                movesMade = emptyList(),
                attemptedAt = System.currentTimeMillis()
            )
            container.exerciseRepository.saveAttempt(attempt)
        }
    }

    // ================================================================
    // UI HANDLERS
    // ================================================================

    fun onShowBetterMoveClicked() {
        val state = _uiState.value
        val analysis = state.currentMistakeAnalysis ?: return
        val betterMove = analysis.betterMoveInfo ?: return

        if (state.betterMoveShown) return

        val arrow = createArrowFromUci(betterMove.moveUci, Color(0xAA4CAF50))

        _uiState.value = state.copy(
            arrows = listOf(arrow),
            betterMoveShown = true,
            feedbackMessage = "Лучше было: ${betterMove.explanation}"
        )
    }

    fun onHintClicked() {
        val state = _uiState.value
        if (state.isSolved || state.isInputLocked || state.isAnimatingPunishment) return

        val correctMoveUci = if (currentMoveIndex < targetSequence.size) {
            targetSequence[currentMoveIndex]
        } else null

        if (correctMoveUci == null) {
            _uiState.value = state.copy(feedbackMessage = "Решение не найдено")
            return
        }

        val fromSq = correctMoveUci.substring(0, 2)
        val toSq = correctMoveUci.substring(2, 4)

        val nextLevel = when (state.hintLevel) {
            HintLevel.NONE -> HintLevel.PIECE_HIGHLIGHT
            HintLevel.PIECE_HIGHLIGHT -> HintLevel.FULL_SOLUTION
            HintLevel.FULL_SOLUTION -> HintLevel.FULL_SOLUTION
        }

        when (nextLevel) {
            HintLevel.PIECE_HIGHLIGHT -> {
                _uiState.value = state.copy(
                    hintLevel = nextLevel,
                    highlights = listOf(fromSq),
                    arrows = emptyList(),
                    feedbackMessage = "Посмотрите на эту фигуру"
                )
            }
            HintLevel.FULL_SOLUTION -> {
                val arrow = BoardArrow(
                    Square.fromValue(fromSq.uppercase()),
                    Square.fromValue(toSq.uppercase()),
                    Color(0xAA4CAF50)
                )
                _uiState.value = state.copy(
                    hintLevel = nextLevel,
                    highlights = emptyList(),
                    arrows = listOf(arrow),
                    feedbackMessage = "Лучший ход"
                )
            }
            else -> {}
        }
    }

    private fun handleSuccess(msg: String) {
        val stats = _uiState.value.sessionStats
        val queueInfo = if (sessionQueue.isNotEmpty()) {
            "\n📚 Ещё ${sessionQueue.size} похожих задач"
        } else ""

        _uiState.value = _uiState.value.copy(
            isSolved = true,
            isInputLocked = true,
            feedbackMessage = msg + queueInfo,
            sessionStats = stats.copy(solved = stats.solved + 1),
            currentMistakeAnalysis = null,
            showBetterMoveHint = false,
            similarExercisesQueued = sessionQueue.size
        )

        val ratingChange = if (attemptFailed) 5 else 15

        viewModelScope.launch {
            updateRating(ratingChange)
            saveAttempt(solved = true)
            currentExercise?.id?.let {
                container.exerciseRepository.incrementTimesSolved(it)
            }
        }
    }

    fun clearSessionQueue() {
        sessionQueue.clear()
        lastMistakePattern = null
        _uiState.value = _uiState.value.copy(
            similarExercisesQueued = 0,
            currentPattern = null
        )
    }
}