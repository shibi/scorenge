package com.bzcode.scorenge.data.auth

/** The signed-in scorekeeper. [uid] is the Firebase Auth uid and the users table's primary key. */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
) {
    /** First word of the display name, for greetings. */
    val firstName: String? get() = displayName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }
}
