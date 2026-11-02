package com.bzcode.scorenge.ui

import com.bzcode.scorenge.data.auth.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun authStateFollowsTheRepositoryAndSignOutClearsIt() = runTest(UnconfinedTestDispatcher()) {
        val user = AuthUser("uid-1", "Sam Lee", null, null)
        val repository = FakeAuthRepository(initial = user)
        val viewModel = AppViewModel(repository)
        val collector = backgroundScope.launch { viewModel.authState.collect {} }

        assertEquals(AuthState.SignedIn(user), viewModel.authState.value)

        viewModel.signOut()
        assertEquals(AuthState.SignedOut, viewModel.authState.value)
        collector.cancel()
    }
}
