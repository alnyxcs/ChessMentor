// domain/opening/OpeningModels.kt
package com.example.chessmentor.domain.opening

/**
 * Представление дебютной позиции
 */
data class OpeningPosition(
    val id: String,                          // Уникальный идентификатор
    val fenPattern: String,                  // Паттерн FEN для сопоставления
    val name: String,                        // Название на русском
    val nameEn: String,                      // Название на английском
    val eco: String,                         // ECO код (A00-E99)
    val moves: List<String>,                 // Последовательность ходов (SAN)
    val moveCount: Int,                      // Количество ходов в дебюте
    val isMainLine: Boolean,                 // Главная линия или вариант
    val characteristics: Set<OpeningCharacteristic>,
    val commonMistakes: List<String> = emptyList(),
    val keyIdeas: List<String> = emptyList()
)

/**
 * Характеристики дебюта
 */
enum class OpeningCharacteristic(val description: String) {
    // Развитие фигур
    EARLY_QUEEN_DEVELOPMENT("Ранний выход ферзя"),
    FIANCHETTO("Фианкетто слона"),
    QUICK_DEVELOPMENT("Быстрое развитие"),
    DELAYED_DEVELOPMENT("Отложенное развитие"),

    // Стратегия центра
    CLASSICAL_CENTER("Классический центр пешками"),
    HYPERMODERN("Контроль центра фигурами"),
    OPEN_CENTER("Открытый центр"),
    CLOSED_CENTER("Закрытый центр"),
    SEMI_OPEN("Полуоткрытая игра"),

    // Тип игры
    GAMBIT("Гамбит - жертва материала"),
    COUNTER_GAMBIT("Контргамбит"),
    POSITIONAL("Позиционная игра"),
    TACTICAL("Тактическая игра"),

    SLOW_MANEUVERING("Медленное маневрирование"),
    // Фланги и атака
    KINGSIDE_ATTACK("Атака на королевском фланге"),
    QUEENSIDE_ATTACK("Атака на ферзевом фланге"),
    WING_ATTACK("Фланговая атака"),

    // Специальные
    SYMMETRICAL("Симметричная структура"),
    ASYMMETRICAL("Асимметричная структура"),
    SOLID_DEFENSE("Надёжная защита"),
    COUNTER_ATTACK("Контратака"),
    PROVOCATIVE("Провокационная игра"),
    FLEXIBLE("Гибкая система"),
    AGGRESSIVE("Агрессивная игра"),
    OPEN_GAME("Открытая игра"),
    PAWN_STORM("Пешечный шторм"),
    PIECE_SACRIFICE("Жертва фигуры"),
    EARLY_CASTLING("Ранняя рокировка"),
    DELAYED_CASTLING("Поздняя рокировка")
}

/**
 * Результат поиска дебюта
 */
sealed class OpeningMatch {
    data class Found(
        val opening: OpeningPosition,
        val confidence: MatchConfidence
    ) : OpeningMatch()

    object NotFound : OpeningMatch()

    data class PartialMatch(
        val opening: OpeningPosition,
        val divergedAtMove: Int
    ) : OpeningMatch()
}

/**
 * Уровень уверенности в совпадении
 */
enum class MatchConfidence {
    EXACT,      // Точное совпадение FEN
    HIGH,       // Совпадение первых 40+ символов
    MEDIUM,     // Совпадение основной структуры
    LOW         // Похожие характеристики
}

/**
 * Контекст для позиционных проверок
 */
data class OpeningContext(
    val isKnownOpening: Boolean,
    val openingName: String?,
    val allowsEarlyQueen: Boolean,
    val allowsFlankPlay: Boolean,
    val expectedCenterStrategy: CenterStrategy,
    val phase: OpeningPhase
)

enum class CenterStrategy {
    OCCUPY_WITH_PAWNS,      // Занимать центр пешками (классика)
    CONTROL_WITH_PIECES,    // Контролировать фигурами (гипермодерн)
    MIXED,                  // Смешанная стратегия
    FLEXIBLE                // Гибкий подход
}

enum class OpeningPhase {
    EARLY_OPENING,      // Ходы 1-5
    MID_OPENING,        // Ходы 6-10
    LATE_OPENING,       // Ходы 11-15
    TRANSITION          // Переход к миттельшпилю (16+)
}