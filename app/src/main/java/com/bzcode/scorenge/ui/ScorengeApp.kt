package com.bzcode.scorenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.ui.auth.SignInRoute
import com.bzcode.scorenge.ui.home.HomeViewModel
import com.bzcode.scorenge.ui.navigation.ScorengeAppRoot

/** Top level: sign-in gate, then the main app for the signed-in user. */
@Composable
fun ScorengeApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (val state = authState) {
        AuthState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        AuthState.SignedOut -> SignInRoute()
        is AuthState.SignedIn -> {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val inProgress by homeViewModel.inProgress.collectAsStateWithLifecycle()
            ScorengeAppRoot(user = state.user, onSignOut = viewModel::signOut, inProgress = inProgress)
        }
    }
}
