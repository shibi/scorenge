package com.bzcode.scorenge.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.match.TeamSetup
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.seedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsRepositoryTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var stats: StatsRepository
    private lateinit var matches: MatchRepository
    private lateinit var rounds: RoundRepository
    private lateinit var golf: String
    private lateinit var players: Map<String, String>
    private var now = 1_000L
    private val clock = Clock { now }

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScorengeDatabase::class.java)
            .build()
            .also { it.seedUser() }
        stats = StatsRepository(db)
        matches = MatchRepository(db, clock)
        rounds = RoundRepository(db, clock)
        golf = (
            GameRepository(db.gameDao(), clock).save(
                TEST_USER,
                null,
                GameDraft("Golf", "⛳", null, ScoringDirection.HIGHER_WINS, ScoringMode.INDIVIDUAL),
            ) as SaveResult.Saved
            ).id
        val repo = PlayerRepository(db.playerDao(), clock)
        players = listOf("Maya", "Leo", "Ana").associateWith { (repo.save(TEST_USER, null, it) as SaveResult.Saved).id }
    }

    @After
    fun tearDown() = db.close()

    /** Plays one round with [scores] in subject order, then finishes the match. */
    private suspend fun play(setup: MatchSetup, title: String, vararg scores: Long): String {
        val id = matches.startMatch(TEST_USER, setup, title)
        rounds.openRound(id, 1)
        val detail = rounds.observeDetail(id).first()!!
        val roundId = detail.rounds.single().id
        detail.subjects.zip(scores.toList()).forEach { (s, v) -> rounds.setScore(roundId, s.id, v) }
        rounds.completeRound(roundId)
        now += 1_000
        rounds.finishMatch(id)
        return id
    }

    @Test
    fun recentMatchesListWinnersNewestFirstAndSkipUnfinished() = runBlocking {
        val individual = MatchSetup(golf, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 5, listOf(players.getValue("Maya"), players.getValue("Leo")), emptyList())
        play(individual, "First", 5, 5)
        play(individual, "Second", 3, 9)
        matches.startMatch(TEST_USER, individual, "Still playing")

        val recent = stats.observeRecentMatches(TEST_USER).first()
        assertEquals(listOf("Second", "First"), recent.map { it.title })
        assertEquals("Leo", recent[0].winners)
        assertEquals(setOf("Maya", "Leo"), recent[1].winners!!.split(", ").toSet())
        assertEquals("⛳", recent[0].gameIcon)
    }

    @Test
    fun summaryCreditsTeamMembersWithTheirTeamsRank() = runBlocking {
        play(
            MatchSetup(golf, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 5, listOf(players.getValue("Maya"), players.getValue("Leo")), emptyList()),
            "Solo",
            10,
            4,
        )
        play(
            MatchSetup(
                golf,
                ScoringMode.TEAM,
                RoundMode.DYNAMIC,
                5,
                emptyList(),
                listOf(TeamSetup("Reds", listOf(players.getValue("Leo"), players.getValue("Ana"))), TeamSetup("Blues", listOf(players.getValue("Maya")))),
            ),
            "Teams",
            8,
            2,
        )

        val summary = stats.observeSummary(TEST_USER).first()
        assertEquals(2, summary.matchesPlayed)
        val byName = summary.players.associateBy { it.name }
        assertEquals(1, byName.getValue("Maya").wins)
        assertEquals(1, byName.getValue("Leo").wins)
        assertEquals(1, byName.getValue("Ana").wins)
        assertEquals(1, byName.getValue("Ana").played)
        assertEquals(2, byName.getValue("Leo").played)
        val game = summary.games.single()
        assertEquals(2, game.played)
        assertEquals(1, game.topWins)
        assertNull(stats.observeRecentMatches(TEST_USER).first().firstOrNull { it.title == "missing" })
    }
}
