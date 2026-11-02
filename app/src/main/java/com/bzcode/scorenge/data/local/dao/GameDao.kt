package com.bzcode.scorenge.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bzcode.scorenge.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Upsert
    suspend fun upsert(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: String): GameEntity?

    /** Games not soft-deleted, alphabetical. */
    @Query("SELECT * FROM games WHERE userId = :userId AND deletedAt IS NULL ORDER BY title COLLATE NOCASE")
    fun observeActive(userId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observe(id: String): Flow<GameEntity?>

    /** An active game with this title, ignoring case; used to block duplicates. */
    @Query("SELECT * FROM games WHERE userId = :userId AND deletedAt IS NULL AND title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findActiveByTitle(userId: String, title: String): GameEntity?
}
