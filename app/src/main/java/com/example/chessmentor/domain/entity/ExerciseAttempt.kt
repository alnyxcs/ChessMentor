package com.example.chessmentor.domain.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverters // Импорт аннотации
import com.example.chessmentor.data.local.TypeConverters as MyConverters // Алиас вашего класса

@Entity(
    tableName = "exercise_attempts",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"])]
)
data class ExerciseAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    val userId: Long,
    val exerciseId: Long,
    val solved: Boolean,
    val timeSpentSeconds: Int,

    @field:TypeConverters(MyConverters::class) // <--- ВОТ ТАК
    val movesMade: List<String>,

    val attemptedAt: Long = System.currentTimeMillis()
)