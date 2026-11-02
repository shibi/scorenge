package com.bzcode.scorenge.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.domain.stats.GameStats
import com.bzcode.scorenge.domain.stats.PlayerStats
import com.bzcode.scorenge.domain.stats.StatsSummary
import com.bzcode.scorenge.ui.theme.ScorengeTheme

@Composable
fun StatsPage(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.loading) StatsScreen(state.summary)
}

/** Wins and podiums per player and matches per game, from finished matches only. */
@Composable
fun StatsScreen(summary: StatsSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Stats",
                style = MaterialTheme.typography.headlineLarge,
                color = ScorengeTheme.colors.brand,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                when (summary.matchesPlayed) {
                    0 -> "Finish a match to see wins and podiums here."
                    1 -> "From 1 finished match"
                    else -> "From ${summary.matchesPlayed} finished matches"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.players.isNotEmpty()) {
            item { SectionTitle("Players") }
            item { PlayersTable(summary.players) }
        }
        if (summary.games.isNotEmpty()) {
            item { SectionTitle("Games") }
            items(summary.games, key = { it.gameId }) { GameRow(it) }
        }
        item { Spacer(Modifier.padding(bottom = 96.dp)) } // room above the floating bar
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() },
    )
}

@Composable
private fun PlayersTable(players: List<PlayerStats>) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Player", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                listOf("Played", "Wins", "Podiums").forEach { HeaderCell(it) }
            }
            players.forEach { player ->
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(player.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${player.winPercent}% wins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ValueCell(player.played.toString())
                    ValueCell(player.wins.toString(), bold = true)
                    ValueCell(player.podiums.toString())
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.End,
        modifier = Modifier.widthIn(min = 64.dp),
    )
}

@Composable
private fun ValueCell(text: String, bold: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.End,
        modifier = Modifier.widthIn(min = 64.dp),
    )
}

@Composable
private fun GameRow(game: GameStats) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = ScorengeTheme.colors.cream,
        contentColor = ScorengeTheme.colors.onCream,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(game.icon ?: "🎲", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        if (game.played == 1) "1 match" else "${game.played} matches",
                        game.topWinners.takeIf { it.isNotEmpty() }?.let { names ->
                            "most wins: ${names.joinToString(", ")} (${game.topWins})"
                        },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
