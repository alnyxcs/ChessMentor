// domain/entity/TacticalTheme.kt
package com.example.chessmentor.domain.entity

enum class TacticalTheme {
    UNKNOWN,            // Тема не определена
    TACTIC,             // Общая тактика (по умолчанию)
    MATE,               // Мат / Угроза мата
    MATERIAL_LOSS,      // Просто зевок фигуры
    FORK,               // Двойной удар
    PIN,                // Связка
    DISCOVERED_ATTACK,  // Вскрытое нападение
    KING_SAFETY         // Опасность для короля
}