package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.Game
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.UserRepository
import java.time.Instant

/**
 * Use Case: Загрузка партии для анализа
 *
 * Бизнес-логика:
 * 1. Валидация PGN данных
 * 2. Проверка существования пользователя
 * 3. Проверка лимитов (для не-премиум пользователей)
 * 4. Сохранение партии в очередь на анализ
 */
class UploadGameUseCase(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {

    /**
     * Входные данные для загрузки партии
     */
    data class Input(
        val userId: Long,
        val pgnData: String,
        val playerColor: ChessColor,
        val timeControl: String? = null,
        val playedAt: Long = System.currentTimeMillis()
    )

    /**
     * Результат загрузки
     */
    sealed class Result {
        data class Success(val game: Game) : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        // Лимиты для разных типов пользователей
        const val FREE_USER_DAILY_LIMIT = 3
        const val FREE_USER_MONTHLY_LIMIT = 30
        const val MIN_PGN_LENGTH = 20
        const val MAX_PGN_LENGTH = 10000
    }

    /**
     * Выполнить загрузку партии
     */
    suspend fun execute(input: Input): Result { // <-- ИЗМЕНЕНО: Добавлен suspend
        // Проверка существования пользователя
        val user = userRepository.findById(input.userId) // <-- Теперь вызов корректен
            ?: return Result.Error("Пользователь не найден")

        // Валидация PGN
        val pgnValidation = validatePgn(input.pgnData)
        if (!pgnValidation.isValid) {
            return Result.Error(pgnValidation.errorMessage ?: "Некорректный PGN")
        }

        // Проверка лимитов для бесплатных пользователей
        if (!user.isPremium) {
            // TODO: В будущем здесь нужно будет вызывать suspend-функции для получения количества игр
            val todayGamesCount = getTodayGamesCount(input.userId)
            if (todayGamesCount >= FREE_USER_DAILY_LIMIT) {
                return Result.Error(
                    "Достигнут дневной лимит ($FREE_USER_DAILY_LIMIT партий). " +
                            "Обновитесь до Premium для безлимитного анализа!"
                )
            }

            val monthGamesCount = getMonthGamesCount(input.userId)
            if (monthGamesCount >= FREE_USER_MONTHLY_LIMIT) {
                return Result.Error(
                    "Достигнут месячный лимит ($FREE_USER_MONTHLY_LIMIT партий). " +
                            "Обновитесь до Premium для безлимитного анализа!"
                )
            }
        }

        // Создание партии
        val game = Game(
            userId = input.userId,
            pgnData = input.pgnData,
            playerColor = input.playerColor,
            timeControl = input.timeControl,
            playedAt = input.playedAt
        )

        // Сохранение
        val savedGame = gameRepository.save(game) // <-- Теперь вызов корректен

        return Result.Success(savedGame)
    }

    /**
     * Валидация PGN данных
     */
    private suspend  fun validatePgn(pgnData: String): PgnValidationResult {
        if (pgnData.isBlank()) {
            return PgnValidationResult(false, "PGN не может быть пустым")
        }

        if (pgnData.length < MIN_PGN_LENGTH) {
            return PgnValidationResult(false, "PGN слишком короткий")
        }

        if (pgnData.length > MAX_PGN_LENGTH) {
            return PgnValidationResult(false, "PGN слишком длинный (максимум $MAX_PGN_LENGTH символов)")
        }

        // Базовая проверка формата PGN
        if (!pgnData.contains("1.") && !pgnData.contains("[")) {
            return PgnValidationResult(false, "Не похоже на формат PGN")
        }

        // TODO: Добавить более детальную валидацию PGN через chess библиотеку

        return PgnValidationResult(true)
    }

    /**
     * Результат валидации PGN
     */
    private data class PgnValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Получить количество партий пользователя за сегодня
     */
    private suspend fun getTodayGamesCount(userId: Long): Int {
        // TODO: Реализовать через репозиторий
        // Пока возвращаем 0
        return 0
    }

    /**
     * Получить количество партий пользователя за месяц
     */
    private suspend fun getMonthGamesCount(userId: Long): Int {
        // TODO: Реализовать через репозиторий
        // Пока возвращаем 0
        return 0
    }
}