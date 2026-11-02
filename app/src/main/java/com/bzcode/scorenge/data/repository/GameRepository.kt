package com.bzcode.scorenge.data.repository

import com.bzcode.scorenge.data.local.dao.GameDao
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** What the user fills in on the game form. */
data class GameDraft(
    val title: String,
    val icon: String?,
    val description: String?,
    val scoringDirection: ScoringDirection,
    val defaultScoringMode: ScoringMode,
)

/** Game types (Carrom, Chess...). Deleting is a soft delete; finished matches keep pointing at the game. */
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val clock: Clock,
) {
    fun observeActive(userId: String): Flow<List<GameEntity>> = gameDao.observeActive(userId)

    fun observe(gameId: String): Flow<GameEntity?> = gameDao.observe(gameId)

    suspend fun getById(gameId: String): GameEntity? = gameDao.getById(gameId)

    /** Creates a game when [gameId] is null, otherwise updates it. */
    suspend fun save(userId: String, gameId: String?, draft: GameDraft): SaveResult {
        val title = normalizeName(draft.title)
        checkNameShape(title)?.let { return SaveResult.Invalid(it) }
        val clash = gameDao.findActiveByTitle(userId, title)
        if (clash != null && clash.id != gameId) return SaveResult.Invalid(NameError.DUPLICATE)

        val now = clock.nowMillis()
        val description = draft.description?.trim()?.takeIf { it.isNotEmpty() }
        val icon = draft.icon?.takeIf { it.isNotBlank() }
        val existing = gameId?.let { gameDao.getById(it) }
        val game = existing?.copy(
            title = title,
            icon = icon,
            description = description,
            scoringDirection = draft.scoringDirection,
            defaultScoringMode = draft.defaultScoringMode,
            updatedAt = now,
        ) ?: GameEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            description = description,
            icon = icon,
            scoringDirection = draft.scoringDirection,
            defaultScoringMode = draft.defaultScoringMode,
            createdAt = now,
            updatedAt = now,
        )
        gameDao.upsert(game)
        return SaveResult.Saved(game.id)
    }

    suspend fun delete(gameId: String) {
        val game = gameDao.getById(gameId) ?: return
        val now = clock.nowMillis()
        gameDao.upsert(game.copy(deletedAt = now, updatedAt = now))
    }
}
