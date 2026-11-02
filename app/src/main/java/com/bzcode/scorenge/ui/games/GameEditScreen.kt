package com.bzcode.scorenge.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.ui.theme.ScorengeTheme

@Composable
fun GameEditPage(onDone: () -> Unit, viewModel: GameEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    GameEditScreen(
        state = state,
        onBack = onDone,
        onTitleChange = viewModel::onTitleChange,
        onIconChange = viewModel::onIconChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDirectionChange = viewModel::onDirectionChange,
        onModeChange = viewModel::onModeChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditScreen(
    state: GameEditUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onIconChange: (String?) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDirectionChange: (ScoringDirection) -> Unit,
    onModeChange: (ScoringMode) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) "New game" else "Edit game",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ScorengeTheme.colors.brand,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete game")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.saving && !state.loading,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp),
            ) {
                Text(if (state.isNew) "Create game" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            val titleError = state.titleError
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Game name") },
                placeholder = { Text("Carrom") },
                singleLine = true,
                isError = titleError != null,
                supportingText = if (titleError != null) {
                    { Text(titleError) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Icon") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    IconChoice(selected = state.icon == null, label = "No icon", onClick = { onIconChange(null) }) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GameIconChoices.forEach { emoji ->
                        IconChoice(selected = state.icon == emoji, label = emoji, onClick = { onIconChange(emoji) }) {
                            Text(emoji, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            Section("Who wins") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ScoringDirection.entries.forEachIndexed { index, direction ->
                        SegmentedButton(
                            selected = state.scoringDirection == direction,
                            onClick = { onDirectionChange(direction) },
                            shape = SegmentedButtonDefaults.itemShape(index, ScoringDirection.entries.size),
                        ) { Text(if (direction == ScoringDirection.HIGHER_WINS) "Highest score" else "Lowest score") }
                    }
                }
            }

            Section("Keep score for") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ScoringMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.scoringMode == mode,
                            onClick = { onModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ScoringMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
                Text(
                    "The default for new matches; you can change it when starting one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text("Notes (optional)") },
                placeholder = { Text("House rules, target score…") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.title}?") },
            text = { Text("It disappears from your games. Finished matches keep their results.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
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
private fun IconChoice(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else ScorengeTheme.colors.cream,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier
            .size(52.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
