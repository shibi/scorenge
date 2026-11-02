package com.bzcode.scorenge.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user, or null. Firebase keeps the session across restarts and offline. */
    val currentUser: Flow<AuthUser?>

    /** Signs in to Firebase with a Google ID token and saves the user locally. */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /** Signs out of Firebase and clears the saved Google credential choice. Local data stays on the device. */
    suspend fun signOut()
}
