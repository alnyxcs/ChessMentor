package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.entity.PredefinedThemes
import com.example.chessmentor.domain.entity.Theme
import com.example.chessmentor.domain.entity.ProblemTheme  // ДОБАВИТЬ
import com.example.chessmentor.domain.repository.GameRepository
import com.example.chessmentor.domain.repository.MistakeRepository
import com.example.chessmentor.domain.repository.UserRepository


class GetUserStatisticsUseCase(
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val mistakeRepository: MistakeRepository
) {

    data class Input(
        val userId: Long
    )

    data class UserStatistics(
        val userId: Long,
        val nickname: String,
        val currentRating: Int,
        val skillLevel: String,
        val totalGames: Int,
        val analyzedGames: Int,
        val averageAccuracy: Double,
        val totalMistakes: Int,
        val blunders: Int,
        val mistakes: Int,
        val inaccuracies: Int,
        val averageEvaluationLoss: Double,
        val problemThemes: List<ProblemTheme>,  // ИЗМЕНЕНО
        val ratingChange: Int,
        val accuracyTrend: String,
        val recommendations: List<String>
    )

    // УДАЛИТЬ ThemeStatistics - теперь используем ProblemTheme

    sealed class Result {
        data class Success(val statistics: UserStatistics) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun execute(input: Input): Result {
        val user = userRepository.findById(input.userId)
            ?: return Result.Error("Пользователь не найден")

        val allGames = gameRepository.findByUserId(input.userId)
        val analyzedGames = allGames.filter { it.isAnalyzed() }

        val totalGames = allGames.size
        val analyzedCount = analyzedGames.size
        val averageAccuracy = if (analyzedGames.isNotEmpty()) {
            analyzedGames.mapNotNull { it.accuracy }.average()
        } else {
            0.0
        }

        val allMistakes = mistakeRepository.findByUserId(input.userId)

        val blundersCount = allMistakes.count { it.mistakeType == MistakeType.BLUNDER }
        val mistakesCount = allMistakes.count { it.mistakeType == MistakeType.MISTAKE }
        val inaccuraciesCount = allMistakes.count { it.mistakeType == MistakeType.INACCURACY }

        val avgEvalLoss = if (allMistakes.isNotEmpty()) {
            allMistakes.map { it.evaluationLoss }.average()
        } else {
            0.0
        }

        val themeFrequency = mistakeRepository.getMostFrequentThemesByUserId(input.userId, 5)
        val problemThemes = calculateProblemThemes(themeFrequency, allMistakes.size)

        val ratingChange = calculateRatingChange(user.rating)
        val accuracyTrend = calculateAccuracyTrend(analyzedGames)

        val recommendations = generateRecommendations(
            blundersCount = blundersCount,
            mistakesCount = mistakesCount,
            problemThemes = problemThemes,
            averageAccuracy = averageAccuracy
        )

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

    private suspend fun calculateProblemThemes(
        themeFrequency: Map<Long, Int>,
        totalMistakes: Int
    ): List<ProblemTheme> {  // ИЗМЕНЕНО
        if (themeFrequency.isEmpty() || totalMistakes == 0) {
            return emptyList()
        }

        return themeFrequency.map { (themeId, count) ->
            val theme = getThemeById(themeId)

            ProblemTheme(  // ИЗМЕНЕНО
                themeName = theme.description,
                category = theme.category.getDisplayName(),
                mistakeCount = count,
                percentage = (count.toDouble() / totalMistakes) * 100
            )
        }.sortedByDescending { it.mistakeCount }
    }

    private suspend fun getThemeById(id: Long): Theme {
        return PredefinedThemes.getAll().firstOrNull { it.id == id }
            ?: PredefinedThemes.PIECE_ACTIVITY
    }

    private suspend fun calculateRatingChange(currentRating: Int): Int {
        return +12
    }

    private suspend fun calculateAccuracyTrend(
        analyzedGames: List<com.example.chessmentor.domain.entity.Game>
    ): String {
        if (analyzedGames.size < 5) {
            return "stable"
        }

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

    private suspend fun generateRecommendations(
        blundersCount: Int,
        mistakesCount: Int,
        problemThemes: List<ProblemTheme>,  // ИЗМЕНЕНО
        averageAccuracy: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (blundersCount > 5) {
            recommendations.add(
                "У вас много грубых ошибок ($blundersCount). " +
                        "Рекомендуем больше времени уделять расчёту вариантов."
            )
        }

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

        if (problemThemes.isNotEmpty()) {
            val topProblem = problemThemes.first()
            recommendations.add(
                "Чаще всего ошибки в теме '${topProblem.themeName}' " +
                        "(${topProblem.mistakeCount} раз). Изучите эту тему подробнее."
            )
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                "Вы играете стабильно! Продолжайте регулярно анализировать партии."
            )
        }

        return recommendations
    }
}