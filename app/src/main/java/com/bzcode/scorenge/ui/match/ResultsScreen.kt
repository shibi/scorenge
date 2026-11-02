package com.bzcode.scorenge.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.ui.theme.ScorengeTheme

@Composable
fun ResultsPage(onDone: () -> Unit, viewModel: MatchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ResultsScreen(state = state, onDone = onDone)
}

/** Final standings. The podium and charts arrive in step 6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(state: MatchUiState, onDone: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Results", style = MaterialTheme.typography.headlineSmall, color = ScorengeTheme.colors.brand) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.loading || state.notFound) return@Scaffold
        val winners = state.standings.filter { it.rank == 1 }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            Text("🏆", style = MaterialTheme.typography.displayMedium)
            Text(
                when (winners.size) {
                    0 -> "No winner"
                    1 -> "${winners.single().subjectName} wins!"
                    else -> "${joinNames(winners.map { it.subjectName })} tie for first!"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                listOfNotNull(state.gameIcon, state.title).joinToString(" ") + " · " +
                    "${state.completedRounds} ${if (state.completedRounds == 1) "round" else "rounds"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ScorengeTheme.colors.cream,
                contentColor = ScorengeTheme.colors.onCream,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    state.standings.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = ScorengeTheme.colors.onCream.copy(alpha = 0.12f))
                        StandingRow(entry)
                    }
                }
            }
            Button(
                onClick = onDone,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Done", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
