package com.bzcode.scorenge.ui.players

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.auth.AuthUser
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.repository.NameError
import com.bzcode.scorenge.data.repository.PlayerRepository
import com.bzcode.scorenge.inlineTestDatabase
import com.bzcode.scorenge.seedUser
import com.bzcode.scorenge.ui.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlayersViewModelTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var viewModel: PlayersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inlineTestDatabase().also { it.seedUser() }
        viewModel = PlayersViewModel(
            FakeAuthRepository(AuthUser(TEST_USER, "Sam Lee", null, null)),
            PlayerRepository(db.playerDao()) { 1_000L },
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun activeNames() = runBlocking { db.playerDao().observeActive(TEST_USER).first().map { it.name } }

    @Test
    fun addThenDuplicateShowsErrorInTheDialog() {
        viewModel.startAdd()
        viewModel.onNameChange("Maya")
        viewModel.save()
        assertNull(viewModel.editor.value)
        assertEquals(listOf("Maya"), activeNames())

        viewModel.startAdd()
        viewModel.onNameChange("maya")
        viewModel.save()
        assertEquals(NameError.DUPLICATE.message, viewModel.editor.value?.error)

        viewModel.onNameChange("Leo")
        assertNull(viewModel.editor.value?.error)
    }

    @Test
    fun editThenRemove() {
        viewModel.startAdd()
        viewModel.onNameChange("Leo")
        viewModel.save()
        val leo = runBlocking { db.playerDao().observeActive(TEST_USER).first().single() }

        viewModel.startEdit(leo)
        viewModel.remove()

        assertNull(viewModel.editor.value)
        assertTrue(activeNames().isEmpty())
    }
}
