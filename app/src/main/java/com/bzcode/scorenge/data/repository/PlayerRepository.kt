package com.bzcode.scorenge.data.repository

import com.bzcode.scorenge.data.local.dao.PlayerDao
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * The scorekeeper's players. Removing a player is a soft delete: past matches keep the name, and
 * the player disappears from lists and pickers.
 */
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val clock: Clock,
) {
    fun observeActive(userId: String): Flow<List<PlayerEntity>> = playerDao.observeActive(userId)

    /** Adds a player when [playerId] is null, otherwise renames it. */
    suspend fun save(userId: String, playerId: String?, rawName: String): SaveResult {
        val name = normalizeName(rawName)
        checkNameShape(name)?.let { return SaveResult.Invalid(it) }
        val clash = playerDao.findActiveByName(userId, name)
        if (clash != null && clash.id != playerId) return SaveResult.Invalid(NameError.DUPLICATE)

        val now = clock.nowMillis()
        val existing = playerId?.let { playerDao.getById(it) }
        val player = existing?.copy(name = name, updatedAt = now)
            ?: PlayerEntity(id = UUID.randomUUID().toString(), userId = userId, name = name, createdAt = now, updatedAt = now)
        playerDao.upsert(player)
        return SaveResult.Saved(player.id)
    }

    suspend fun remove(playerId: String) {
        val player = playerDao.getById(playerId) ?: return
        val now = clock.nowMillis()
        playerDao.upsert(player.copy(deletedAt = now, updatedAt = now))
    }
}
