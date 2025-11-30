// GameRepository.kt
package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.AnalysisStatus
import com.example.chessmentor.domain.entity.Game

interface GameRepository {
    suspend fun findById(id: Long): Game?
    suspend fun findByUserId(userId: Long): List<Game>
    suspend fun findByUserIdAndStatus(userId: Long, status: AnalysisStatus): List<Game>
    suspend fun save(game: Game): Game
    suspend fun update(game: Game): Game
    suspend fun deleteById(id: Long)
}


