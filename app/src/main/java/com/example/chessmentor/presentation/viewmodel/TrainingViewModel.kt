package com.example.chessmentor.presentation.viewmodel

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
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TrainingViewModel(
    private val container: AppContainer
) : ViewModel() {

    var currentUser = mutableStateOf<User?>(null)
    var currentExercise = mutableStateOf<Exercise?>(null)

    // Состояние доски для UI
    var boardState = mutableStateOf(Board())

    // Сообщения для пользователя
    var message = mutableStateOf<String?>(null)
    var isSolved = mutableStateOf(false)
    var isLoading = mutableStateOf(false)
    
    // ✅ НОВОЕ: Показывать подсказку
    var showHint = mutableStateOf(false)
    
    // ✅ НОВОЕ: Таймер
    private var startTime: Long = 0
    
    // ✅ НОВОЕ: Статистика сессии
    var sessionStats = mutableStateOf(SessionStats())
    
    // ✅ НОВОЕ: Попытка повторного хода после ошибки
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
    
    // ✅ НОВОЕ: Генерация упражнений из ошибок
    private fun generateExercisesIfNeeded() {
        val userId = currentUser.value?.id ?: return
        
        viewModelScope.launch {
            isLoading.value = true
            val input = GenerateExercisesFromMistakesUseCase.Input(userId, maxExercises = 50)
            
            when (val result = container.generateExercisesFromMistakesUseCase.execute(input)) {
                is GenerateExercisesFromMistakesUseCase.Result.Success -> {
                    if (result.generatedCount > 0) {
                        message.value = "Создано ${result.generatedCount} новых задач из ваших ошибок!"
                    }
                }
                is GenerateExercisesFromMistakesUseCase.Result.Error -> {
                    // Не показываем ошибку генерации, просто используем существующие задачи
                }
            }
            isLoading.value = false
        }
    }

    fun loadNextExercise() {
        val userId = currentUser.value?.id ?: return
        isSolved.value = false
        message.value = null
        showHint.value = false
        attemptedWrongMove.value = false
        startTime = System.currentTimeMillis()

        viewModelScope.launch {
            isLoading.value = true
            val input = GetTrainingExerciseUseCase.Input(userId)
            
            when (val result = container.getTrainingExerciseUseCase.execute(input)) {
                is GetTrainingExerciseUseCase.Result.Success -> {
                    currentExercise.value = result.exercise
                    val newBoard = Board()
                    newBoard.loadFromFen(result.exercise.fenPosition)
                    boardState.value = newBoard
                }
                is GetTrainingExerciseUseCase.Result.Error -> {
                    message.value = "Ошибка: ${result.message}"
                }
                is GetTrainingExerciseUseCase.Result.NoSuitableExercises -> {
                    message.value = "Нет новых задач. Проанализируйте больше партий!"
                }
            }
            isLoading.value = false
        }
    }

    fun onMoveMade(move: Move) {
        val exercise = currentExercise.value ?: return
        if (isSolved.value) return

        try {
            val legalMoves = boardState.value.legalMoves()
            val legalMove = legalMoves.find { it.from == move.from && it.to == move.to }

            if (legalMove != null) {
                val newBoard = Board()
                newBoard.loadFromFen(boardState.value.fen)
                newBoard.doMove(legalMove)
                boardState.value = newBoard

                val moveSan = try {
                    legalMove.san ?: "${move.from}-${move.to}"
                } catch (e: Exception) {
                    "${move.from}-${move.to}"
                }

                val bestMoveSan = exercise.solutionMoves.firstOrNull() ?: ""

                if (moveSan.equals(bestMoveSan, ignoreCase = true)) {
                    handleSuccess(exercise)
                } else {
                    handleFailure(exercise, moveSan)
                }
            } else {
                message.value = "Нелегальный ход"
            }
        } catch (e: Exception) {
            message.value = "Ошибка: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun handleSuccess(exercise: Exercise) {
        isSolved.value = true
        
        val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        
        // Рейтинг в зависимости от попыток
        val ratingChange = if (!attemptedWrongMove.value && !showHint.value) {
            15  // Правильно с первой попытки без подсказок
        } else if (!showHint.value) {
            10  // Со второй попытки
        } else {
            5   // С подсказкой
        }
        
        message.value = "✅ Правильно! ${if (timeSpent < 10) "Быстро!" else ""} Рейтинг +$ratingChange"

        updateRating(ratingChange)
        saveAttempt(exercise, true, timeSpent)
        
        // Обновить статистику сессии
        val stats = sessionStats.value
        sessionStats.value = stats.copy(
            solved = stats.solved + 1,
            totalTime = stats.totalTime + timeSpent,
            averageTime = (stats.totalTime + timeSpent) / (stats.solved + 1)
        )
    }

    private fun handleFailure(exercise: Exercise, wrongMove: String) {
        attemptedWrongMove.value = true
        
        if (!showHint.value) {
            message.value = "❌ Неверно ($wrongMove). Попробуйте еще раз или возьмите подсказку."
        } else {
            // Уже показывали подсказку, но всё равно ошибка
            message.value = "❌ Всё ещё неверно. Правильный ход: ${exercise.solutionMoves.firstOrNull()}"
            updateRating(-5)
            
            val stats = sessionStats.value
            sessionStats.value = stats.copy(failed = stats.failed + 1)
        }
        
        // Откатываем доску на начальную позицию
        resetPosition()
    }

    private fun updateRating(change: Int) {
        val user = currentUser.value ?: return
        val newRating = (user.rating + change).coerceIn(100, 3500)

        val updatedUser = user.withNewRating(newRating)
        currentUser.value = updatedUser

        viewModelScope.launch {
            container.userRepository.update(updatedUser)
        }
    }

    private fun saveAttempt(exercise: Exercise, success: Boolean, timeSpent: Int) {
        val userId = currentUser.value?.id ?: return

        viewModelScope.launch {
            val attempt = ExerciseAttempt(
                userId = userId,
                exerciseId = exercise.id ?: 0,
                solved = success,
                timeSpentSeconds = timeSpent,
                movesMade = emptyList()
            )
            container.exerciseRepository.saveAttempt(attempt)
        }
    }

    fun resetPosition() {
        val exercise = currentExercise.value ?: return
        val newBoard = Board()
        newBoard.loadFromFen(exercise.fenPosition)
        boardState.value = newBoard
    }
    
    // ✅ НОВОЕ: Показать подсказку
    fun toggleHint() {
        showHint.value = !showHint.value
        
        if (showHint.value) {
            val bestMove = currentExercise.value?.solutionMoves?.firstOrNull()
            message.value = if (bestMove != null) {
                "💡 Подсказка: лучший ход — $bestMove"
            } else {
                "Подсказка недоступна"
            }
        } else {
            message.value = null
        }
    }
    
    // ✅ НОВОЕ: Сброс статистики сессии
    fun resetSessionStats() {
        sessionStats.value = SessionStats()
    }
}
