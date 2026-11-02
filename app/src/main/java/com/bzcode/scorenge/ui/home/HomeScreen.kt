package com.bzcode.scorenge.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bzcode.scorenge.data.repository.InProgressMatch
import com.bzcode.scorenge.ui.theme.ScorengeTheme

/**
 * Hub home, following the approved "Hub Home (Updated Matches)" design: the match in progress (or a
 * prompt to start one) and recent matches, which are wired to data in step 6.
 */
@Composable
fun HomeScreen(
    firstName: String?,
    inProgress: InProgressMatch?,
    onNewMatch: () -> Unit,
    onContinueMatch: (String) -> Unit,
    onSearchGames: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Header(firstName, onOpenSettings) }
        item { SearchPill(onSearchGames) }
        item {
            if (inProgress != null) {
                ContinueMatchCard(inProgress, onContinue = { onContinueMatch(inProgress.eventId) })
            } else {
                NoMatchCard(onNewMatch)
            }
        }
        item { RecentMatchesSection() }
    }
}

@Composable
private fun Header(firstName: String?, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (firstName != null) "Hi $firstName" else "Hi there",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Ready to roll",
                style = MaterialTheme.typography.headlineLarge,
                color = ScorengeTheme.colors.brand,
                modifier = Modifier.semantics { heading() },
            )
        }
        OutlinedIconButton(
            onClick = onOpenSettings,
            border = BorderStroke(2.dp, ScorengeTheme.colors.brand),
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = "Settings",
                tint = ScorengeTheme.colors.brand,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Search games…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NoMatchCard(onNewMatch: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = "No match in progress",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Start a match to keep score round by round.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onNewMatch,
                shape = CircleShape,
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start a new match", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ContinueMatchCard(match: InProgressMatch, onContinue: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = "Match in progress",
                style = MaterialTheme.typography.labelLarge,
                color = ScorengeTheme.colors.brand,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = listOfNotNull(match.game?.icon, match.title).joinToString(" "),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Round ${match.roundNumber.coerceAtLeast(1)} · ${match.subjectNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onContinue,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Continue match", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RecentMatchesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = ScorengeTheme.colors.brand)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recent matches",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = ScorengeTheme.colors.cream,
            contentColor = ScorengeTheme.colors.onCream,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Finished matches will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFBEC)
@Composable
private fun HomeScreenPreview() {
    ScorengeTheme {
        HomeScreen(
            firstName = "Sam",
            inProgress = null,
            onNewMatch = {},
            onContinueMatch = {},
            onSearchGames = {},
            onOpenSettings = {},
        )
    }
}
