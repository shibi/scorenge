package com.bzcode.scorenge.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.domain.match.FinishBlocker
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.MatchScoring
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoreEntry
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.seedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoundRepositoryTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var rounds: RoundRepository
    private lateinit var matches: MatchRepository
    private lateinit var gameId: String
    private lateinit var maya: String
    private lateinit var leo: String
    private var now = 1_000L
    private val clock = Clock { now }

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScorengeDatabase::class.java)
            .build()
            .also { it.seedUser() }
        rounds = RoundRepository(db, clock)
        matches = MatchRepository(db, clock)
        gameId = (
            GameRepository(db.gameDao(), clock).save(
                TEST_USER,
                null,
                GameDraft("Golf", null, null, ScoringDirection.LOWER_WINS, ScoringMode.INDIVIDUAL),
            ) as SaveResult.Saved
            ).id
        val players = PlayerRepository(db.playerDao(), clock)
        maya = (players.save(TEST_USER, null, "Maya") as SaveResult.Saved).id
        leo = (players.save(TEST_USER, null, "Leo") as SaveResult.Saved).id
    }

    @After
    fun tearDown() = db.close()

    private fun start(roundMode: RoundMode = RoundMode.DYNAMIC, total: Int = 5) = runBlocking {
        matches.startMatch(TEST_USER, MatchSetup(gameId, ScoringMode.INDIVIDUAL, roundMode, total, listOf(maya, leo), emptyList()), "")
    }

    private fun detail(eventId: String) = runBlocking { rounds.observeDetail(eventId).first()!! }

    private fun round(eventId: String, number: Int) = detail(eventId).rounds.single { it.roundNumber == number }

    private fun scoreAll(eventId: String, number: Int, vararg scores: Long) = runBlocking {
        val d = detail(eventId)
        val roundId = d.rounds.single { it.roundNumber == number }.id
        d.subjects.zip(scores.toList()).forEach { (subject, score) -> assertTrue(rounds.setScore(roundId, subject.id, score)) }
        roundId
    }

    @Test
    fun finishingOrderTurnsPlacesIntoPoints() = runBlocking {
        val carol = (PlayerRepository(db.playerDao(), clock).save(TEST_USER, null, "Carol") as SaveResult.Saved).id
        val id = matches.startMatch(
            TEST_USER,
            MatchSetup(gameId, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 0, listOf(maya, leo, carol), emptyList(), MatchScoring.FINISHING_ORDER),
            "",
        )
        // Golf is "lowest wins", but finishing order always ranks by points, highest first.
        assertEquals(ScoringDirection.HIGHER_WINS, detail(id).event.scoringDirection)
        assertEquals(ScoreEntry.FINISHING_ORDER, detail(id).event.scoreEntry)
        val roundId = round(id, 1).id
        val (m, l, c) = detail(id).subjects.map { it.id }
        assertFalse("pending rounds take no places", rounds.setFinishingOrder(roundId, listOf(l)))

        rounds.openRound(id, 1)
        assertTrue(rounds.setFinishingOrder(roundId, listOf(l, m)))
        assertEquals(mapOf(l to 3L, m to 2L, c to null), detail(id).scoresOf(roundId))
        assertFalse(rounds.completeRound(roundId))

        // Taking 1st off moves Maya up; then Carol and Leo finish.
        rounds.setFinishingOrder(roundId, listOf(m, c, l))
        assertEquals(mapOf(m to 3L, c to 2L, l to 1L), detail(id).scoresOf(roundId))
        assertTrue(rounds.completeRound(roundId))
        assertNull(rounds.finishMatch(id))
        assertEquals(listOf(1, 2, 3), detail(id).subjects.sortedBy { it.finalRank }.map { it.finalRank })
        assertEquals(m, detail(id).subjects.single { it.finalRank == 1 }.id)
    }

    @Test
    fun clockRunsOnlyWhileTheRoundPageIsOpen() = runBlocking {
        val id = start()
        rounds.openRound(id, 1)
        assertEquals(RoundStatus.ACTIVE, round(id, 1).status)
        assertEquals(1_000L, round(id, 1).startedAt)

        now = 61_000L
        rounds.closeRound(id, 1)
        assertEquals(60_000L, round(id, 1).elapsedMillis)
        assertNull(round(id, 1).openedAt)

        now = 500_000L // page closed: this time doesn't count
        rounds.openRound(id, 1)
        now = 530_000L
        rounds.closeRound(id, 1)
        assertEquals(90_000L, round(id, 1).elapsedMillis)
        assertEquals(1_000L, round(id, 1).startedAt)
    }

    @Test
    fun scoresNeedAnOpenRoundAndCompletingNeedsEveryScore() = runBlocking {
        val id = start()
        val roundId = round(id, 1).id
        val subject = detail(id).subjects.first().id
        assertFalse("pending rounds take no scores", rounds.setScore(roundId, subject, 3))

        rounds.openRound(id, 1)
        assertTrue(rounds.setScore(roundId, subject, 3))
        assertTrue(rounds.setScore(roundId, subject, 4)) // overwrite keeps one row
        assertEquals(listOf(4L), detail(id).scores.map { it.score })
        assertFalse(rounds.completeRound(roundId))

        scoreAll(id, 1, 4, 5)
        now = 10_000L
        assertTrue(rounds.completeRound(roundId))
        val done = round(id, 1)
        assertEquals(RoundStatus.COMPLETED, done.status)
        assertEquals(9_000L, done.elapsedMillis)
        assertEquals(RoundStatus.PENDING, round(id, 2).status)
        assertFalse("completed rounds are locked", rounds.setScore(roundId, subject, 1))
    }

    @Test
    fun noRoundAfterTheLastFixedRound() = runBlocking {
        val id = start(RoundMode.FIXED, total = 1)
        rounds.openRound(id, 1)
        assertTrue(rounds.completeRound(scoreAll(id, 1, 2, 3)))
        assertEquals(listOf(1), detail(id).rounds.map { it.roundNumber })
    }

    @Test
    fun finishSavesFinalRanksAndDropsTheUnplayedRound() = runBlocking {
        val id = start()
        assertEquals(FinishBlocker.NO_COMPLETED_ROUND, rounds.finishMatch(id))

        rounds.openRound(id, 1)
        rounds.completeRound(scoreAll(id, 1, 7, 4)) // lower wins: Leo leads
        rounds.openRound(id, 2)
        val roundTwo = round(id, 2).id
        rounds.setScore(roundTwo, detail(id).subjects.first().id, 1)
        assertEquals(FinishBlocker.ROUND_IN_PROGRESS, rounds.finishMatch(id))
        rounds.setScore(roundTwo, detail(id).subjects.first().id, null)

        assertNull(rounds.finishMatch(id))

        val d = detail(id)
        assertEquals(EventStatus.COMPLETED, d.event.status)
        assertEquals(listOf(1), d.rounds.map { it.roundNumber })
        val bySubject = d.subjects.associate { it.name to (it.finalRank to it.finalTotal) }
        assertEquals(2 to 7L, bySubject["Maya"])
        assertEquals(1 to 4L, bySubject["Leo"])
        assertNull(matches.observeLatestInProgress(TEST_USER).first())

        rounds.openRound(id, 1) // finished matches stay locked
        assertEquals(RoundStatus.COMPLETED, round(id, 1).status)
    }
}
