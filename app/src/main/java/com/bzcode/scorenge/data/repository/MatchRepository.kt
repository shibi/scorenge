package com.bzcode.scorenge.data.repository

import androidx.room.withTransaction
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.TeamEntity
import com.bzcode.scorenge.data.local.entity.TeamMemberEntity
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.match.MatchSetupRules
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.domain.model.ScoringSubjectType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** What Home's "Continue match" card shows. */
data class InProgressMatch(
    val eventId: String,
    val title: String,
    val game: GameEntity?,
    /** Latest round's number; 0 before any round exists. */
    val roundNumber: Int,
    val subjectNames: List<String>,
)

class MatchRepository @Inject constructor(
    private val db: ScorengeDatabase,
    private val clock: Clock,
) {
    private val eventDao get() = db.eventDao()
    private val roundDao get() = db.roundDao()

    /**
     * Saves a new match and starts it: the match (in progress), its participants, teams and scoring
     * subjects, and round 1 as PENDING (it becomes active when the round page opens). All or nothing.
     *
     * @return the new match id
     * @throws IllegalArgumentException if [setup] breaks a setup rule or names an unknown game or player
     */
    suspend fun startMatch(userId: String, setup: MatchSetup, title: String): String {
        val problems = MatchSetupRules.problems(setup)
        require(problems.isEmpty()) { "Invalid setup: $problems" }
        val game = requireNotNull(db.gameDao().getById(setup.gameId!!)) { "Unknown game ${setup.gameId}" }
        val now = clock.nowMillis()
        val eventId = newId()

        db.withTransaction {
            eventDao.upsert(
                EventEntity(
                    id = eventId,
                    userId = userId,
                    gameId = game.id,
                    title = title.trim().ifEmpty { game.title },
                    scoringMode = setup.scoringMode,
                    scoringDirection = game.scoringDirection,
                    roundMode = setup.roundMode,
                    totalRounds = if (setup.roundMode == RoundMode.FIXED) setup.totalRounds else null,
                    status = EventStatus.IN_PROGRESS,
                    startedAt = now,
                    lastPlayedAt = now,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            val participants = setup.participantIds.mapIndexed { index, playerId ->
                val player = requireNotNull(db.playerDao().getById(playerId)) { "Unknown player $playerId" }
                EventParticipantEntity(newId(), eventId, playerId, player.name, index, now, now)
            }
            eventDao.insertParticipants(participants)
            val participantByPlayer = participants.associateBy { it.playerId }

            val subjects = when (setup.scoringMode) {
                ScoringMode.INDIVIDUAL -> participants.map { p ->
                    ScoringSubjectEntity(
                        id = newId(),
                        eventId = eventId,
                        type = ScoringSubjectType.PLAYER,
                        participantId = p.id,
                        teamId = null,
                        name = p.playerName,
                        sortOrder = p.sortOrder,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
                ScoringMode.TEAM -> {
                    val teams = setup.teams.mapIndexed { index, team ->
                        TeamEntity(newId(), eventId, team.name.trim(), index, now, now)
                    }
                    eventDao.insertTeams(teams)
                    eventDao.insertTeamMembers(
                        setup.teams.zip(teams).flatMap { (draft, team) ->
                            draft.playerIds.distinct().map { playerId ->
                                TeamMemberEntity(newId(), team.id, participantByPlayer.getValue(playerId).id, now, now)
                            }
                        },
                    )
                    teams.map { team ->
                        ScoringSubjectEntity(
                            id = newId(),
                            eventId = eventId,
                            type = ScoringSubjectType.TEAM,
                            participantId = null,
                            teamId = team.id,
                            name = team.name,
                            sortOrder = team.sortOrder,
                            createdAt = now,
                            updatedAt = now,
                        )
                    }
                }
            }
            eventDao.insertSubjects(subjects)

            roundDao.insert(
                RoundEntity(
                    id = newId(),
                    eventId = eventId,
                    roundNumber = 1,
                    status = RoundStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return eventId
    }

    /** The most recently played match in progress, or null. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLatestInProgress(userId: String): Flow<InProgressMatch?> =
        eventDao.observeInProgress(userId)
            .map { it.firstOrNull() }
            .flatMapLatest { event ->
                if (event == null) {
                    flowOf(null)
                } else {
                    combine(
                        db.gameDao().observe(event.gameId),
                        eventDao.observeSubjects(event.id),
                        roundDao.observeForEvent(event.id),
                    ) { game, subjects, rounds ->
                        InProgressMatch(
                            eventId = event.id,
                            title = event.title,
                            game = game,
                            roundNumber = rounds.maxOfOrNull { it.roundNumber } ?: 0,
                            subjectNames = subjects.map { it.name },
                        )
                    }
                }
            }

    fun observeMatch(eventId: String): Flow<EventEntity?> = eventDao.observe(eventId)

    private fun newId() = UUID.randomUUID().toString()
}
