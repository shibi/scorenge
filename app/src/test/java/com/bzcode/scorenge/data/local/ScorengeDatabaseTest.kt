package com.bzcode.scorenge.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.UserEntity
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.domain.model.ScoringSubjectType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScorengeDatabaseTest {
    private lateinit var db: ScorengeDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScorengeDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun inProgressAndRecentMatchesAreScopedAndOrdered() = runTest {
        seedUserGame()
        db.eventDao().upsert(event("e1", EventStatus.IN_PROGRESS, lastPlayedAt = 100))
        db.eventDao().upsert(event("e2", EventStatus.IN_PROGRESS, lastPlayedAt = 300))
        db.eventDao().upsert(event("e3", EventStatus.COMPLETED, completedAt = 200))
        db.eventDao().upsert(event("e4", EventStatus.IN_PROGRESS, lastPlayedAt = 400, deletedAt = 500))

        val inProgress = db.eventDao().observeInProgress(USER).first()
        assertEquals(listOf("e2", "e1"), inProgress.map { it.id })

        val recent = db.eventDao().observeRecentCompleted(USER, limit = 5).first()
        assertEquals(listOf("e3"), recent.map { it.id })

        assertTrue(db.eventDao().observeInProgress("someone-else").first().isEmpty())
    }

    @Test
    fun participantMustReferenceAnExistingPlayer() = runTest {
        seedUserGame()
        db.eventDao().upsert(event("e1", EventStatus.NOT_STARTED))

        val result = runCatching {
            db.eventDao().insertParticipants(listOf(participant("p1", "e1", playerId = "missing")))
        }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun roundNumberIsUniqueWithinAMatch() = runTest {
        seedUserGame()
        db.eventDao().upsert(event("e1", EventStatus.IN_PROGRESS))
        db.roundDao().insert(round("r1", "e1", 1))

        val result = runCatching { db.roundDao().insert(round("r1b", "e1", 1)) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun notEnteredScoreStaysNullUntilSavedAsZero() = runTest {
        val subjectId = seedMatchWithOneSubject()
        db.roundDao().insert(round("r1", "e1", 1))

        db.roundDao().upsertScore(score("s1", "r1", subjectId, null))
        assertNull(db.roundDao().observeScores("r1").first().single().score)

        db.roundDao().upsertScore(score("s1", "r1", subjectId, 0))
        assertEquals(0L, db.roundDao().observeScores("r1").first().single().score)
    }

    @Test
    fun deletingAMatchCascadesToItsRowsButKeepsPlayers() = runTest {
        val subjectId = seedMatchWithOneSubject()
        db.roundDao().insert(round("r1", "e1", 1))
        db.roundDao().upsertScore(score("s1", "r1", subjectId, 7))

        db.eventDao().deleteById("e1")

        assertNull(db.eventDao().getById("e1"))
        assertNull(db.roundDao().getById("r1"))
        assertTrue(db.roundDao().observeScoresForEvent("e1").first().isEmpty())
        assertTrue(db.eventDao().getParticipants("e1").isEmpty())
        assertEquals("Maya", db.playerDao().getById("pl1")?.name)
    }

    private suspend fun seedUserGame() {
        db.userDao().upsert(UserEntity(USER, "Sam", null, null, createdAt = 0, updatedAt = 0))
        db.gameDao().upsert(
            GameEntity(
                id = GAME,
                userId = USER,
                title = "Carrom",
                scoringDirection = ScoringDirection.HIGHER_WINS,
                defaultScoringMode = ScoringMode.INDIVIDUAL,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    private suspend fun seedMatchWithOneSubject(): String {
        seedUserGame()
        db.playerDao().upsert(PlayerEntity("pl1", USER, "Maya", createdAt = 0, updatedAt = 0))
        db.eventDao().upsert(event("e1", EventStatus.IN_PROGRESS))
        db.eventDao().insertParticipants(listOf(participant("p1", "e1", "pl1")))
        db.eventDao().insertSubjects(
            listOf(
                ScoringSubjectEntity(
                    id = "sub1",
                    eventId = "e1",
                    type = ScoringSubjectType.PLAYER,
                    participantId = "p1",
                    teamId = null,
                    name = "Maya",
                    sortOrder = 0,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            ),
        )
        return "sub1"
    }

    private fun event(
        id: String,
        status: EventStatus,
        lastPlayedAt: Long? = null,
        completedAt: Long? = null,
        deletedAt: Long? = null,
    ) = EventEntity(
        id = id,
        userId = USER,
        gameId = GAME,
        title = "Friday night",
        scoringMode = ScoringMode.INDIVIDUAL,
        scoringDirection = ScoringDirection.HIGHER_WINS,
        roundMode = RoundMode.DYNAMIC,
        totalRounds = null,
        status = status,
        lastPlayedAt = lastPlayedAt,
        completedAt = completedAt,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = deletedAt,
    )

    private fun participant(id: String, eventId: String, playerId: String) =
        EventParticipantEntity(id, eventId, playerId, "Maya", sortOrder = 0, createdAt = 0, updatedAt = 0)

    private fun round(id: String, eventId: String, number: Int) =
        RoundEntity(id = id, eventId = eventId, roundNumber = number, status = RoundStatus.ACTIVE, createdAt = 0, updatedAt = 0)

    private fun score(id: String, roundId: String, subjectId: String, value: Long?) =
        RoundScoreEntity(id, roundId, subjectId, value, createdAt = 0, updatedAt = 0)

    private companion object {
        const val USER = "uid-1"
        const val GAME = "g1"
    }
}
