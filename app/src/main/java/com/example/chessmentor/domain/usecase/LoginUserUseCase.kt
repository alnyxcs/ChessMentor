package com.example.chessmentor.domain.usecase

import com.example.chessmentor.domain.entity.User
import com.example.chessmentor.domain.repository.UserRepository

/**
 * Use Case: Вход пользователя в систему
 *
 * Бизнес-логика авторизации:
 * 1. Поиск пользователя по email
 * 2. Проверка пароля
 * 3. Обновление времени последнего входа
 */
class LoginUserUseCase(
    private  val userRepository: UserRepository
) {

    /**
     * Входные данные для входа
     */
    data class Input(
        val email: String,
        val password: String
    )

    /**
     * Результат входа
     */
    sealed class Result {
        data class Success(val user: User) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Выполнить вход
     */
    suspend fun execute(input: Input): Result {
        // Валидация входных данных
        if (input.email.isBlank()) {
            return Result.Error("Email не может быть пустым")
        }

        if (input.password.isBlank()) {
            return Result.Error("Пароль не может быть пустым")
        }

        // Поиск пользователя по email
        val user = userRepository.findByEmail(input.email)
            ?: return Result.Error("Пользователь с таким email не найден")

        // Проверка пароля
        val passwordHash = hashPassword(input.password)
        if (user.passwordHash != passwordHash) {
            return Result.Error("Неверный пароль")
        }

        // Обновление времени последнего входа
        val updatedUser = user.withLogin()
        userRepository.update(updatedUser)

        return Result.Success(updatedUser)
    }

    /**
     * Хеширование пароля (должно совпадать с RegisterUserUseCase)
     * TODO: Заменить на настоящий bcrypt
     */
    private fun hashPassword(password: String): String {
        return "hashed_$password"
    }
}