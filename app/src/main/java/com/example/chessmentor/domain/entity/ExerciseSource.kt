// domain/entity/ExerciseSource.kt
package com.example.chessmentor.domain.entity

/**
 * Источник создания упражнения
 */
enum class ExerciseSource {
    /**
     * Создано из ошибки в партии пользователя
     */
    MISTAKE,

    /**
     * Импортировано из внешней базы
     */
    IMPORTED,

    /**
     * Сгенерировано автоматически
     */
    GENERATED,

    /**
     * Создано вручную (для тестов)
     */
    MANUAL
}