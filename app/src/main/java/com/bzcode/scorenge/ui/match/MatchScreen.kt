package com.bzcode.scorenge.ui.match

import androidx.compose.runtime.Composable
import com.bzcode.scorenge.ui.common.PlaceholderScreen

/** Stand-in until step 5 adds the round page and scoring. */
@Composable
fun MatchPage(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Match",
        message = "Your match has started. Round scoring arrives in the next update.",
        onBack = onBack,
    )
}
