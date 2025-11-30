package com.example.chessmentor.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessmentor.di.AppContainer
import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.presentation.ui.components.BoardThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    var currentUser = mutableStateOf<User?>(null)

    // Состояния полей для редактирования
    var tempNickname = mutableStateOf("")
    var isSoundEnabled = mutableStateOf(true)
    var selectedThemeName = mutableStateOf("Classic")

    var message = mutableStateOf<String?>(null)

    fun init(user: User) {
        currentUser.value = user
        tempNickname.value = user.nickname
        isSoundEnabled.value = user.isSoundEnabled
        selectedThemeName.value = user.preferredTheme
    }

    fun saveSettings() {
        val user = currentUser.value ?: return

        if (tempNickname.value.isBlank()) {
            message.value = "❌ Никнейм не может быть пустым"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Создаем обновленного пользователя
                val updatedUser = user.copy(
                    nickname = tempNickname.value,
                    isSoundEnabled = isSoundEnabled.value,
                    preferredTheme = selectedThemeName.value
                )

                // Сохраняем в БД
                container.userRepository.update(updatedUser)

                withContext(Dispatchers.Main) {
                    currentUser.value = updatedUser
                    message.value = "✅ Настройки сохранены"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    message.value = "❌ Ошибка сохранения: ${e.message}"
                }
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        currentUser.value = null
        onLogout()
    }

    fun clearMessage() {
        message.value = null
    }
}