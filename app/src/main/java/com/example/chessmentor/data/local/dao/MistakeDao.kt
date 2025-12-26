package com.example.chessmentor.data.local.dao

import androidx.room.*
import com.example.chessmentor.domain.entity.Mistake

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes WHERE gameId = :gameId ORDER BY moveNumber ASC")
    suspend fun findByGameId(gameId: Long): List<Mistake>
    
    @Query("SELECT * FROM mistakes WHERE gameId IN (:gameIds) ORDER BY gameId, moveNumber ASC")
    suspend fun findByGameIds(gameIds: List<Long>): List<Mistake>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mistakes: List<Mistake>)

    @Query("DELETE FROM mistakes WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("SELECT m.* FROM mistakes m INNER JOIN games g ON m.gameId = g.id WHERE g.userId = :userId")
    suspend fun findByUserId(userId: Long): List<Mistake>
}
