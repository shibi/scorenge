package com.bzcode.scorenge.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

// Warm palette for player initials; each passes 4.5:1 with white text.
private val AvatarColors = listOf(
    Color(0xFFC93D18), // deep orange
    Color(0xFF8A4B2E), // brown
    Color(0xFF15803D), // green
    Color(0xFF1D4ED8), // blue
    Color(0xFF7E22CE), // purple
    Color(0xFFB42318), // red
    Color(0xFF0F766E), // teal
    Color(0xFF4B5563), // slate
)

/** Circle with the first letter of [name], coloured stably from [key]. */
@Composable
fun InitialAvatar(name: String, key: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(AvatarColors[key.hashCode().absoluteValue % AvatarColors.size], CircleShape),
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
