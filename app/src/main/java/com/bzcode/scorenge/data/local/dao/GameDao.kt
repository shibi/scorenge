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
}
