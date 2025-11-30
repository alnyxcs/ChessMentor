package com.example.chessmentor.domain.repository

import com.example.chessmentor.domain.entity.SkillLevel
import com.example.chessmentor.domain.entity.User

interface UserRepository {
    suspend fun findById(id: Long): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByNickname(nickname: String): User?
    suspend fun save(user: User): User
    suspend fun update(user: User): User
    suspend fun deleteById(id: Long)
    suspend fun existsByEmail(email: String): Boolean
    suspend fun existsByNickname(nickname: String): Boolean
    suspend fun findAll(): List<User>
    suspend fun findBySkillLevel(skillLevel: SkillLevel): List<User>
    suspend fun findTopByRating(limit: Int): List<User>
}