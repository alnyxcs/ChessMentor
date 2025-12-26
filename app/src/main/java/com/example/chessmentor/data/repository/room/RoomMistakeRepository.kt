package com.example.chessmentor.data.repository.room

import com.example.chessmentor.data.local.dao.MistakeDao
import com.example.chessmentor.domain.entity.Mistake
import com.example.chessmentor.domain.entity.MistakeType
import com.example.chessmentor.domain.repository.MistakeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomMistakeRepository(private val mistakeDao: MistakeDao) : MistakeRepository {
    override suspend fun findById(id: Long): Mistake? = withContext(Dispatchers.IO) {
        null // TODO: Реализовать при необходимости
    }

    override suspend fun findByGameId(gameId: Long): List<Mistake> = withContext(Dispatchers.IO) {
        mistakeDao.findByGameId(gameId)
    }

    override suspend fun findByUserId(userId: Long): List<Mistake> = withContext(Dispatchers.IO) {
        mistakeDao.findByUserId(userId)
    }
    
    // ✅ НОВОЕ: Поиск по нескольким играм
    override suspend fun findByGameIds(gameIds: List<Long>): List<Mistake> = withContext(Dispatchers.IO) {
        if (gameIds.isEmpty()) return@withContext emptyList()
        mistakeDao.findByGameIds(gameIds)
    }

    override suspend fun save(mistake: Mistake): Mistake = withContext(Dispatchers.IO) {
        mistakeDao.insertAll(listOf(mistake))
        mistake
    }

    override suspend fun saveAll(mistakes: List<Mistake>): List<Mistake> = withContext(Dispatchers.IO) {
        mistakeDao.insertAll(mistakes)
        mistakes
    }

    override suspend fun update(mistake: Mistake): Mistake = withContext(Dispatchers.IO) {
        mistake // TODO: Реализовать при необходимости
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        // TODO: Реализовать при необходимости
    }

    override suspend fun deleteByGameId(gameId: Long) = withContext(Dispatchers.IO) {
        mistakeDao.deleteByGameId(gameId)
    }

    override suspend fun findByThemeId(themeId: Long): List<Mistake> = withContext(Dispatchers.IO) {
        emptyList() // TODO: Реализовать при необходимости
    }

    override suspend fun findByGameIdAndType(gameId: Long, type: MistakeType): List<Mistake> = withContext(Dispatchers.IO) {
        emptyList() // TODO: Реализовать при необходимости
    }

    override suspend fun findByUserIdAndTheme(userId: Long, themeId: Long): List<Mistake> = withContext(Dispatchers.IO) {
        emptyList() // TODO: Реализовать при необходимости
    }

    override suspend fun countByGameId(gameId: Long): Int = withContext(Dispatchers.IO) {
        findByGameId(gameId).size
    }

    override suspend fun countByUserIdAndType(userId: Long, type: MistakeType): Int = withContext(Dispatchers.IO) {
        0 // TODO: Реализовать при необходимости
    }

    override suspend fun getMostFrequentThemesByUserId(userId: Long, limit: Int): Map<Long, Int> = withContext(Dispatchers.IO) {
        emptyMap() // TODO: Реализовать при необходимости
    }

    override suspend fun getAverageEvaluationLossByUserId(userId: Long): Double? = withContext(Dispatchers.IO) {
        null // TODO: Реализовать при необходимости
    }
}