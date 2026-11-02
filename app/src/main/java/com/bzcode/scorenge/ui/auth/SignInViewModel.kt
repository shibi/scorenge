package com.bzcode.scorenge.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.auth.GoogleIdTokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class SignInUiState(
    val inProgress: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** On success the app's auth state changes and the sign-in screen goes away. */
    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.inProgress) return
        _uiState.value = SignInUiState(inProgress = true)
        viewModelScope.launch {
            try {
                val idToken = googleIdTokenProvider.requestIdToken(activityContext)
                authRepository.signInWithGoogle(idToken)
                _uiState.value = SignInUiState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SignInUiState(errorMessage = signInErrorMessage(e))
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}

/** User-facing text for a failed sign-in; null when the user simply backed out. */
internal fun signInErrorMessage(e: Exception): String? = when (e) {
    is GetCredentialCancellationException -> null
    is NoCredentialException -> "No Google account found on this device. Add one in Settings, then try again."
    is GetCredentialException -> "Google sign-in didn't work. Please try again."
    is IOException -> "No connection. Connect to the internet for the first sign-in."
    else -> "Sign-in failed. Please try again."
}
