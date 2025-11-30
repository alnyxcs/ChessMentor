package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.usecase.AnalyzeGameUseCase
import com.example.chessmentor.domain.usecase.UploadGameUseCase
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel для управления партиями
 */
class GameViewModel(
    container: AppContainer
) : ViewModel() {

    private val container: AppContainer

    init {
        this.container = container
    }

    // Текущий пользователь
    var currentUser = mutableStateOf<User?>(null)

    // Список партий пользователя
    val userGames = mutableStateListOf<Game>()

    // Все ошибки пользователя
    private val _userMistakes = mutableStateListOf<Mistake>()
    val userMistakes: List<Mistake> = _userMistakes

    // Текущая партия для просмотра
    var selectedGame = mutableStateOf<Game?>(null)

    // Ошибки в текущей партии
    val selectedGameMistakes = mutableStateListOf<Mistake>()

    // Поля формы загрузки
    var pgnInput = mutableStateOf("")
    var selectedColor = mutableStateOf(ChessColor.WHITE)
    var timeControl = mutableStateOf("")

    // Сообщения
    var message = mutableStateOf("")
    var isLoading = mutableStateOf(false)

    /**
     * Установить текущего пользователя и загрузить его данные
     */
    fun setUser(user: User) {
        currentUser.value = user
        loadUserData()
    }

    /**
     * Загрузить все данные пользователя (партии и ошибки)
     */
    fun loadUserData() {
        val userId = currentUser.value?.id ?: return

        viewModelScope.launch {
            try {
                // Загружаем партии
                val games = container.gameRepository.findByUserId(userId)
                userGames.clear()
                userGames.addAll(games.sortedByDescending { it.playedAt })

                // Загружаем ошибки
                val mistakes = container.mistakeRepository.findByUserId(userId)
                _userMistakes.clear()
                _userMistakes.addAll(mistakes)
            } catch (e: Exception) {
                message.value = "❌ Ошибка загрузки данных: ${e.message}"
            }
        }
    }

    /**
     * Получить ошибки для конкретной игры
     */
    fun getMistakesForGame(gameId: Long): List<Mistake> {
        return _userMistakes.filter { it.gameId == gameId }
    }

    /**
     * Загрузить партию на анализ
     */
    fun uploadGame() {
        val userId = currentUser.value?.id
        if (userId == null) {
            message.value = "❌ Необходимо войти в систему"
            return
        }

        if (pgnInput.value.isBlank()) {
            message.value = "❌ Введите PGN партии"
            return
        }

        isLoading.value = true

        viewModelScope.launch {
            try {
                val input = UploadGameUseCase.Input(
                    userId = userId,
                    pgnData = pgnInput.value,
                    playerColor = selectedColor.value,
                    timeControl = timeControl.value.ifBlank { null },
                    playedAt = System.currentTimeMillis()
                )

                when (val result = container.uploadGameUseCase.execute(input)) {
                    is UploadGameUseCase.Result.Success -> {
                        message.value = "✅ Партия загружена! Запускаем анализ..."

                        // Очищаем форму
                        pgnInput.value = ""
                        timeControl.value = ""

                        // Анализируем партию
                        analyzeGame(result.game.id!!)
                    }
                    is UploadGameUseCase.Result.Error -> {
                        message.value = "❌ Ошибка: ${result.message}"
                        isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                message.value = "❌ Ошибка: ${e.message}"
                isLoading.value = false
            }
        }
    }

    /**
     * Анализировать партию
     */
    private fun analyzeGame(gameId: Long) {
        viewModelScope.launch {
            try {
                val input = AnalyzeGameUseCase.Input(gameId = gameId)

                when (val result = container.analyzeGameUseCase.execute(input)) {
                    is AnalyzeGameUseCase.Result.Success -> {
                        message.value = "✅ Анализ завершён! Найдено ${result.mistakes.size} ошибок"
                        isLoading.value = false

                        // Обновляем данные
                        loadUserData()

                        // Открываем партию для просмотра
                        selectGame(result.game)
                    }
                    is AnalyzeGameUseCase.Result.Error -> {
                        message.value = "❌ Ошибка анализа: ${result.message}"
                        isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                message.value = "❌ Ошибка анализа: ${e.message}"
                isLoading.value = false
            }
        }
    }

    /**
     * Выбрать партию для просмотра
     */
    fun selectGame(game: Game) {
        selectedGame.value = game

        // Получаем ошибки из уже загруженных данных
        if (game.id != null) {
            val mistakes = getMistakesForGame(game.id!!)
            selectedGameMistakes.clear()
            selectedGameMistakes.addAll(mistakes.sortedBy { it.moveNumber })
        }
    }

    /**
     * Закрыть просмотр партии
     */
    fun closeGameView() {
        selectedGame.value = null
        selectedGameMistakes.clear()
    }

    /**
     * Удалить партию
     */
    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            try {
                // Удаляем игру из БД
                container.gameRepository.deleteById(gameId)

                // Удаляем связанные ошибки из БД
                container.mistakeRepository.deleteByGameId(gameId)

                // Удаляем из локальных списков
                userGames.removeAll { it.id == gameId }
                _userMistakes.removeAll { it.gameId == gameId }

                message.value = "✅ Партия удалена"
            } catch (e: Exception) {
                message.value = "❌ Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Очистить сообщение
     */
    fun clearMessage() {
        message.value = ""
    }
}