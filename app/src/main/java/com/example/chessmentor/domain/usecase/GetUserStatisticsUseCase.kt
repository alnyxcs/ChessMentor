package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.entity.PredefinedThemes
import com.example.chessmentor.domain.entity.Theme
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository

/**
 * Use Case: Получение статистики пользователя
 *
 * Собирает полную статистику:
 * - Общие показатели
 * - Прогресс по времени
 * - Проблемные темы
 * - Рекомендации
 */
class GetUserStatisticsUseCase(
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository
) {

    /**
     * Входные данные
     */
    data class Input(
        val userId: Long
    )

    /**
     * Результат - статистика пользователя
     */
    data class UserStatistics(
        // Основная информация
        val userId: Long,
        val nickname: String,
        val currentRating: Int,
        val skillLevel: String,

        // Статистика по партиям
        val totalGames: Int,
        val analyzedGames: Int,
        val averageAccuracy: Double,

        // Статистика по ошибкам
        val totalMistakes: Int,
        val blunders: Int,
        val mistakes: Int,
        val inaccuracies: Int,
        val averageEvaluationLoss: Double,

        // Проблемные темы (топ-5)
        val problemThemes: List<ThemeStatistics>,

        // Прогресс
        val ratingChange: Int,  // За последние 30 дней
        val accuracyTrend: String, // "improving", "stable", "declining"

        // Рекомендации
        val recommendations: List<String>
    )

    /**
     * Статистика по теме
     */
    data class ThemeStatistics(
        val themeName: String,
        val category: String,
        val mistakeCount: Int,
        val percentage: Double  // Процент от всех ошибок
    )

    /**
     * Результат выполнения
     */
    sealed class Result {
        data class Success(val statistics: UserStatistics) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Выполнить получение статистики
     */
    suspend fun execute(input: Input): Result { // <-- ИЗМЕНЕНО: Добавлен suspend
        // Получение пользователя
        val user = userRepository.findById(input.userId) // <-- Теперь вызов корректен
            ?: return Result.Error("Пользователь не найден")

        // Получение партий
        val allGames = gameRepository.findByUserId(input.userId) // <-- Теперь вызов корректен
        val analyzedGames = allGames.filter { it.isAnalyzed() }

        // Расчёт статистики по партиям
        val totalGames = allGames.size
        val analyzedCount = analyzedGames.size
        val averageAccuracy = if (analyzedGames.isNotEmpty()) {
            analyzedGames.mapNotNull { it.accuracy }.average()
        } else {
            0.0
        }

        // Получение всех ошибок
        val allMistakes = mistakeRepository.findByUserId(input.userId) // <-- Теперь вызов корректен

        // Подсчёт по типам
        val blundersCount = allMistakes.count { it.mistakeType == MistakeType.BLUNDER }
        val mistakesCount = allMistakes.count { it.mistakeType == MistakeType.MISTAKE }
        val inaccuraciesCount = allMistakes.count { it.mistakeType == MistakeType.INACCURACY }

        // Средняя потеря оценки
        val avgEvalLoss = if (allMistakes.isNotEmpty()) {
            allMistakes.map { it.evaluationLoss }.average()
        } else {
            0.0
        }

        // Проблемные темы
        val themeFrequency = mistakeRepository.getMostFrequentThemesByUserId(input.userId, 5) // <-- Теперь вызов корректен
        val problemThemes = calculateProblemThemes(themeFrequency, allMistakes.size)

        // Прогресс (упрощённый расчёт)
        val ratingChange = calculateRatingChange(user.rating)
        val accuracyTrend = calculateAccuracyTrend(analyzedGames)

        // Генерация рекомендаций
        val recommendations = generateRecommendations(
            blundersCount = blundersCount,
            mistakesCount = mistakesCount,
            problemThemes = problemThemes,
            averageAccuracy = averageAccuracy
        )

        // Сборка результата
        val statistics = UserStatistics(
            userId = user.id!!,
            nickname = user.nickname,
            currentRating = user.rating,
            skillLevel = user.skillLevel.getDisplayName(),
            totalGames = totalGames,
            analyzedGames = analyzedCount,
            averageAccuracy = averageAccuracy,
            totalMistakes = allMistakes.size,
            blunders = blundersCount,
            mistakes = mistakesCount,
            inaccuracies = inaccuraciesCount,
            averageEvaluationLoss = avgEvalLoss,
            problemThemes = problemThemes,
            ratingChange = ratingChange,
            accuracyTrend = accuracyTrend,
            recommendations = recommendations
        )

        return Result.Success(statistics)
    }

    /**
     * Расчёт проблемных тем
     */
    private  suspend fun calculateProblemThemes(
        themeFrequency: Map<Long, Int>,
        totalMistakes: Int
    ): List<ThemeStatistics> {
        if (themeFrequency.isEmpty() || totalMistakes == 0) {
            return emptyList()
        }

        return themeFrequency.map { (themeId, count) ->
            // В реальном приложении нужно получать тему из БД
            val theme = getThemeById(themeId)

            ThemeStatistics(
                themeName = theme.description,
                category = theme.category.getDisplayName(),
                mistakeCount = count,
                percentage = (count.toDouble() / totalMistakes) * 100
            )
        }.sortedByDescending { it.mistakeCount }
    }

    /**
     * Получение темы по ID (заглушка)
     */
    private suspend fun getThemeById(id: Long): Theme {
        // В реальном приложении получать из репозитория
        return PredefinedThemes.getAll().firstOrNull { it.id == id }
            ?: PredefinedThemes.PIECE_ACTIVITY
    }

    /**
     * Расчёт изменения рейтинга (заглушка)
     */
    private  suspend fun calculateRatingChange(currentRating: Int): Int {
        // В реальном приложении сравнивать с рейтингом месяц назад
        return +12  // Временная заглушка
    }

    /**
     * Расчёт тренда точности
     */
    private  suspend fun calculateAccuracyTrend(analyzedGames: List<com.example.chessmentor.domain.entity.Game>): String {
        if (analyzedGames.size < 5) {
            return "stable"
        }

        // Сравнение средней точности первой и второй половины партий
        val sortedByDate = analyzedGames.sortedBy { it.playedAt }
        val midpoint = sortedByDate.size / 2

        val firstHalf = sortedByDate.take(midpoint)
        val secondHalf = sortedByDate.drop(midpoint)

        val firstAvg = firstHalf.mapNotNull { it.accuracy }.average()
        val secondAvg = secondHalf.mapNotNull { it.accuracy }.average()

        return when {
            secondAvg > firstAvg + 5 -> "improving"
            secondAvg < firstAvg - 5 -> "declining"
            else -> "stable"
        }
    }

    /**
     * Генерация рекомендаций на основе статистики
     */
    private  suspend fun generateRecommendations(
        blundersCount: Int,
        mistakesCount: Int,
        problemThemes: List<ThemeStatistics>,
        averageAccuracy: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Рекомендации по грубым ошибкам
        if (blundersCount > 5) {
            recommendations.add(
                "У вас много грубых ошибок ($blundersCount). " +
                        "Рекомендуем больше времени уделять расчёту вариантов."
            )
        }

        // Рекомендации по точности
        when {
            averageAccuracy < 70 -> {
                recommendations.add(
                    "Ваша средняя точность игры низкая (${averageAccuracy.toInt()}%). " +
                            "Попробуйте играть в более медленный контроль времени."
                )
            }
            averageAccuracy > 85 -> {
                recommendations.add(
                    "Отличная точность игры (${averageAccuracy.toInt()}%)! " +
                            "Можете попробовать играть против более сильных соперников."
                )
            }
        }

        // Рекомендации по проблемным темам
        if (problemThemes.isNotEmpty()) {
            val topProblem = problemThemes.first()
            recommendations.add(
                "Чаще всего ошибки в теме '${topProblem.themeName}' " +
                        "(${topProblem.mistakeCount} раз). Изучите эту тему подробнее."
            )
        }

        // Общая рекомендация если всё хорошо
        if (recommendations.isEmpty()) {
            recommendations.add(
                "Вы играете стабильно! Продолжайте регулярно анализировать партии."
            )
        }

        return recommendations
    }
}