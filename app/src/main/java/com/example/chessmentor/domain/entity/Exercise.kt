package com.example.chessmentor.domain.entity

import androidx.room.Entity

import androidx.room.PrimaryKey

import androidx.room.TypeConverters // Импорт аннотации
import com.example.chessmentor.data.local.TypeConverters as MyConverters // Алиас вашего класса

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    val fenPosition: String,
    val prompt: String,

    @field:TypeConverters(MyConverters::class) // <--- ВОТ ЭТО
    val solutionMoves: List<String>,

    val rating: Int,
    val themeId: Long,
    val sourceGameId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)