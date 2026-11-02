package com.bzcode.scorenge.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.domain.match.MatchSetupRules
import com.bzcode.scorenge.domain.model.MatchScoring
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.ui.games.label
import com.bzcode.scorenge.ui.theme.ScorengeTheme

/** Callbacks for the setup form, grouped to keep signatures short. */
interface NewMatchActions {
    fun selectGame(game: GameEntity)
    fun onTitleChange(value: String)
    fun setScoringMode(mode: ScoringMode)
    fun setScoring(scoring: MatchScoring)
    fun setRoundMode(mode: RoundMode)
    fun setTotalRounds(value: Int)
    fun togglePlayer(playerId: String)
    fun addTeam()
    fun removeTeam(key: String)
    fun renameTeam(key: String, name: String)
    fun toggleTeamPlayer(key: String, playerId: String)
    fun onNewPlayerNameChange(value: String)
    fun addNewPlayer()
    fun start()
}

@Composable
fun NewMatchPage(
    onBack: () -> Unit,
    onCreateGame: () -> Unit,
    onStarted: (String) -> Unit,
    viewModel: NewMatchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val startedId = state.form.startedEventId
    LaunchedEffect(startedId) { if (startedId != null) onStarted(startedId) }
    val actions = object : NewMatchActions {
        override fun selectGame(game: GameEntity) = viewModel.selectGame(game)
        override fun onTitleChange(value: String) = viewModel.onTitleChange(value)
        override fun setScoringMode(mode: ScoringMode) = viewModel.setScoringMode(mode)
        override fun setScoring(scoring: MatchScoring) = viewModel.setScoring(scoring)
        override fun setRoundMode(mode: RoundMode) = viewModel.setRoundMode(mode)
        override fun setTotalRounds(value: Int) = viewModel.setTotalRounds(value)
        override fun togglePlayer(playerId: String) = viewModel.togglePlayer(playerId)
        override fun addTeam() = viewModel.addTeam()
        override fun removeTeam(key: String) = viewModel.removeTeam(key)
        override fun renameTeam(key: String, name: String) = viewModel.renameTeam(key, name)
        override fun toggleTeamPlayer(key: String, playerId: String) = viewModel.toggleTeamPlayer(key, playerId)
        override fun onNewPlayerNameChange(value: String) = viewModel.onNewPlayerNameChange(value)
        override fun addNewPlayer() = viewModel.addNewPlayer()
        override fun start() = viewModel.start()
    }
    NewMatchScreen(state = state, actions = actions, onBack = onBack, onCreateGame = onCreateGame)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewMatchScreen(
    state: NewMatchUiState,
    actions: NewMatchActions,
    onBack: () -> Unit,
    onCreateGame: () -> Unit,
) {
    val form = state.form
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New match", style = MaterialTheme.typography.headlineSmall, color = ScorengeTheme.colors.brand) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                val hint = form.startError ?: state.problems.firstOrNull()?.message
                if (hint != null && !state.loading) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (form.startError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Button(
                    onClick = actions::start,
                    enabled = state.canStart,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("Start match", style = MaterialTheme.typography.titleMedium) }
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Section("Game") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.games.forEach { game ->
                        FilterChip(
                            selected = form.gameId == game.id,
                            onClick = { actions.selectGame(game) },
                            label = { Text(listOfNotNull(game.icon, game.title).joinToString(" ")) },
                            leadingIcon = if (form.gameId == game.id) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onCreateGame,
                        label = { Text("New game") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    )
                }
            }

            OutlinedTextField(
                value = form.title,
                onValueChange = actions::onTitleChange,
                label = { Text("Match name (optional)") },
                placeholder = { Text(state.defaultTitle.ifEmpty { "Friday game night" }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Keep score for") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ScoringMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = form.scoringMode == mode,
                            onClick = { actions.setScoringMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ScoringMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
            }

            Section("How to score") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MatchScoring.entries.forEachIndexed { index, scoring ->
                        SegmentedButton(
                            selected = form.scoring == scoring,
                            onClick = { actions.setScoring(scoring) },
                            shape = SegmentedButtonDefaults.itemShape(index, MatchScoring.entries.size),
                            icon = {},
                        ) { Text(scoring.shortLabel, maxLines = 1) }
                    }
                }
                Hint(form.scoring.hint)
            }

            Section("Rounds") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = form.roundMode == RoundMode.DYNAMIC,
                        onClick = { actions.setRoundMode(RoundMode.DYNAMIC) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Unlimited") }
                    SegmentedButton(
                        selected = form.roundMode == RoundMode.FIXED,
                        onClick = { actions.setRoundMode(RoundMode.FIXED) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Fixed") }
                }
                if (form.roundMode == RoundMode.FIXED) {
                    RoundStepper(form.totalRounds, actions::setTotalRounds)
                } else {
                    Hint("Play as many rounds as you like, then finish the match.")
                }
            }

            when (form.scoringMode) {
                ScoringMode.INDIVIDUAL -> Section("Players") {
                    if (state.players.isEmpty()) Hint("Add the people playing below.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.players.forEach { player ->
                            val order = form.playerIds.indexOf(player.id)
                            FilterChip(
                                selected = order >= 0,
                                onClick = { actions.togglePlayer(player.id) },
                                label = { Text(if (order >= 0) "${order + 1}. ${player.name}" else player.name) },
                            )
                        }
                    }
                    QuickAddPlayer(form.newPlayerName, form.newPlayerError, actions)
                }
                ScoringMode.TEAM -> Section("Teams") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        form.teams.forEach { team -> TeamCard(team, form.teams, state.players, actions) }
                        OutlinedButton(onClick = actions::addTeam, shape = CircleShape) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add team")
                        }
                        QuickAddPlayer(form.newPlayerName, form.newPlayerError, actions)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun RoundStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > MatchSetupRules.MIN_ROUNDS) {
            Icon(Icons.Rounded.Remove, contentDescription = "Fewer rounds")
        }
        Text(
            text = "$value rounds",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < MatchSetupRules.MAX_ROUNDS) {
            Icon(Icons.Rounded.Add, contentDescription = "More rounds")
        }
    }
}

@Composable
private fun QuickAddPlayer(name: String, error: String?, actions: NewMatchActions) {
    OutlinedTextField(
        value = name,
        onValueChange = actions::onNewPlayerNameChange,
        label = { Text("Add a new player") },
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        trailingIcon = {
            TextButton(onClick = actions::addNewPlayer, enabled = name.isNotBlank()) { Text("Add") }
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) actions.addNewPlayer() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamCard(team: TeamDraft, allTeams: List<TeamDraft>, players: List<PlayerEntity>, actions: NewMatchActions) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = team.name,
                    onValueChange = { actions.renameTeam(team.key, it) },
                    label = { Text("Team name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { actions.removeTeam(team.key) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove ${team.name}")
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                players.forEach { player ->
                    val here = player.id in team.playerIds
                    val elsewhere = allTeams.firstOrNull { it.key != team.key && player.id in it.playerIds }
                    FilterChip(
                        selected = here,
                        onClick = { actions.toggleTeamPlayer(team.key, player.id) },
                        label = { Text(player.name) },
                        colors = if (elsewhere != null) {
                            FilterChipDefaults.filterChipColors(labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        } else {
                            FilterChipDefaults.filterChipColors()
                        },
                        modifier = Modifier.semantics {
                            if (elsewhere != null) contentDescription = "${player.name}, on ${elsewhere.name}"
                        },
                    )
                }
            }
        }
    }
}

private val MatchScoring.shortLabel: String
    get() = when (this) {
        MatchScoring.HIGHEST_WINS -> "Highest"
        MatchScoring.LOWEST_WINS -> "Lowest"
        MatchScoring.FINISHING_ORDER -> "Finish order"
    }

private val MatchScoring.hint: String
    get() = when (this) {
        MatchScoring.HIGHEST_WINS -> "Type each round's score. Highest total wins."
        MatchScoring.LOWEST_WINS -> "Type each round's score. Lowest total wins."
        MatchScoring.FINISHING_ORDER ->
            "Each round, tap players in the order they finished. 1st gets the most points, last gets 1. Highest total wins."
    }
