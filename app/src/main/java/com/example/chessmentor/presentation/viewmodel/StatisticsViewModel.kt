package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // <-- Добавьте этот импорт
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.usecase.GetUserStatisticsUseCase
import kotlinx.coroutines.launch // <-- Добавьте этот импорт

/**
 * ViewModel для экрана статистики
 */
class StatisticsViewModel(
    container: AppContainer
) : ViewModel() {

    // Убираем лишнюю переменную, так как container можно использовать напрямую
    private val container = container

    var statistics = mutableStateOf<GetUserStatisticsUseCase.UserStatistics?>(null)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    /**
     * Загрузить статистику для пользователя
     */
    fun loadStatistics(user: User) {
        // Запускаем корутину в viewModelScope
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            val input = GetUserStatisticsUseCase.Input(user.id!!)

            // Теперь вызов suspend-функции происходит внутри корутины
            when (val result = container.getUserStatisticsUseCase.execute(input)) {
                is GetUserStatisticsUseCase.Result.Success -> {
                    statistics.value = result.statistics
                }
                is GetUserStatisticsUseCase.Result.Error -> {
                    errorMessage.value = result.message
                }
            }

            isLoading.value = false
        }
    }
}