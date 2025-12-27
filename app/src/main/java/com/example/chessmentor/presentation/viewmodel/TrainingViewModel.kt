package com.example.chessmentor.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.usecase.GenerateExercisesFromMistakesUseCase
import com.example.chessmentor.domain.usecase.GetTrainingExerciseUseCase
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

class TrainingViewModel(
    private val container: AppContainer
) : ViewModel() {

    companion object {
        private const val TAG = "TEST_LOG" // Специальный тег для поиска
        private const val WIN_THRESHOLD_CP = 30
    }

    enum class Phase { READY, FEEDBACK }
    enum class MoveQuality { BEST, EXCELLENT, GOOD, INACCURACY, MISTAKE, BLUNDER }

    var currentUser = mutableStateOf<User?>(null)
    var currentExercise = mutableStateOf<Exercise?>(null)

    var boardState = mutableStateOf(Board())
    var message = mutableStateOf<String?>(null)
    var playerSide = mutableStateOf(Side.WHITE)

    var phase = mutableStateOf(Phase.READY)
    var isSolved = mutableStateOf(false)
    var isLoading = mutableStateOf(false)
    var showHint = mutableStateOf(false)

    // Флаг блокировки
    var isAnalyzingMove = mutableStateOf(false)

    private var targetSequence = mutableListOf<String>()
    private var currentMoveIndex = 0
    private var startTime: Long = 0
    private var currentJob: Job? = null

    var sessionStats = mutableStateOf(SessionStats())
    var attemptedWrongMove = mutableStateOf(false)

    data class SessionStats(
        val solved: Int = 0,
        val failed: Int = 0,
        val totalTime: Int = 0,
        val averageTime: Int = 0
    )

    fun init(user: User) {
        if (currentUser.value == null || currentUser.value?.id != user.id) {
            currentUser.value = user
            generateExercisesIfNeeded()
            loadNextExercise()
        }
    }

    private fun generateExercisesIfNeeded() {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Generating exercises...")
                container.generateExercisesFromMistakesUseCase.execute(
                    GenerateExercisesFromMistakesUseCase.Input(userId, maxExercises = 50)
                )
            } catch (_: Exception) {}
        }
    }

    // --- ЗАГРУЗКА ---
    fun loadNextExercise() {
        val userId = currentUser.value?.id ?: return
        val excludeId = currentExercise.value?.id ?: -1L

        Log.e(TAG, ">>> loadNextExercise STARTED. Exclude ID: $excludeId")

        currentJob?.cancel()

        // Сброс
        phase.value = Phase.READY
        isSolved.value = false
        isAnalyzingMove.value = false
        message.value = null
        showHint.value = false
        attemptedWrongMove.value = false
        targetSequence.clear()
        currentMoveIndex = 0
        startTime = System.currentTimeMillis()

        currentJob = viewModelScope.launch {
            isLoading.value = true
            try {
                // Диагностика БД: Сколько всего задач?
                val count = container.exerciseRepository.countExercises()
                Log.e(TAG, "Total exercises in DB: $count")

                // Ждем сохранения
                delay(200)

                var attempts = 0
                while (attempts < 3) {
                    Log.d(TAG, "Attempt #$attempts to find new exercise (exclude $excludeId)")
                    val input = GetTrainingExerciseUseCase.Input(userId, excludeExerciseId = excludeId)
                    val result = container.getTrainingExerciseUseCase.execute(input)

                    if (result is GetTrainingExerciseUseCase.Result.Success) {
                        val newId = result.exercise.id
                        Log.e(TAG, "DB returned ID: $newId (Excluded was: $excludeId)")

                        if (newId == excludeId) {
                            Log.e(TAG, "ALARM! Got duplicate! DB ignoring filter? Retry...")
                            delay(500)
                            attempts++
                            continue
                        }
                        setupExercise(result.exercise)
                        return@launch
                    } else {
                        Log.e(TAG, "GetUseCase failed or empty: $result")
                        message.value = "Нет новых задач."
                        return@launch
                    }
                }
                message.value = "Ошибка: база возвращает одно и то же."
            } catch (e: Exception) {
                Log.e(TAG, "CRASH in loadNextExercise", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun setupExercise(exercise: Exercise) {
        Log.d(TAG, "Setting up exercise ID=${exercise.id}. Moves: ${exercise.solutionMoves}")
        currentExercise.value = exercise

        val newBoard = Board()
        newBoard.loadFromFen(exercise.fenPosition)
        boardState.value = newBoard
        playerSide.value = newBoard.sideToMove

        if (exercise.solutionMoves.size < 3) {
            Log.d(TAG, "Exercise too short (${exercise.solutionMoves.size}), extending...")
            extendSolutionWithEngine(exercise)
        } else {
            targetSequence.addAll(exercise.solutionMoves.map { it.lowercase() })
            Log.d(TAG, "Sequence loaded: $targetSequence")
        }
    }

    // --- ХОД ---
    fun onMoveMade(move: Move) {
        Log.d(TAG, "Tap on board: $move")

        // ЛОГИРУЕМ ПРИЧИНУ БЛОКИРОВКИ
        if (isSolved.value) { Log.e(TAG, "Blocked: isSolved=true"); return }
        if (phase.value == Phase.FEEDBACK) { Log.e(TAG, "Blocked: phase=FEEDBACK"); return }
        if (isAnalyzingMove.value) { Log.e(TAG, "Blocked: isAnalyzingMove=true"); return }
        if (boardState.value.sideToMove != playerSide.value) { Log.e(TAG, "Blocked: Not player turn"); return }

        val currentFen = boardState.value.fen
        val exercise = currentExercise.value ?: return

        try {
            val legalMoves = boardState.value.legalMoves()
            val legalMove = legalMoves.find { it.from == move.from && it.to == move.to }

            if (legalMove == null) {
                Log.d(TAG, "Illegal move selected (normal behavior)")
                return
            }

            Log.d(TAG, "Processing move: $legalMove")

            // Визуал
            val visualBoard = Board()
            visualBoard.loadFromFen(currentFen)
            visualBoard.doMove(legalMove)
            boardState.value = visualBoard

            if (visualBoard.isMated) {
                phase.value = Phase.FEEDBACK
                handleSuccess(exercise, "🏆 Мат! Победа.")
                return
            }

            isAnalyzingMove.value = true
            currentJob?.cancel()

            currentJob = viewModelScope.launch {
                try {
                    processUserMove(currentFen, legalMove, exercise)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing move", e)
                    isAnalyzingMove.value = false
                    phase.value = Phase.READY
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun processUserMove(fenBefore: String, move: Move, exercise: Exercise) {
        val userUci = "${move.from.toString().lowercase()}${move.to.toString().lowercase()}"
        var isCorrect = false

        Log.d(TAG, "Checking move $userUci vs sequence $targetSequence (index $currentMoveIndex)")

        if (currentMoveIndex < targetSequence.size) {
            val expected = targetSequence[currentMoveIndex]
            if (userUci.startsWith(expected)) isCorrect = true
        }

        if (!isCorrect) {
            Log.d(TAG, "Move not in sequence. Checking engine...")
            val quality = checkMoveQuality(fenBefore, move)
            Log.d(TAG, "Engine quality: $quality")

            if (quality == MoveQuality.BEST || quality == MoveQuality.EXCELLENT) {
                isCorrect = true
                targetSequence.clear()
                phase.value = Phase.FEEDBACK
                handleSuccess(exercise, "✅ Отличный ход! (Альтернатива)")
                isAnalyzingMove.value = false
                return
            } else {
                isAnalyzingMove.value = false
                handleMistake(fenBefore, "❌ Ошибка", move)
                return
            }
        }

        // --- ПРАВИЛЬНО ---
        isAnalyzingMove.value = false
        currentMoveIndex++
        Log.d(TAG, "Move correct. New index: $currentMoveIndex")

        if (targetSequence.isNotEmpty() && currentMoveIndex < targetSequence.size) {
            val opponentMoveUci = targetSequence[currentMoveIndex]
            delay(400)

            val b = Board()
            b.loadFromFen(boardState.value.fen)
            applyUciMove(b, opponentMoveUci)
            boardState.value = b

            val oppMoveStr = opponentMoveUci.substring(0,2) + "-" + opponentMoveUci.substring(2,4)
            message.value = "Соперник: $oppMoveStr"

            currentMoveIndex++
            Log.d(TAG, "Opponent moved $opponentMoveUci. New index: $currentMoveIndex")

            if (currentMoveIndex >= targetSequence.size) {
                phase.value = Phase.FEEDBACK
                handleSuccess(exercise, "✅ Решено!")
            } else {
                delay(800)
                message.value = null
            }
        } else {
            phase.value = Phase.FEEDBACK
            handleSuccess(exercise, "✅ Задача решена!")
        }

        // Финальная разблокировка на всякий случай
        isAnalyzingMove.value = false
    }

    private suspend fun handleMistake(fenBefore: String, msg: String, userMove: Move) {
        Log.d(TAG, "handleMistake started: $msg")
        attemptedWrongMove.value = true
        message.value = msg
        phase.value = Phase.FEEDBACK

        if (!msg.contains("Неплохо")) {
            val s = sessionStats.value
            sessionStats.value = s.copy(failed = s.failed + 1)
        }

        try {
            delay(500)
            try {
                val engine = container.chessEngine
                if (engine.init()) {
                    val wrongBoard = Board()
                    wrongBoard.loadFromFen(fenBefore)
                    wrongBoard.doMove(userMove)
                    val punishment = engine.getBestMove(wrongBoard.fen, depthLimit = 8)
                    engine.destroy()
                    if (punishment != null) {
                        applyUciMove(wrongBoard, punishment)
                        boardState.value = wrongBoard
                        message.value = "⚠️ Опровержение..."
                        Log.d(TAG, "Refutation shown: $punishment")
                    }
                }
            } catch (_: Exception) {}
            delay(2000)
        } finally {
            Log.d(TAG, "Resetting board to original state")
            val originalBoard = Board()
            originalBoard.loadFromFen(fenBefore)
            boardState.value = originalBoard
            phase.value = Phase.READY
            delay(1500)
            if (message.value == msg || message.value?.contains("Опровержение") == true) {
                message.value = null
            }
        }
    }

    // --- UTILS ---
    private suspend fun checkMoveQuality(fen: String, userMove: Move): MoveQuality {
        return withContext(Dispatchers.IO) {
            try {
                val engine = container.chessEngine
                if (!engine.init()) return@withContext MoveQuality.MISTAKE
                val bestMoveUci = engine.getBestMove(fen, depthLimit = 10) ?: return@withContext MoveQuality.MISTAKE
                val userUci = "${userMove.from.toString().lowercase()}${userMove.to.toString().lowercase()}"
                if (bestMoveUci.lowercase().startsWith(userUci)) {
                    engine.destroy()
                    return@withContext MoveQuality.BEST
                }
                val b1 = Board(); b1.loadFromFen(fen); applyUciMove(b1, bestMoveUci)
                val s1 = engine.evaluate(b1.fen, depthLimit = 10)
                val b2 = Board(); b2.loadFromFen(fen); b2.doMove(userMove)
                val s2 = engine.evaluate(b2.fen, depthLimit = 10)
                engine.destroy()
                val diff = abs(s1 - s2)
                return@withContext when {
                    diff <= WIN_THRESHOLD_CP -> MoveQuality.EXCELLENT
                    diff <= 90 -> MoveQuality.GOOD
                    else -> MoveQuality.MISTAKE
                }
            } catch (e: Exception) { return@withContext MoveQuality.MISTAKE }
        }
    }

    private suspend fun extendSolutionWithEngine(exercise: Exercise) {
        withContext(Dispatchers.IO) {
            val firstMove = exercise.solutionMoves.firstOrNull() ?: return@withContext
            targetSequence.clear()
            targetSequence.add(firstMove.lowercase())
            try {
                val engine = container.chessEngine
                if (!engine.init()) return@withContext
                val sim = Board()
                sim.loadFromFen(exercise.fenPosition)
                applyUciMove(sim, firstMove)
                if (!sim.isMated && !sim.isDraw) {
                    val reply = engine.getBestMove(sim.fen, depthLimit = 10)
                    if (reply != null) {
                        targetSequence.add(reply.lowercase())
                        applyUciMove(sim, reply)
                        if (!sim.isMated && !sim.isDraw) {
                            val nextUser = engine.getBestMove(sim.fen, depthLimit = 10)
                            if (nextUser != null) targetSequence.add(nextUser.lowercase())
                        }
                    }
                }
                engine.destroy()
                if (targetSequence.size > exercise.solutionMoves.size) {
                    val updated = exercise.copy(solutionMoves = ArrayList(targetSequence))
                    container.exerciseRepository.saveExercise(updated)
                    withContext(Dispatchers.Main) { currentExercise.value = updated }
                    Log.d(TAG, "Extended exercise to ${targetSequence.size} moves")
                }
            } catch (e: Exception) { Log.e(TAG, "Extend failed", e) }
        }
    }

    private fun handleSuccess(exercise: Exercise, successMessage: String) {
        Log.d(TAG, "SUCCESS! Saving attempt...")
        isSolved.value = true
        val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val ratingChange = if (attemptedWrongMove.value) 5 else 15

        message.value = "$successMessage (+${ratingChange})"

        viewModelScope.launch {
            updateRating(ratingChange)
            saveAttempt(exercise, true, timeSpent)
            Log.d(TAG, "Attempt saved to DB.")
        }

        val s = sessionStats.value
        sessionStats.value = s.copy(solved = s.solved + 1, totalTime = s.totalTime + timeSpent)
    }

    private fun applyUciMove(board: Board, uci: String) {
        val from = Square.fromValue(uci.substring(0, 2).uppercase())
        val to = Square.fromValue(uci.substring(2, 4).uppercase())
        board.doMove(Move(from, to))
    }

    fun resetPosition() {
        val exercise = currentExercise.value ?: return
        val newBoard = Board()
        newBoard.loadFromFen(exercise.fenPosition)
        boardState.value = newBoard
        phase.value = Phase.READY
        isAnalyzingMove.value = false
        message.value = null
    }

    fun continueNext() { loadNextExercise() }

    fun toggleHint() {
        showHint.value = !showHint.value
        if (showHint.value) {
            val m = if (currentMoveIndex < targetSequence.size) targetSequence[currentMoveIndex] else null
            message.value = if (m != null) "💡 Ход: $m" else "Нет подсказки"
        } else {
            message.value = null
        }
    }

    private suspend fun updateRating(change: Int) {
        val user = currentUser.value ?: return
        val newRating = (user.rating + change).coerceIn(100, 3500)
        currentUser.value = user.withNewRating(newRating)
        container.userRepository.update(currentUser.value!!)
    }

    private suspend fun saveAttempt(exercise: Exercise, success: Boolean, timeSpent: Int) {
        val userId = currentUser.value?.id ?: return
        val attempt = ExerciseAttempt(
            userId = userId, exerciseId = exercise.id ?: 0,
            solved = success, timeSpentSeconds = timeSpent, movesMade = emptyList()
        )
        container.exerciseRepository.saveAttempt(attempt)
    }
}