package com.bzcode.scorenge.ui.match

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.repository.MatchDetail
import com.bzcode.scorenge.data.repository.RoundRepository
import com.bzcode.scorenge.domain.match.FinishBlocker
import com.bzcode.scorenge.domain.match.RoundRules
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.CumulativeSeries
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.domain.scoring.RankBasis
import com.bzcode.scorenge.domain.scoring.ScoringEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoundRow(
    val number: Int,
    val status: RoundStatus,
    val elapsedMillis: Long,
    /** Completed rounds: each subject's score in subject order. */
    val scores: List<Long?>,
)

/** What the round button on the match page does. */
sealed interface NextRound {
    val number: Int
    data class Start(override val number: Int) : NextRound
    data class Continue(override val number: Int) : NextRound
}

data class MatchUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val gameIcon: String? = null,
    val gameTitle: String = "",
    val finished: Boolean = false,
    val totalRounds: Int? = null,
    val completedRounds: Int = 0,
    val subjectNames: List<String> = emptyList(),
    /** Official standings: completed rounds only. After finishing these are the final results. */
    val standings: List<LeaderboardEntry> = emptyList(),
    val rounds: List<RoundRow> = emptyList(),
    /** Running totals after each completed round, for the line chart. */
    val series: List<CumulativeSeries> = emptyList(),
    val nextRound: NextRound? = null,
    val finishBlocker: FinishBlocker? = null,
    val finishing: Boolean = false,
    /** Set once this screen finished the match; the screen then shows the results. */
    val justFinished: Boolean = false,
)

/** Match page and results page; both routes carry `eventId`. */
@HiltViewModel
class MatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roundRepository: RoundRepository,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val local = MutableStateFlow(MatchUiState(loading = false))

    val uiState: StateFlow<MatchUiState> = combine(roundRepository.observeDetail(eventId), local) { detail, local ->
        if (detail == null) MatchUiState(loading = false, notFound = true) else detail.toUiState(local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchUiState())

    fun finish() {
        if (local.value.finishing) return
        local.update { it.copy(finishing = true) }
        viewModelScope.launch {
            val blocker = roundRepository.finishMatch(eventId)
            local.update { it.copy(finishing = false, justFinished = blocker == null) }
        }
    }
}

private fun MatchDetail.toUiState(local: MatchUiState): MatchUiState {
    val board = ScoringEngine.leaderboard(subjectRefs, roundScores, event.scoringDirection, RankBasis.OFFICIAL)
    val finished = event.status == EventStatus.COMPLETED
    val active = rounds.firstOrNull { it.status == RoundStatus.ACTIVE }
    val pending = rounds.firstOrNull { it.status == RoundStatus.PENDING }
    return MatchUiState(
        loading = false,
        title = event.title,
        gameIcon = game?.icon,
        gameTitle = game?.title.orEmpty(),
        finished = finished,
        totalRounds = event.totalRounds,
        completedRounds = rounds.count { it.status == RoundStatus.COMPLETED },
        subjectNames = subjects.map { it.name },
        standings = board.entries,
        rounds = rounds.map { round ->
            val scores = scoresOf(round.id)
            RoundRow(round.roundNumber, round.status, round.elapsedMillis, subjects.map { scores[it.id] })
        },
        series = ScoringEngine.cumulativeSeries(subjectRefs, roundScores),
        nextRound = when {
            finished -> null
            active != null -> NextRound.Continue(active.roundNumber)
            pending != null -> NextRound.Start(pending.roundNumber)
            else -> null
        },
        finishBlocker = if (finished) null else RoundRules.finishBlocker(roundStates),
        finishing = local.finishing,
        justFinished = local.justFinished,
    )
}
