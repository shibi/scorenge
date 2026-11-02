package com.bzcode.scorenge.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.repository.Clock
import com.bzcode.scorenge.data.repository.GameDraft
import com.bzcode.scorenge.data.repository.GameRepository
import com.bzcode.scorenge.data.repository.MatchRepository
import com.bzcode.scorenge.data.repository.PlayerRepository
import com.bzcode.scorenge.data.repository.RoundRepository
import com.bzcode.scorenge.data.repository.SaveResult
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.match.TeamSetup
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
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
class SyncEngineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remote = FakeRemoteStore()
    private var now = 1_000L
    private val clock = Clock { now }
    private val opened = mutableListOf<ScorengeDatabase>()

    private lateinit var db: ScorengeDatabase
    private lateinit var prefs: SyncPrefs
    private lateinit var engine: SyncEngine
    private lateinit var rounds: RoundRepository
    private lateinit var gameId: String
    private lateinit var maya: String
    private lateinit var leo: String

    @Before
    fun setUp() = runBlocking {
        db = newDevice()
        prefs = SyncPrefs(context)
        engine = SyncEngine(db, remote, prefs, clock)
        rounds = RoundRepository(db, clock, PrefsDeletionLog(prefs))
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
    fun tearDown() {
        opened.forEach { it.close() }
    }

    /** A fresh install: empty database and no sync state. */
    private fun newDevice(): ScorengeDatabase {
        context.getSharedPreferences("sync", Context.MODE_PRIVATE).edit().clear().commit()
        return Room.inMemoryDatabaseBuilder(context, ScorengeDatabase::class.java).build()
            .also { it.seedUser() }
            .also { opened += it }
    }

    private suspend fun playTeamMatch(): String {
        val id = MatchRepository(db, clock).startMatch(
            TEST_USER,
            MatchSetup(
                gameId, ScoringMode.TEAM, RoundMode.DYNAMIC, 0, emptyList(),
                listOf(TeamSetup("Reds", listOf(maya)), TeamSetup("Blues", listOf(leo))),
            ),
            "Sunday golf",
        )
        rounds.openRound(id, 1)
        val detail = rounds.observeDetail(id).first()!!
        val roundId = detail.rounds.single().id
        detail.subjects.forEachIndexed { i, s -> rounds.setScore(roundId, s.id, 3L + i) }
        assertTrue(rounds.completeRound(roundId))
        assertNull(rounds.finishMatch(id))
        return id
    }

    @Test
    fun firstUploadSendsEverythingAndLaterOnesOnlyChanges() = runBlocking {
        playTeamMatch()
        now = 2_000L
        engine.upload(TEST_USER)

        assertEquals("Sam Lee", remote.profiles[TEST_USER]?.get("displayName"))
        assertEquals(2, remote.docs("players").size)
        assertEquals(1, remote.docs("games").size)
        assertEquals(1, remote.docs("events").size)
        assertEquals(2, remote.docs("teams").size)
        assertEquals(2, remote.docs("team_members").size)
        assertEquals(1, remote.docs("rounds").size)
        assertEquals(2, remote.docs("round_scores").size)
        assertTrue(prefs.lastSyncedAt(TEST_USER) != null)

        remote.putLog.clear()
        now = 50_000L
        PlayerRepository(db.playerDao(), clock).save(TEST_USER, maya, "Maya B")
        engine.upload(TEST_USER)
        assertEquals(listOf("players" to 1), remote.putLog)
        assertEquals("Maya B", remote.docs("players").getValue(maya)["name"])
    }

    @Test
    fun failedUploadIsRetriedInFull() = runBlocking {
        playTeamMatch()
        remote.failing = true
        runCatching { engine.upload(TEST_USER) }
        assertNull(prefs.lastSyncedAt(TEST_USER))

        remote.failing = false
        engine.upload(TEST_USER)
        assertEquals(1, remote.docs("events").size)
        assertEquals(2, remote.docs("round_scores").size)
    }

    @Test
    fun discardedRoundsAreDeletedFromTheCloud() = runBlocking {
        val id = MatchRepository(db, clock).startMatch(
            TEST_USER,
            MatchSetup(gameId, ScoringMode.INDIVIDUAL, RoundMode.DYNAMIC, 0, listOf(maya, leo), emptyList()),
            "",
        )
        rounds.openRound(id, 1)
        val first = rounds.observeDetail(id).first()!!
        first.subjects.forEach { rounds.setScore(first.rounds.single().id, it.id, 2) }
        rounds.completeRound(first.rounds.single().id)
        engine.upload(TEST_USER)
        assertEquals(2, remote.docs("rounds").size) // round 1 + pending round 2

        now = 9_000L
        assertNull(rounds.finishMatch(id))
        engine.upload(TEST_USER)
        assertEquals(1, remote.docs("rounds").size)
        assertEquals(EventStatus.COMPLETED.name, remote.docs("events").getValue(id)["status"])
        assertTrue(prefs.tombstones(TEST_USER).isEmpty())
    }

    @Test
    fun newPhoneRestoresTheWholeMatch() = runBlocking {
        val id = playTeamMatch()
        engine.upload(TEST_USER)

        now = 100_000L
        val phone = newDevice()
        val phonePrefs = SyncPrefs(context)
        val restored = SyncEngine(phone, remote, phonePrefs, clock).restoreIfNeeded(TEST_USER)

        assertEquals(remote.collections.values.sumOf { it.size }, restored)
        val detail = RoundRepository(phone, clock).observeDetail(id).first()!!
        assertEquals("Sunday golf", detail.event.title)
        assertEquals(EventStatus.COMPLETED, detail.event.status)
        assertEquals(listOf("Reds", "Blues"), detail.subjects.map { it.name })
        assertEquals(listOf(1, 2), detail.subjects.mapNotNull { it.finalRank }.sorted())
        assertEquals(RoundStatus.COMPLETED, detail.rounds.single().status)
        assertEquals(listOf(3L, 4L), detail.scores.mapNotNull { it.score }.sorted())
        assertEquals(2, phone.syncDao().teamMembersSince(TEST_USER, 0).size)
        assertTrue(phonePrefs.restoreChecked(TEST_USER))

        // Restored rows are already in the cloud, so the next upload sends nothing new.
        remote.putLog.clear()
        SyncEngine(phone, remote, phonePrefs, clock).upload(TEST_USER)
        assertTrue(remote.putLog.isEmpty())
    }

    @Test
    fun restoreSkipsBrokenDocumentsAndKeepsTheRest() = runBlocking {
        playTeamMatch()
        engine.upload(TEST_USER)
        remote.put(TEST_USER, "players", listOf("bad" to mapOf("id" to "bad", "userId" to TEST_USER)))
        remote.put(
            TEST_USER,
            "rounds",
            listOf(
                "orphan" to SyncCollection.Rounds.toDoc(
                    SyncCollection.Rounds.fromDoc(remote.docs("rounds").values.first()).copy(id = "orphan", eventId = "gone"),
                ),
            ),
        )

        val phone = newDevice()
        val restored = SyncEngine(phone, remote, SyncPrefs(context), clock).restoreIfNeeded(TEST_USER)

        assertEquals(remote.collections.values.sumOf { it.size } - 2, restored)
        assertEquals(1, phone.syncDao().roundsSince(TEST_USER, 0).size)
        assertEquals(2, phone.syncDao().playersSince(TEST_USER, 0).size)
    }

    @Test
    fun restoreNeverTouchesAPhoneThatAlreadyHasData() = runBlocking {
        playTeamMatch()
        engine.upload(TEST_USER)
        remote.collections["events"]!!.clear()

        // This device already has data, so the cloud copy is not merged in.
        assertEquals(0, engine.restoreIfNeeded(TEST_USER))
        assertTrue(prefs.restoreChecked(TEST_USER))
        assertEquals(1, db.syncDao().eventsSince(TEST_USER, 0).size)
        assertFalse(db.syncDao().eventsSince(TEST_USER, 0).isEmpty())
    }

    @Test
    fun everyCollectionSurvivesTheRoundTrip() = runBlocking {
        playTeamMatch()
        engine.upload(TEST_USER)
        SyncCollection.all.forEach { collection ->
            remote.docs(collection.name).values.forEach { doc ->
                @Suppress("UNCHECKED_CAST")
                val c = collection as SyncCollection<Any>
                assertEquals(doc, remote.normalize(c.toDoc(c.fromDoc(doc))))
            }
        }
    }
}

private fun FakeRemoteStore.normalize(doc: Doc): Doc = doc.mapValues { (_, v) -> if (v is Int) v.toLong() else v }
