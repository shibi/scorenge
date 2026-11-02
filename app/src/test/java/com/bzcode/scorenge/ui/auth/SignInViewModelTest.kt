package com.bzcode.scorenge.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.auth.GoogleIdTokenProvider
import com.bzcode.scorenge.ui.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SignInViewModelTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = FakeAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun successSignsInWithTheGoogleToken() {
        val viewModel = SignInViewModel(repository, returning("id-token"))

        viewModel.signInWithGoogle(context)

        assertEquals("id-token", repository.lastIdToken)
        assertEquals("uid-1", repository.user.value?.uid)
        assertEquals(SignInUiState(), viewModel.uiState.value)
    }

    @Test
    fun cancellingThePickerShowsNoError() {
        val viewModel = SignInViewModel(repository, throwing(GetCredentialCancellationException()))

        viewModel.signInWithGoogle(context)

        assertFalse(viewModel.uiState.value.inProgress)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun noAccountAndFirebaseFailuresShowAMessage() {
        val noAccount = SignInViewModel(repository, throwing(NoCredentialException()))
        noAccount.signInWithGoogle(context)
        assertNotNull(noAccount.uiState.value.errorMessage)

        repository.signInError = IOException("offline")
        val offline = SignInViewModel(repository, returning("id-token"))
        offline.signInWithGoogle(context)
        assertEquals(
            "No connection. Connect to the internet for the first sign-in.",
            offline.uiState.value.errorMessage,
        )
        assertNull(repository.user.value)

        offline.dismissError()
        assertNull(offline.uiState.value.errorMessage)
    }

    private fun returning(token: String) = object : GoogleIdTokenProvider {
        override suspend fun requestIdToken(activityContext: Context): String = token
    }

    private fun throwing(e: Exception) = object : GoogleIdTokenProvider {
        override suspend fun requestIdToken(activityContext: Context): String = throw e
    }
}
