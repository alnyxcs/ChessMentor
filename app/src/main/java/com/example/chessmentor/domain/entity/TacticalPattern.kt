// domain/entity/TacticalPattern.kt
package com.example.chessmentor.domain.entity

import com.example.chessmentor.domain.analysis.TacticalMotif

/**
 * Классификация тактических паттернов для кластеризации
 */
enum class TacticalPattern(
    val displayName: String,
    val description: String
) {
    // ============================================================
    // ДВОЙНЫЕ УДАРЫ
    // ============================================================
    FORK_KNIGHT("Вилка конём", "Конь атакует две фигуры"),
    FORK_BISHOP("Вилка слоном", "Слон атакует две фигуры по диагонали"),
    FORK_ROOK("Вилка ладьёй", "Ладья атакует две фигуры"),
    FORK_QUEEN("Вилка ферзём", "Ферзь атакует две фигуры"),
    FORK_PAWN("Вилка пешкой", "Пешка атакует две фигуры"),

    // ============================================================
    // СВЯЗКИ И СКВОЗНЫЕ УДАРЫ
    // ============================================================
    PIN_ABSOLUTE("Абсолютная связка", "Фигура не может уйти (король под ударом)"),
    PIN_RELATIVE("Относительная связка", "Фигура прикрывает ценную фигуру"),
    SKEWER("Сквозной удар", "Атака через ценную фигуру на менее ценную"),

    // ============================================================
    // ОТКРЫТЫЕ НАПАДЕНИЯ
    // ============================================================
    DISCOVERED_ATTACK("Вскрытое нападение", "Уход фигуры открывает атаку"),
    DISCOVERED_CHECK("Вскрытый шах", "Уход фигуры открывает шах"),
    DOUBLE_CHECK("Двойной шах", "Шах от двух фигур одновременно"),

    // ============================================================
    // МАТОВЫЕ АТАКИ
    // ============================================================
    BACK_RANK_MATE("Мат на последней горизонтали", "Король заперт своими фигурами"),
    SMOTHERED_MATE("Спёртый мат", "Мат конём, король окружён"),
    ANASTASIA_MATE("Мат Анастасии", "Конь + ладья на краю доски"),
    ARABIAN_MATE("Арабский мат", "Конь + ладья"),
    BODEN_MATE("Мат Бодена", "Два слона на диагоналях"),

    // ============================================================
    // ТАКТИЧЕСКИЕ ПРИЁМЫ
    // ============================================================
    REMOVING_DEFENDER("Устранение защитника", "Размен или отвлечение защитника"),
    DEFLECTION("Отвлечение", "Фигуру заставляют уйти с важного поля"),
    DECOY("Завлечение", "Фигуру заманивают на невыгодное поле"),
    OVERLOADING("Перегрузка", "Фигура не справляется с двумя задачами"),

    TRAPPED_PIECE("Ловля фигуры", "Фигура не может уйти без потерь"),
    HANGING_PIECE("Незащищённая фигура", "Фигура под боем без защиты"),
    QUEEN_TRAP("Ловля ферзя", "Ферзь попадает в ловушку"),

    ZUGZWANG("Цугцванг", "Любой ход ухудшает позицию"),
    STALEMATE_TRICK("Трюк с патом", "Использование пата для ничьей"),
    PERPETUAL_CHECK("Вечный шах", "Серия шахов для ничьей"),

    // ============================================================
    // ПОЗИЦИОННЫЕ
    // ============================================================
    WEAK_SQUARE("Слабое поле", "Захват ключевого поля"),
    PAWN_BREAKTHROUGH("Прорыв пешки", "Пешка идёт в ферзи"),
    EXCHANGE_SACRIFICE("Жертва качества", "Жертва ладьи за лёгкую фигуру"),
    PIECE_SACRIFICE("Жертва фигуры", "Жертва для атаки"),

    // ============================================================
    // ЗАЩИТА
    // ============================================================
    DEFENSIVE_MOVE("Защитный ход", "Парирование угрозы"),
    COUNTER_ATTACK("Контратака", "Ответная угроза"),
    BLOCKADE("Блокада", "Остановка пешки/фигуры"),

    // ============================================================
    // ПРОЧЕЕ
    // ============================================================
    POSITIONAL("Позиционное улучшение", "Улучшение позиции без тактики"),
    ENDGAME_TECHNIQUE("Эндшпильная техника", "Реализация перевеса"),
    CALCULATION("Расчёт вариантов", "Сложный тактический расчёт"),

    MIXED("Комбинированная тактика", "Несколько мотивов");

    companion object {
        /**
         * Конвертация из TacticalMotif (из анализатора)
         */
        fun fromMotif(motif: TacticalMotif): TacticalPattern {
            return when (motif) {
                TacticalMotif.FORK -> FORK_KNIGHT
                TacticalMotif.PIN -> PIN_RELATIVE
                TacticalMotif.SKEWER -> SKEWER
                TacticalMotif.DISCOVERED_ATTACK -> DISCOVERED_ATTACK
                TacticalMotif.DISCOVERED_CHECK -> DISCOVERED_CHECK
                TacticalMotif.BACK_RANK -> BACK_RANK_MATE
                TacticalMotif.SMOTHERED_MATE -> SMOTHERED_MATE
                TacticalMotif.OVERLOADING -> OVERLOADING
                TacticalMotif.REMOVING_DEFENDER -> REMOVING_DEFENDER
                TacticalMotif.DEFLECTION -> DEFLECTION
                TacticalMotif.TRAPPED_PIECE -> TRAPPED_PIECE
                TacticalMotif.HANGING_PIECE -> HANGING_PIECE
                TacticalMotif.QUEEN_TRAP -> QUEEN_TRAP
            }
        }

        /**
         * Получить подтипы для более точной кластеризации
         */
        fun getSubtypes(pattern: TacticalPattern): List<String> {
            return when (pattern) {
                FORK_KNIGHT, FORK_BISHOP, FORK_ROOK, FORK_QUEEN, FORK_PAWN ->
                    listOf("with_check", "without_check", "family_fork")
                PIN_RELATIVE, PIN_ABSOLUTE ->
                    listOf("to_queen", "to_rook", "to_king")
                DISCOVERED_ATTACK, DISCOVERED_CHECK ->
                    listOf("with_check", "pure_attack")
                BACK_RANK_MATE ->
                    listOf("one_move", "two_moves", "with_sacrifice")
                else ->
                    listOf("standard")
            }
        }
    }
}