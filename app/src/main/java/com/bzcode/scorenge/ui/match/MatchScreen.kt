package com.bzcode.scorenge.ui.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.ui.theme.ScorengeTheme

@Composable
fun MatchPage(
    onBack: () -> Unit,
    onOpenRound: (Int) -> Unit,
    onShowResults: () -> Unit,
    viewModel: MatchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.justFinished) { if (state.justFinished) onShowResults() }
    MatchScreen(
        state = state,
        onBack = onBack,
        onOpenRound = onOpenRound,
        onFinish = viewModel::finish,
        onShowResults = onShowResults,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    state: MatchUiState,
    onBack: () -> Unit,
    onOpenRound: (Int) -> Unit,
    onFinish: () -> Unit,
    onShowResults: () -> Unit,
) {
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        listOfNotNull(state.gameIcon, state.title).joinToString(" "),
                        style = MaterialTheme.typography.headlineSmall,
                        color = ScorengeTheme.colors.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (state.notFound) {
            Text(
                "This match no longer exists.",
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
            )
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            ProgressCard(state, onOpenRound, onShowResults)
            StandingsSection(state)
            if (state.rounds.isNotEmpty()) RoundsSection(state, onOpenRound)
            if (!state.finished) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { confirmFinish = true },
                        enabled = state.finishBlocker == null && !state.finishing,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text("Finish match") }
                    state.finishBlocker?.let {
                        Text(
                            it.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish match?") },
            text = {
                Text("The standings after ${state.completedRounds} completed rounds become the final result. Rounds not played are dropped, and a finished match can't be reopened.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    onFinish()
                }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep playing") } },
        )
    }
}

@Composable
private fun ProgressCard(state: MatchUiState, onOpenRound: (Int) -> Unit, onShowResults: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            val played = state.completedRounds
            Text(
                text = when {
                    state.finished -> "Finished · $played ${if (played == 1) "round" else "rounds"}"
                    state.totalRounds != null -> "$played of ${state.totalRounds} rounds played"
                    else -> "$played ${if (played == 1) "round" else "rounds"} played"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOf(state.gameTitle, joinNames(state.subjectNames)).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val next = state.nextRound
            when {
                state.finished -> Button(onClick = onShowResults, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("See results", style = MaterialTheme.typography.titleMedium)
                }
                next != null -> Button(onClick = { onOpenRound(next.number) }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (next) {
                            is NextRound.Start -> "Start round ${next.number}"
                            is NextRound.Continue -> "Continue round ${next.number}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                else -> Text(
                    "All rounds played. Finish the match to see the results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScorengeTheme.colors.brand,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun StandingsSection(state: MatchUiState) {
    Column {
        SectionTitle(if (state.finished) "Final standings" else "Standings")
        if (state.completedRounds == 0) {
            Text(
                "Standings appear after the first round is completed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TotalsBarChart(state.standings, Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
internal fun StandingRow(entry: LeaderboardEntry, provisional: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            entry.rank?.toString() ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        Text(entry.subjectName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(entry.totalText(provisional), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoundsSection(state: MatchUiState, onOpenRound: (Int) -> Unit) {
    Column {
        SectionTitle("Rounds")
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                state.rounds.forEachIndexed { index, round ->
                    if (index > 0) HorizontalDivider()
                    val openable = round.status == RoundStatus.COMPLETED || !state.finished
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = openable) { onOpenRound(round.number) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Round ${round.number}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                when (round.status) {
                                    RoundStatus.COMPLETED -> round.scores.joinToString(" · ") { it?.toString() ?: "–" }
                                    RoundStatus.ACTIVE -> "In progress"
                                    RoundStatus.PENDING -> "Not started"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (round.status != RoundStatus.PENDING) {
                            Text(
                                formatElapsed(round.elapsedMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
