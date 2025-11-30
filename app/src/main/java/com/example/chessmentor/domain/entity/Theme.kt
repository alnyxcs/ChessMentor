package com.example.chessmentor.domain.entity

/**
 * Сущность: Тема (паттерн) в шахматах
 *
 * Представляет конкретную тему/паттерн:
 * например "вилка", "связка", "слабость последней горизонтали"
 */
data class Theme(
    val id: Long? = null,

    // ==================== ОСНОВНЫЕ ДАННЫЕ ====================
    val name: String,  // Например: "fork", "pin", "back_rank"
    val category: ThemeCategory,
    val description: String,
    val icon: String? = null  // Название иконки для UI
) {

    init {
        require(name.isNotBlank()) {
            "Название темы не может быть пустым"
        }
        require(description.isNotBlank()) {
            "Описание темы не может быть пустым"
        }
    }

    /**
     * Получить полное отображаемое имя
     * Например: "⚔️ Вилка"
     */
    fun getDisplayName(): String {
        val emoji = category.getIcon()
        return "$emoji $description"
    }

    /**
     * Получить название категории на русском
     */
    fun getCategoryName(): String = category.getDisplayName()

    override fun toString(): String {
        return "Theme(id=$id, name='$name', category=$category)"
    }
}

/**
 * Предопределённые темы для быстрого создания
 */
object PredefinedThemes {

    // ТАКТИЧЕСКИЕ ТЕМЫ
    val FORK = Theme(
        name = "fork",
        category = ThemeCategory.TACTICS,
        description = "Вилка",
        icon = "knight_fork"
    )

    val PIN = Theme(
        name = "pin",
        category = ThemeCategory.TACTICS,
        description = "Связка",
        icon = "pin_icon"
    )

    val SKEWER = Theme(
        name = "skewer",
        category = ThemeCategory.TACTICS,
        description = "Рентген",
        icon = "skewer_icon"
    )

    val DISCOVERED_ATTACK = Theme(
        name = "discovered_attack",
        category = ThemeCategory.TACTICS,
        description = "Вскрытая атака",
        icon = "discovery_icon"
    )

    val DOUBLE_CHECK = Theme(
        name = "double_check",
        category = ThemeCategory.TACTICS,
        description = "Двойной шах",
        icon = "double_check_icon"
    )

    val SACRIFICE = Theme(
        name = "sacrifice",
        category = ThemeCategory.TACTICS,
        description = "Жертва",
        icon = "sacrifice_icon"
    )

    val BACK_RANK = Theme(
        name = "back_rank",
        category = ThemeCategory.TACTICS,
        description = "Слабость последней горизонтали",
        icon = "back_rank_icon"
    )

    // СТРАТЕГИЧЕСКИЕ ТЕМЫ
    val WEAK_SQUARES = Theme(
        name = "weak_squares",
        category = ThemeCategory.STRATEGY,
        description = "Слабые поля",
        icon = "weak_square_icon"
    )

    val PAWN_STRUCTURE = Theme(
        name = "pawn_structure",
        category = ThemeCategory.STRATEGY,
        description = "Пешечная структура",
        icon = "pawn_icon"
    )

    val PIECE_ACTIVITY = Theme(
        name = "piece_activity",
        category = ThemeCategory.STRATEGY,
        description = "Активность фигур",
        icon = "activity_icon"
    )

    // ДЕБЮТНЫЕ ТЕМЫ
    val OPENING_PRINCIPLES = Theme(
        name = "opening_principles",
        category = ThemeCategory.OPENING,
        description = "Принципы дебюта",
        icon = "opening_icon"
    )

    val DEVELOPMENT = Theme(
        name = "development",
        category = ThemeCategory.OPENING,
        description = "Развитие фигур",
        icon = "development_icon"
    )

    val CENTER_CONTROL = Theme(
        name = "center_control",
        category = ThemeCategory.OPENING,
        description = "Контроль центра",
        icon = "center_icon"
    )

    // ЭНДШПИЛЬНЫЕ ТЕМЫ
    val KING_ACTIVITY = Theme(
        name = "king_activity",
        category = ThemeCategory.ENDGAME,
        description = "Активность короля",
        icon = "king_endgame_icon"
    )

    val PAWN_ENDGAME = Theme(
        name = "pawn_endgame",
        category = ThemeCategory.ENDGAME,
        description = "Пешечный эндшпиль",
        icon = "pawn_endgame_icon"
    )

    // БЕЗОПАСНОСТЬ КОРОЛЯ
    val KING_SAFETY = Theme(
        name = "king_safety",
        category = ThemeCategory.KING_SAFETY,
        description = "Безопасность короля",
        icon = "king_safety_icon"
    )

    val CASTLING = Theme(
        name = "castling",
        category = ThemeCategory.KING_SAFETY,
        description = "Рокировка",
        icon = "castling_icon"
    )

    /**
     * Получить все предопределённые темы
     */
    fun getAll(): List<Theme> = listOf(
        FORK, PIN, SKEWER, DISCOVERED_ATTACK, DOUBLE_CHECK, SACRIFICE, BACK_RANK,
        WEAK_SQUARES, PAWN_STRUCTURE, PIECE_ACTIVITY,
        OPENING_PRINCIPLES, DEVELOPMENT, CENTER_CONTROL,
        KING_ACTIVITY, PAWN_ENDGAME,
        KING_SAFETY, CASTLING
    )
}