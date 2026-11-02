package com.bzcode.scorenge.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.inlineTestDatabase
import com.bzcode.scorenge.seedUser
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
class GameRepositoryTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var repository: GameRepository

    @Before
    fun setUp() {
        db = inlineTestDatabase().also { it.seedUser() }
        repository = GameRepository(db.gameDao()) { 1_000L }
    }

    @After
    fun tearDown() = db.close()

    private fun draft(title: String, description: String? = null) = GameDraft(
        title = title,
        icon = "🎯",
        description = description,
        scoringDirection = ScoringDirection.LOWER_WINS,
        defaultScoringMode = ScoringMode.TEAM,
    )

    @Test
    fun createsAndUpdatesAGame() = runTest {
        val id = (repository.save(TEST_USER, null, draft(" Golf ", description = "  ")) as SaveResult.Saved).id
        val created = repository.getById(id)!!
        assertEquals("Golf", created.title)
        assertNull(created.description)
        assertEquals(ScoringDirection.LOWER_WINS, created.scoringDirection)
        assertEquals(ScoringMode.TEAM, created.defaultScoringMode)

        repository.save(TEST_USER, id, draft("Mini golf", description = "18 holes"))
        val updated = repository.getById(id)!!
        assertEquals("Mini golf", updated.title)
        assertEquals("18 holes", updated.description)
    }

    @Test
    fun blocksDuplicateTitlesUntilTheOtherGameIsDeleted() = runTest {
        val id = (repository.save(TEST_USER, null, draft("Carrom")) as SaveResult.Saved).id
        assertEquals(SaveResult.Invalid(NameError.DUPLICATE), repository.save(TEST_USER, null, draft("CARROM")))

        repository.delete(id)

        assertTrue(repository.observeActive(TEST_USER).first().isEmpty())
        assertTrue(repository.save(TEST_USER, null, draft("Carrom")) is SaveResult.Saved)
    }
}
