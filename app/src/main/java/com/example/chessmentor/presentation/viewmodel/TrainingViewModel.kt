package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.Exercise
import com.example.chessmentor.domain.entity.ExerciseAttempt
import com.example.chessmentor.domain.entity.User
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

    fun init(user: User) {
        if (currentUser.value == null || currentUser.value?.id != user.id) {
            currentUser.value = user
            loadNextExercise()
        }
    }

    fun loadNextExercise() {
        val userId = currentUser.value?.id ?: return
        isSolved.value = false
        message.value = null

        viewModelScope.launch {
            val input = GetTrainingExerciseUseCase.Input(userId)
            when (val result = container.getTrainingExerciseUseCase.execute(input)) {
                is GetTrainingExerciseUseCase.Result.Success -> {
                    currentExercise.value = result.exercise
                    // Инициализируем доску FEN-ом задачи
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
        }
    }

    fun onMoveMade(move: Move) {
        val exercise = currentExercise.value ?: return
        if (isSolved.value) return

        try {
            // 1. Ищем легальный ход
            val legalMoves = boardState.value.legalMoves()
            val legalMove = legalMoves.find { it.from == move.from && it.to == move.to }

            if (legalMove != null) {
                // 2. Делаем ход на доске
                val newBoard = Board()
                newBoard.loadFromFen(boardState.value.fen)
                newBoard.doMove(legalMove)
                boardState.value = newBoard

                // 3. Безопасно получаем SAN
                val moveSan = try {
                    legalMove.san ?: "${move.from}-${move.to}" // Фолбек, если SAN null
                } catch (e: Exception) {
                    "${move.from}-${move.to}"
                }

                // 4. Проверяем правильность
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
        message.value = "✅ Правильно! Рейтинг +10"

        updateRating(10)
        saveAttempt(exercise, true)
    }

    private fun handleFailure(exercise: Exercise, wrongMove: String) {
        message.value = "❌ Неверно ($wrongMove). Попробуйте еще раз."

        // Откатываем ход на доске (визуально)
        // Но можно и оставить, чтобы пользователь видел ошибку.
        // Давайте оставим, но дадим кнопку "Сброс"

        // Рейтинг снижаем только один раз за задачу (можно добавить флаг)
        updateRating(-5)
        saveAttempt(exercise, false)
    }

    private fun updateRating(change: Int) {
        val user = currentUser.value ?: return
        val newRating = (user.rating + change).coerceIn(100, 3500)

        // Обновляем объект User в памяти
        val updatedUser = user.withNewRating(newRating)
        currentUser.value = updatedUser

        // Сохраняем в БД
        viewModelScope.launch {
            container.userRepository.update(updatedUser)
        }
    }

    private fun saveAttempt(exercise: Exercise, success: Boolean) {
        val userId = currentUser.value?.id ?: return

        viewModelScope.launch {
            val attempt = ExerciseAttempt(
                userId = userId,
                exerciseId = exercise.id ?: 0,
                solved = success,
                timeSpentSeconds = 0, // Пока 0, можно добавить таймер
                movesMade = emptyList() // Можно сохранять ходы
            )
            container.exerciseRepository.saveAttempt(attempt)
        }
    }

    fun resetPosition() {
        val exercise = currentExercise.value ?: return
        val newBoard = Board()
        newBoard.loadFromFen(exercise.fenPosition)
        boardState.value = newBoard
        message.value = null
        // isSolved не сбрасываем, чтобы нельзя было нафармить рейтинг на одной задаче
    }
}