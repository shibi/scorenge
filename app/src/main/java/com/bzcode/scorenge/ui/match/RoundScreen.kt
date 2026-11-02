package com.bzcode.scorenge.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.RoundTimer
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import kotlinx.coroutines.delay

@Composable
fun RoundPage(
    onBack: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The round's clock runs only while this page is on screen.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onPageVisible() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onPageHidden() }
    LaunchedEffect(state.justCompleted) { if (state.justCompleted) onBack() }
    RoundScreen(
        state = state,
        onBack = onBack,
        onScoreChange = viewModel::onScoreChange,
        onComplete = viewModel::completeRound,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundScreen(
    state: RoundUiState,
    onBack: () -> Unit,
    onScoreChange: (subjectId: String, text: String) -> Unit,
    onComplete: () -> Unit,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            roundLabel(state.roundNumber, state.totalRounds),
                            style = MaterialTheme.typography.headlineSmall,
                            color = ScorengeTheme.colors.brand,
                        )
                        Text(
                            listOfNotNull(state.gameIcon, state.matchTitle).joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (state.editable) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    if (state.missingCount > 0) {
                        Text(
                            if (state.missingCount == 1) "Enter 1 more score to complete the round." else "Enter ${state.missingCount} more scores to complete the round.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = onComplete,
                        enabled = state.canComplete,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Complete round", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (state.notFound) {
            Text("This round no longer exists.", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ClockCard(state, nowMillis)
            if (state.editable && state.leaders.isNotEmpty()) LeadingNowCard(state)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.fields.forEachIndexed { index, field ->
                    ScoreRow(
                        entry = field,
                        enabled = state.editable,
                        last = index == state.fields.lastIndex,
                        onChange = { onScoreChange(field.subjectId, it) },
                    )
                }
            }
            if (!state.editable && state.status == RoundStatus.COMPLETED) {
                Text(
                    "This round is complete and its scores are locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ClockCard(state: RoundUiState, nowMillis: () -> Long) {
    val timer = RoundTimer(state.elapsedMillis, state.openedAt.takeIf { state.editable })
    val elapsed by produceState(timer.elapsedMillis(nowMillis()), timer) {
        value = timer.elapsedMillis(nowMillis())
        while (timer.isRunning) {
            delay(1_000)
            value = timer.elapsedMillis(nowMillis())
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = ScorengeTheme.colors.brand)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(formatElapsed(elapsed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    state.startedAt?.let { "Started ${formatClockTime(it)}" } ?: "Not started",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.editable) {
                Text(
                    "Pauses when you leave this page",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun LeadingNowCard(state: RoundUiState) {
    val leaders = state.leaders
    Surface(
        shape = MaterialTheme.shapes.large,
        color = ScorengeTheme.colors.cream,
        contentColor = ScorengeTheme.colors.onCream,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Leading now", style = MaterialTheme.typography.labelLarge, color = ScorengeTheme.colors.brand)
            Text(
                "${joinNames(leaders.map { it.subjectName })} · ${leaders.first().totalText(provisional = true)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Includes this round's scores so far. Not final until the round is completed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ScoreRow(entry: ScoreField, enabled: Boolean, last: Boolean, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "Total so far ${entry.totalBefore}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = entry.text,
            onValueChange = onChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text("–") },
            label = { Text("Score") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
            ),
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .width(120.dp)
                .semantics { contentDescription = "Score for ${entry.name}" },
        )
    }
}
