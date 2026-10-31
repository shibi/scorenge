package com.bzcode.scorenge.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bzcode.scorenge.ui.theme.ScorengeTheme

/**
 * Floating pill-shaped bottom bar: Home, All games, "+ New match", Players, Stats.
 */
@Composable
fun PillNavBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onNewMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val destinations = TopLevelDestination.entries
                destinations.take(2).forEach { destination ->
                    NavIconItem(destination, destination == selected) { onSelect(destination) }
                }
                NewMatchPill(onClick = onNewMatch, modifier = Modifier.weight(1f, fill = false))
                destinations.drop(2).forEach { destination ->
                    NavIconItem(destination, destination == selected) { onSelect(destination) }
                }
            }
        }
    }
}

@Composable
private fun NavIconItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label,
            tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewMatchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.primaryContainer,
        contentColor = colors.primary,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.6f)),
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "New match",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun PillNavBarPreview() {
    ScorengeTheme {
        PillNavBar(selected = TopLevelDestination.HOME, onSelect = {}, onNewMatch = {})
    }
}
