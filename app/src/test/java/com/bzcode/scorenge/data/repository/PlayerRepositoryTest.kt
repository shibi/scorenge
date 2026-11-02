package com.bzcode.scorenge.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.inlineTestDatabase
import com.bzcode.scorenge.seedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerRepositoryTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var repository: PlayerRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        db = inlineTestDatabase().also { it.seedUser() }
        repository = PlayerRepository(db.playerDao()) { now }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addsWithNormalizedNameAndRejectsBadOrDuplicateNames() = runTest {
        val saved = repository.save(TEST_USER, null, "  Maya   K ") as SaveResult.Saved
        assertEquals("Maya K", db.playerDao().getById(saved.id)?.name)

        assertEquals(SaveResult.Invalid(NameError.BLANK), repository.save(TEST_USER, null, "   "))
        assertEquals(SaveResult.Invalid(NameError.TOO_LONG), repository.save(TEST_USER, null, "x".repeat(41)))
        assertEquals(SaveResult.Invalid(NameError.DUPLICATE), repository.save(TEST_USER, null, "maya k"))
    }

    @Test
    fun renamingKeepsIdAndAllowsChangingOnlyCase() = runTest {
        val id = (repository.save(TEST_USER, null, "leo") as SaveResult.Saved).id
        now = 2_000L

        assertEquals(SaveResult.Saved(id), repository.save(TEST_USER, id, "Leo"))
        val player = db.playerDao().getById(id)!!
        assertEquals("Leo", player.name)
        assertEquals(1_000L, player.createdAt)
        assertEquals(2_000L, player.updatedAt)
    }

    @Test
    fun removeIsASoftDeleteAndFreesTheName() = runTest {
        val id = (repository.save(TEST_USER, null, "Leo") as SaveResult.Saved).id
        now = 3_000L

        repository.remove(id)

        assertTrue(repository.observeActive(TEST_USER).first().isEmpty())
        assertEquals(3_000L, db.playerDao().getById(id)?.deletedAt)
        assertTrue(repository.save(TEST_USER, null, "Leo") is SaveResult.Saved)
    }
}
