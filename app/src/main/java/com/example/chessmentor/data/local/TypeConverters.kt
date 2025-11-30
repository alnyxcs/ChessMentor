package com.example.chessmentor.data.local

import androidx.room.TypeConverter
import com.example.chessmentor.domain.entity.AnalysisStatus
import com.example.chessmentor.domain.entity.ChessColor
import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.entity.SkillLevel

class TypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }

    // ... остальные конвертеры для enum (SkillLevel и т.д.)
    // Оставьте их как есть

    @TypeConverter
    fun fromSkillLevel(value: SkillLevel?): String? = value?.name
    @TypeConverter
    fun toSkillLevel(value: String?): SkillLevel? = value?.let { SkillLevel.valueOf(it) }
    @TypeConverter
    fun fromChessColor(value: ChessColor?): String? = value?.name
    @TypeConverter
    fun toChessColor(value: String?): ChessColor? = value?.let { ChessColor.valueOf(it) }
    @TypeConverter
    fun fromAnalysisStatus(value: AnalysisStatus?): String? = value?.name
    @TypeConverter
    fun toAnalysisStatus(value: String?): AnalysisStatus? = value?.let { AnalysisStatus.valueOf(it) }
    @TypeConverter
    fun fromMistakeType(value: MistakeType?): String? = value?.name
    @TypeConverter
    fun toMistakeType(value: String?): MistakeType? = value?.let { MistakeType.valueOf(it) }
}