package com.bzcode.scorenge.data.repository

/** Trims and collapses inner whitespace, so "  Maya   K " is stored as "Maya K". */
fun normalizeName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

/** Why a name can't be saved. */
enum class NameError(val message: String) {
    BLANK("Enter a name."),
    TOO_LONG("Keep it under $MAX_NAME_LENGTH characters."),
    DUPLICATE("That name is already in your list."),
}

const val MAX_NAME_LENGTH = 40

/** Checks a normalized name's shape; duplicates are checked against the database separately. */
fun checkNameShape(name: String): NameError? = when {
    name.isEmpty() -> NameError.BLANK
    name.length > MAX_NAME_LENGTH -> NameError.TOO_LONG
    else -> null
}

/** Outcome of a save. */
sealed interface SaveResult {
    data class Saved(val id: String) : SaveResult
    data class Invalid(val error: NameError) : SaveResult
}
