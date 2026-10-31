package com.bzcode.scorenge.domain.scoring

import com.bzcode.scorenge.domain.model.RoundStatus.ACTIVE
import com.bzcode.scorenge.domain.model.RoundStatus.COMPLETED
import com.bzcode.scorenge.domain.model.RoundStatus.PENDING
import com.bzcode.scorenge.domain.model.ScoringDirection.HIGHER_WINS
import com.bzcode.scorenge.domain.model.ScoringDirection.LOWER_WINS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringEngineTest {

    private val a = SubjectRef("a", "Player A")
    private val b = SubjectRef("b", "Player B")
    private val c = SubjectRef("c", "Player C")
    private val d = SubjectRef("d", "Player D")
    private val all = listOf(a, b, c, d)

    @Test
    fun `competition ranking skips after ties - spec example`() {
        val rounds = listOf(RoundScores(1, COMPLETED, mapOf("a" to 150L, "b" to 120L, "c" to 120L, "d" to 90L)))
        val board = ScoringEngine.leaderboard(all, rounds, HIGHER_WINS, RankBasis.OFFICIAL)
        assertEquals(listOf("a" to 1, "b" to 2, "c" to 2, "d" to 4), board.entries.map { it.subjectId to it.rank })
    }

    @Test
    fun `lower wins sorts ascending`() {
        val rounds = listOf(RoundScores(1, COMPLETED, mapOf("a" to 10L, "b" to 3L, "c" to 7L, "d" to 3L)))
        val board = ScoringEngine.leaderboard(all, rounds, LOWER_WINS, RankBasis.OFFICIAL)
        assertEquals(listOf("b" to 1, "d" to 1, "c" to 3, "a" to 4), board.entries.map { it.subjectId to it.rank })
        assertEquals(listOf("b", "d"), board.leaders.map { it.subjectId })
    }

    @Test
    fun `official totals ignore active and pending rounds`() {
        val rounds = listOf(
            RoundScores(1, COMPLETED, mapOf("a" to 5L, "b" to 8L)),
            RoundScores(2, ACTIVE, mapOf("a" to 10L, "b" to null)),
            RoundScores(3, PENDING, emptyMap()),
        )
        val board = ScoringEngine.leaderboard(listOf(a, b), rounds, HIGHER_WINS, RankBasis.OFFICIAL)
        val byId = board.entries.associateBy { it.subjectId }
        assertEquals(5L, byId.getValue("a").officialTotal)
        assertEquals(10L, byId.getValue("a").activeRoundScore)
        assertEquals(15L, byId.getValue("a").projectedTotal)
        assertEquals(8L, byId.getValue("b").projectedTotal)
        assertEquals("b", board.leaders.single().subjectId)
    }

    @Test
    fun `provisional view ranks by projected totals and is flagged`() {
        val rounds = listOf(
            RoundScores(1, COMPLETED, mapOf("a" to 5L, "b" to 8L)),
            RoundScores(2, ACTIVE, mapOf("a" to 10L)),
        )
        val board = ScoringEngine.leaderboard(listOf(a, b), rounds, HIGHER_WINS, RankBasis.PROVISIONAL)
        assertTrue(board.isProvisional)
        assertEquals("a", board.leaders.single().subjectId)
    }

    @Test
    fun `subjects without scores are unranked and listed last`() {
        val rounds = listOf(RoundScores(1, ACTIVE, mapOf("b" to 4L)))
        val official = ScoringEngine.leaderboard(listOf(a, b), rounds, HIGHER_WINS, RankBasis.OFFICIAL)
        assertTrue(official.entries.all { it.rank == null })
        assertTrue(official.leaders.isEmpty())

        val provisional = ScoringEngine.leaderboard(listOf(a, b), rounds, HIGHER_WINS, RankBasis.PROVISIONAL)
        assertEquals(listOf("b", "a"), provisional.entries.map { it.subjectId })
        assertEquals(1, provisional.entries[0].rank)
        assertNull(provisional.entries[1].rank)
    }

    @Test
    fun `zero is a real score and negatives are allowed`() {
        val rounds = listOf(
            RoundScores(1, COMPLETED, mapOf("a" to 0L, "b" to -3L)),
            RoundScores(2, COMPLETED, mapOf("a" to 0L, "b" to 2L)),
        )
        val board = ScoringEngine.leaderboard(listOf(a, b), rounds, HIGHER_WINS, RankBasis.OFFICIAL)
        assertEquals(listOf("a" to 0L, "b" to -1L), board.entries.map { it.subjectId to it.officialTotal })
    }

    @Test
    fun `missing scores distinguishes null from zero`() {
        val round = RoundScores(1, ACTIVE, mapOf("a" to 0L, "b" to null))
        assertEquals(listOf("b", "c"), ScoringEngine.missingScores(listOf(a, b, c), round))
    }

    @Test
    fun `cumulative series follows round number order and skips unfinished rounds`() {
        val rounds = listOf(
            RoundScores(2, COMPLETED, mapOf("a" to 3L, "b" to 1L)),
            RoundScores(1, COMPLETED, mapOf("a" to 5L, "b" to 9L)),
            RoundScores(3, ACTIVE, mapOf("a" to 100L)),
        )
        val series = ScoringEngine.cumulativeSeries(listOf(a, b), rounds)
        assertEquals(listOf(5L, 8L), series[0].totals)
        assertEquals(listOf(9L, 10L), series[1].totals)
    }
}
