package com.bzcode.scorenge.data.repository

import androidx.room.withTransaction
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.domain.match.FinishBlocker
import com.bzcode.scorenge.domain.match.RoundRules
import com.bzcode.scorenge.domain.match.RoundState
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.RankBasis
import com.bzcode.scorenge.domain.scoring.RoundScores
import com.bzcode.scorenge.domain.scoring.RoundTimer
import com.bzcode.scorenge.domain.scoring.ScoringEngine
import com.bzcode.scorenge.domain.scoring.SubjectRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject

/** Everything the match and round pages show. */
data class MatchDetail(
    val event: EventEntity,
    val game: GameEntity?,
    val subjects: List<ScoringSubjectEntity>,
    val rounds: List<RoundEntity>,
    val scores: List<RoundScoreEntity>,
) {
    val subjectRefs: List<SubjectRef> get() = subjects.map { SubjectRef(it.id, it.name) }

    fun scoresOf(roundId: String): Map<String, Long?> =
        scores.filter { it.roundId == roundId }.associate { it.subjectId to it.score }

    val roundScores: List<RoundScores>
        get() = rounds.map { RoundScores(it.roundNumber, it.status, scoresOf(it.id)) }

    val roundStates: List<RoundState>
        get() = rounds.map { round -> RoundState(round.id, round.status, scoresOf(round.id).values.count { it != null }) }
}

/** Round lifecycle: open (starts the clock), close (pauses it), score, complete, and finish the match. */
class RoundRepository @Inject constructor(
    private val db: ScorengeDatabase,
    private val clock: Clock,
) {
    private val eventDao get() = db.eventDao()
    private val roundDao get() = db.roundDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDetail(eventId: String): Flow<MatchDetail?> =
        eventDao.observe(eventId)
            .distinctUntilChangedBy { it?.gameId }
            .flatMapLatest { first ->
                if (first == null) {
                    flowOf(null)
                } else {
                    combine(
                        eventDao.observe(eventId),
                        db.gameDao().observe(first.gameId),
                        eventDao.observeSubjects(eventId),
                        roundDao.observeForEvent(eventId),
                        roundDao.observeScoresForEvent(eventId),
                    ) { event, game, subjects, rounds, scores ->
                        event?.let { MatchDetail(it, game, subjects, rounds, scores) }
                    }
                }
            }

    /**
     * Opens round [roundNumber]'s page: a pending round becomes active and its clock starts; an
     * active round's clock resumes. Completed rounds are unchanged.
     */
    suspend fun openRound(eventId: String, roundNumber: Int) = db.withTransaction {
        val event = eventDao.getById(eventId) ?: return@withTransaction
        if (event.status != EventStatus.IN_PROGRESS) return@withTransaction
        val round = roundDao.getByNumber(eventId, roundNumber) ?: return@withTransaction
        val now = clock.nowMillis()
        val opened = when (round.status) {
            RoundStatus.COMPLETED -> return@withTransaction
            RoundStatus.PENDING -> round.copy(status = RoundStatus.ACTIVE, startedAt = now, openedAt = now, updatedAt = now)
            RoundStatus.ACTIVE -> if (round.openedAt != null) return@withTransaction else round.copy(openedAt = now, updatedAt = now)
        }
        roundDao.update(opened)
        eventDao.upsert(event.copy(lastPlayedAt = now, updatedAt = now))
    }

    /** Closes the round page: the clock pauses and the elapsed time so far is saved. */
    suspend fun closeRound(eventId: String, roundNumber: Int) {
        val round = roundDao.getByNumber(eventId, roundNumber) ?: return
        if (round.status != RoundStatus.ACTIVE || round.openedAt == null) return
        val now = clock.nowMillis()
        val timer = round.timer().close(now)
        roundDao.update(round.copy(elapsedMillis = timer.accumulatedMillis, openedAt = null, updatedAt = now))
    }

    /** Saves a score in an active round. Null clears it ("not entered", which is not 0). */
    suspend fun setScore(roundId: String, subjectId: String, score: Long?): Boolean = db.withTransaction {
        val round = roundDao.getById(roundId)
        if (round == null || round.status != RoundStatus.ACTIVE) return@withTransaction false
        val now = clock.nowMillis()
        val existing = roundDao.getScores(roundId).firstOrNull { it.subjectId == subjectId }
        roundDao.upsertScore(
            existing?.copy(score = score, updatedAt = now)
                ?: RoundScoreEntity(newId(), roundId, subjectId, score, now, now),
        )
        eventDao.getById(round.eventId)?.let { eventDao.upsert(it.copy(lastPlayedAt = now, updatedAt = now)) }
        true
    }

    /**
     * Locks an active round once every player or team has a score, and creates the next round as
     * PENDING unless all fixed rounds are played.
     *
     * @return false when the round isn't active or a score is missing
     */
    suspend fun completeRound(roundId: String): Boolean = db.withTransaction {
        val round = roundDao.getById(roundId)
        if (round == null || round.status != RoundStatus.ACTIVE) return@withTransaction false
        val event = eventDao.getById(round.eventId) ?: return@withTransaction false
        val subjects = eventDao.getSubjects(event.id).map { SubjectRef(it.id, it.name) }
        val scores = roundDao.getScores(roundId).associate { it.subjectId to it.score }
        val missing = ScoringEngine.missingScores(subjects, RoundScores(round.roundNumber, round.status, scores))
        if (missing.isNotEmpty()) return@withTransaction false

        val now = clock.nowMillis()
        roundDao.update(
            round.copy(
                status = RoundStatus.COMPLETED,
                completedAt = now,
                elapsedMillis = round.timer().close(now).accumulatedMillis,
                openedAt = null,
                updatedAt = now,
            ),
        )
        RoundRules.nextRoundNumber(round.roundNumber, event.roundMode, event.totalRounds)?.let { next ->
            roundDao.insert(RoundEntity(newId(), event.id, next, RoundStatus.PENDING, createdAt = now, updatedAt = now))
        }
        eventDao.upsert(event.copy(lastPlayedAt = now, updatedAt = now))
        true
    }

    /**
     * Finishes the match: drops rounds that were never played, saves each player's or team's final
     * total and rank from completed rounds, and locks the match for good.
     *
     * @return null when finished, otherwise why it can't finish yet
     */
    suspend fun finishMatch(eventId: String): FinishBlocker? = db.withTransaction {
        val event = eventDao.getById(eventId) ?: return@withTransaction null
        if (event.status == EventStatus.COMPLETED) return@withTransaction null
        val rounds = roundDao.getForEvent(eventId)
        val scoresByRound = rounds.associate { round ->
            round.id to roundDao.getScores(round.id).associate { it.subjectId to it.score }
        }
        val states = rounds.map { r -> RoundState(r.id, r.status, scoresByRound.getValue(r.id).values.count { it != null }) }
        RoundRules.finishBlocker(states)?.let { return@withTransaction it }

        val discarded = RoundRules.roundsDiscardedOnFinish(states).map { it.id }
        if (discarded.isNotEmpty()) roundDao.deleteByIds(discarded)

        val subjects = eventDao.getSubjects(eventId)
        val board = ScoringEngine.leaderboard(
            subjects = subjects.map { SubjectRef(it.id, it.name) },
            rounds = rounds.filter { it.id !in discarded }
                .map { RoundScores(it.roundNumber, it.status, scoresByRound.getValue(it.id)) },
            direction = event.scoringDirection,
            basis = RankBasis.OFFICIAL,
        )
        val now = clock.nowMillis()
        val byId = board.entries.associateBy { it.subjectId }
        eventDao.updateSubjects(
            subjects.map { s ->
                s.copy(finalRank = byId[s.id]?.rank, finalTotal = byId[s.id]?.officialTotal, updatedAt = now)
            },
        )
        eventDao.upsert(event.copy(status = EventStatus.COMPLETED, completedAt = now, lastPlayedAt = now, updatedAt = now))
        null
    }

    private fun RoundEntity.timer() = RoundTimer(elapsedMillis, openedAt)

    private fun newId() = UUID.randomUUID().toString()
}
