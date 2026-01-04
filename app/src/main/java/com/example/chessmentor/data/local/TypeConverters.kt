// data/local/TypeConverters.kt
package com.example.chessmentor.data.local

import androidx.room.TypeConverter
import com.example.chessmentor.domain.entity.AnalysisStatus
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.ExerciseSource
import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.entity.MoveQuality
import com.example.chessmentor.domain.entity.SkillLevel
import com.example.chessmentor.domain.entity.TacticalPattern
import com.example.chessmentor.domain.entity.TacticalTheme

class TypeConverters {

    // ============================================================
    // LIST CONVERTERS
    // ============================================================

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // ============================================================
    // SKILL LEVEL
    // ============================================================

    @TypeConverter
    fun fromSkillLevel(value: SkillLevel?): String? = value?.name

    @TypeConverter
    fun toSkillLevel(value: String?): SkillLevel? = value?.let {
        try { SkillLevel.valueOf(it) } catch (e: Exception) { null }
    }

    // ============================================================
    // CHESS COLOR
    // ============================================================

    @TypeConverter
    fun fromChessColor(value: ChessColor?): String? = value?.name

    @TypeConverter
    fun toChessColor(value: String?): ChessColor? = value?.let {
        try { ChessColor.valueOf(it) } catch (e: Exception) { null }
    }

    // ============================================================
    // ANALYSIS STATUS
    // ============================================================

    @TypeConverter
    fun fromAnalysisStatus(value: AnalysisStatus?): String? = value?.name

    @TypeConverter
    fun toAnalysisStatus(value: String?): AnalysisStatus? = value?.let {
        try { AnalysisStatus.valueOf(it) } catch (e: Exception) { null }
    }

    // ============================================================
    // MISTAKE TYPE
    // ============================================================

    @TypeConverter
    fun fromMistakeType(value: MistakeType?): String? = value?.name

    @TypeConverter
    fun toMistakeType(value: String?): MistakeType? = value?.let {
        try { MistakeType.valueOf(it) } catch (e: Exception) { null }
    }

    // ============================================================
    // MOVE QUALITY
    // ============================================================

    @TypeConverter
    fun fromMoveQuality(quality: MoveQuality): String = quality.name

    @TypeConverter
    fun toMoveQuality(value: String): MoveQuality {
        return try { MoveQuality.valueOf(value) } catch (e: Exception) { MoveQuality.BOOK }
    }

    // ============================================================
    // TACTICAL PATTERN (НОВОЕ)
    // ============================================================

    @TypeConverter
    fun fromTacticalPattern(value: TacticalPattern?): String? = value?.name

    @TypeConverter
    fun toTacticalPattern(value: String?): TacticalPattern? {
        if (value.isNullOrBlank()) return TacticalPattern.MIXED
        return try {
            TacticalPattern.valueOf(value)
        } catch (e: Exception) {
            TacticalPattern.MIXED
        }
    }

    // ============================================================
    // TACTICAL THEME (НОВОЕ)
    // ============================================================

    @TypeConverter
    fun fromTacticalTheme(value: TacticalTheme?): String? = value?.name

    @TypeConverter
    fun toTacticalTheme(value: String?): TacticalTheme? {
        if (value.isNullOrBlank()) return TacticalTheme.TACTIC
        return try {
            TacticalTheme.valueOf(value)
        } catch (e: Exception) {
            TacticalTheme.TACTIC
        }
    }

    // ============================================================
    // EXERCISE SOURCE (НОВОЕ)
    // ============================================================

    @TypeConverter
    fun fromExerciseSource(value: ExerciseSource?): String? = value?.name

    @TypeConverter
    fun toExerciseSource(value: String?): ExerciseSource? {
        if (value.isNullOrBlank()) return ExerciseSource.MISTAKE
        return try {
            ExerciseSource.valueOf(value)
        } catch (e: Exception) {
            ExerciseSource.MISTAKE
        }
    }
}