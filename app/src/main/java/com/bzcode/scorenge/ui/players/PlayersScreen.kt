package com.bzcode.scorenge.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.repository.MAX_NAME_LENGTH
import com.bzcode.scorenge.ui.common.InitialAvatar
import com.bzcode.scorenge.ui.common.ScreenHeader

@Composable
fun PlayersPage(viewModel: PlayersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    PlayersScreen(
        state = state,
        editor = editor,
        onAdd = viewModel::startAdd,
        onEdit = viewModel::startEdit,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::save,
        onRemove = viewModel::remove,
        onDismiss = viewModel::dismissEditor,
    )
}

@Composable
fun PlayersScreen(
    state: PlayersUiState,
    editor: PlayerEditorState?,
    onAdd: () -> Unit,
    onEdit: (PlayerEntity) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Players") {
            Button(onClick = onAdd, shape = CircleShape) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add player")
            }
        }
        if (!state.loading && state.players.isEmpty()) {
            Text(
                text = "Add the people you play with. They're just names: nobody else needs the app.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.players, key = { it.id }) { player ->
                PlayerRow(player, onClick = { onEdit(player) })
            }
        }
    }

    if (editor != null) {
        PlayerEditorDialog(editor, onNameChange, onSave, onRemove, onDismiss)
    }
}

@Composable
private fun PlayerRow(player: PlayerEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            InitialAvatar(name = player.name, key = player.id)
            Spacer(Modifier.width(14.dp))
            Text(player.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.Edit,
                contentDescription = "Edit ${player.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerEditorDialog(
    editor: PlayerEditorState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val error = editor.error
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.playerId == null) "Add player" else "Edit player") },
        text = {
            Column {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { if (it.length <= MAX_NAME_LENGTH + 10) onNameChange(it) },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(error) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                if (editor.playerId != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove player", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = "Past matches keep their name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !editor.saving) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
