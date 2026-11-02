package com.bzcode.scorenge.ui

import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(initial: AuthUser? = null) : AuthRepository {
    val user = MutableStateFlow(initial)
    var signInError: Exception? = null
    var lastIdToken: String? = null

    override val currentUser = user

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        lastIdToken = idToken
        signInError?.let { throw it }
        return AuthUser("uid-1", "Sam Lee", "sam@example.com", null).also { user.value = it }
    }

    override suspend fun signOut() {
        user.value = null
    }
}
