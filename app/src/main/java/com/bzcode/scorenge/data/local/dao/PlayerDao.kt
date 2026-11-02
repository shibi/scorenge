package com.bzcode.scorenge.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Upsert
    suspend fun upsert(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: String): PlayerEntity?

    /** Players not soft-deleted, alphabetical. */
    @Query("SELECT * FROM players WHERE userId = :userId AND deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(userId: String): Flow<List<PlayerEntity>>
}
