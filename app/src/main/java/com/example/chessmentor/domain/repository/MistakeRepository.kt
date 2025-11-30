package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MistakeType

interface MistakeRepository {
    suspend fun findById(id: Long): Mistake?
    suspend fun findByGameId(gameId: Long): List<Mistake>
    suspend fun findByUserId(userId: Long): List<Mistake> // <-- Убедитесь, что этот метод есть
    suspend fun save(mistake: Mistake): Mistake
    suspend fun saveAll(mistakes: List<Mistake>): List<Mistake>
    suspend fun update(mistake: Mistake): Mistake
    suspend fun deleteById(id: Long)
    suspend fun deleteByGameId(gameId: Long)

    // Дополнительные методы
    suspend fun findByThemeId(themeId: Long): List<Mistake>
    suspend fun findByGameIdAndType(gameId: Long, type: MistakeType): List<Mistake>
    suspend fun findByUserIdAndTheme(userId: Long, themeId: Long): List<Mistake>
    suspend fun countByGameId(gameId: Long): Int
    suspend fun countByUserIdAndType(userId: Long, type: MistakeType): Int
    suspend fun getMostFrequentThemesByUserId(userId: Long, limit: Int): Map<Long, Int>
    suspend fun getAverageEvaluationLossByUserId(userId: Long): Double?

}