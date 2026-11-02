package com.bzcode.scorenge.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.match.TeamSetup
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.domain.model.ScoringSubjectType
import com.bzcode.scorenge.seedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchRepositoryTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var repository: MatchRepository
    private lateinit var gameId: String
    private lateinit var maya: String
    private lateinit var leo: String
    private lateinit var ana: String

    @Before
    fun setUp() = runBlocking {
        // Regular executors: startMatch uses withTransaction, which needs a real transaction thread.
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScorengeDatabase::class.java)
            .build()
            .also { it.seedUser() }
        val clock = Clock { 5_000L }
        repository = MatchRepository(db, clock)
        val games = GameRepository(db.gameDao(), clock)
        gameId = (
            games.save(
                TEST_USER,
                null,
                GameDraft("Golf", "⛳", null, ScoringDirection.LOWER_WINS, ScoringMode.INDIVIDUAL),
            ) as SaveResult.Saved
            ).id
        val players = PlayerRepository(db.playerDao(), clock)
        maya = (players.save(TEST_USER, null, "Maya") as SaveResult.Saved).id
        leo = (players.save(TEST_USER, null, "Leo") as SaveResult.Saved).id
        ana = (players.save(TEST_USER, null, "Ana") as SaveResult.Saved).id
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun startsAnIndividualMatchWithPendingFirstRound() = runBlocking {
        val setup = MatchSetup(gameId, ScoringMode.INDIVIDUAL, RoundMode.FIXED, 3, listOf(leo, maya), emptyList())

        val id = repository.startMatch(TEST_USER, setup, "  ")

        val event = db.eventDao().getById(id)!!
        assertEquals("Golf", event.title)
        assertEquals(EventStatus.IN_PROGRESS, event.status)
        assertEquals(ScoringDirection.LOWER_WINS, event.scoringDirection)
        assertEquals(3, event.totalRounds)
        assertEquals(5_000L, event.startedAt)
        assertEquals(listOf("Leo", "Maya"), db.eventDao().getParticipants(id).map { it.playerName })
        val subjects = db.eventDao().observeSubjects(id).first()
        assertEquals(listOf("Leo", "Maya"), subjects.map { it.name })
        assertTrue(subjects.all { it.type == ScoringSubjectType.PLAYER })
        val round = db.roundDao().observeForEvent(id).first().single()
        assertEquals(1, round.roundNumber)
        assertEquals(RoundStatus.PENDING, round.status)
    }

    @Test
    fun startsATeamMatchWithTeamSubjectsAndUnlimitedRounds() = runBlocking {
        val setup = MatchSetup(
            gameId,
            ScoringMode.TEAM,
            RoundMode.DYNAMIC,
            5,
            emptyList(),
            listOf(TeamSetup(" Reds ", listOf(maya, leo)), TeamSetup("Blues", listOf(ana))),
        )

        val id = repository.startMatch(TEST_USER, setup, "Sunday golf")

        val event = db.eventDao().getById(id)!!
        assertEquals("Sunday golf", event.title)
        assertNull(event.totalRounds)
        assertEquals(listOf("Reds", "Blues"), db.eventDao().getTeams(id).map { it.name })
        assertEquals(3, db.eventDao().getParticipants(id).size)
        val subjects = db.eventDao().observeSubjects(id).first()
        assertEquals(listOf("Reds", "Blues"), subjects.map { it.name })
        assertTrue(subjects.all { it.type == ScoringSubjectType.TEAM })
    }

    @Test
    fun rejectsAnInvalidSetupAndSavesNothing() = runBlocking {
        val setup = MatchSetup(gameId, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 5, emptyList(), emptyList())
        try {
            repository.startMatch(TEST_USER, setup, "")
            fail("expected an error")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
        assertNull(repository.observeLatestInProgress(TEST_USER).first())
    }

    @Test
    fun latestInProgressDescribesTheMatchToContinue() = runBlocking {
        val setup = MatchSetup(gameId, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 5, listOf(maya, ana), emptyList())
        val id = repository.startMatch(TEST_USER, setup, "")

        val match = repository.observeLatestInProgress(TEST_USER).first()!!
        assertEquals(id, match.eventId)
        assertEquals("Golf", match.title)
        assertEquals("⛳", match.game?.icon)
        assertEquals(1, match.roundNumber)
        assertEquals(listOf("Maya", "Ana"), match.subjectNames)
    }
}
