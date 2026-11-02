package com.bzcode.scorenge.ui.games

import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode

/** Emoji choices for a game's icon. */
val GameIconChoices = listOf("🎲", "♟️", "🃏", "🎯", "🏓", "🎳", "🧩", "🏸", "⚽", "🏏", "🎮", "🀄")

val ScoringDirection.label: String
    get() = when (this) {
        ScoringDirection.HIGHER_WINS -> "Highest score wins"
        ScoringDirection.LOWER_WINS -> "Lowest score wins"
    }

val ScoringMode.label: String
    get() = when (this) {
        ScoringMode.INDIVIDUAL -> "Each player"
        ScoringMode.TEAM -> "Teams"
    }
