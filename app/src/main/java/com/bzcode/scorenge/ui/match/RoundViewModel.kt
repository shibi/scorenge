package com.bzcode.scorenge.ui.match

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.repository.MatchDetail
import com.bzcode.scorenge.data.repository.RoundRepository
import com.bzcode.scorenge.di.ApplicationScope
import com.bzcode.scorenge.domain.match.FinishingOrder
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoreEntry
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.domain.scoring.RankBasis
import com.bzcode.scorenge.domain.scoring.ScoringEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScoreField(
    val subjectId: String,
    val name: String,
    /** What the field shows; may be a half-typed value such as "-". */
    val text: String,
    /** Running total before this round (completed rounds only). */
    val totalBefore: Long,
    /** Finishing-order matches: this round's place, or null if not placed yet. */
    val place: Int? = null,
    /** Finishing-order matches: points this place earns. */
    val points: Long? = null,
)

data class RoundUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val matchTitle: String = "",
    val gameIcon: String? = null,
    val roundNumber: Int = 0,
    val totalRounds: Int? = null,
    val status: RoundStatus = RoundStatus.PENDING,
    val startedAt: Long? = null,
    /** Saved elapsed time; while [openedAt] is set the clock is running from there. */
    val elapsedMillis: Long = 0L,
    val openedAt: Long? = null,
    val fields: List<ScoreField> = emptyList(),
    /** Players or teams are tapped in finishing order instead of typing scores. */
    val finishingOrder: Boolean = false,
    /** Provisional standings: completed rounds plus this round's scores so far. */
    val leading: List<LeaderboardEntry> = emptyList(),
    val missingCount: Int = 0,
    val completing: Boolean = false,
    /** Set when this round was completed here; the screen then goes back to the match. */
    val justCompleted: Boolean = false,
) {
    val editable: Boolean get() = status == RoundStatus.ACTIVE
    val canComplete: Boolean get() = editable && missingCount == 0 && !completing
    val leaders: List<LeaderboardEntry> get() = leading.filter { it.rank == 1 }
}

private data class LocalState(
    val drafts: Map<String, String> = emptyMap(),
    val completing: Boolean = false,
    val justCompleted: Boolean = false,
)

@HiltViewModel
class RoundViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roundRepository: RoundRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val roundNumber: Int = checkNotNull(savedStateHandle["roundNumber"])
    private val local = MutableStateFlow(LocalState())
    private var roundId: String? = null

    val uiState: StateFlow<RoundUiState> = combine(roundRepository.observeDetail(eventId), local) { detail, local ->
        detail?.toRoundState(local) ?: RoundUiState(loading = false, notFound = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoundUiState())

    /** The page is showing: start or resume the round's clock. Runs past the page's lifetime. */
    fun onPageVisible() {
        appScope.launch { roundRepository.openRound(eventId, roundNumber) }
    }

    /** The page is gone or the app went to the background: pause the clock and save the time. */
    fun onPageHidden() {
        appScope.launch { roundRepository.closeRound(eventId, roundNumber) }
    }

    /** Keeps what the user typed, and saves it once it is a whole number (blank clears the score). */
    fun onScoreChange(subjectId: String, input: String) {
        val text = sanitize(input) ?: return
        local.update { it.copy(drafts = it.drafts + (subjectId to text)) }
        val id = roundId ?: return
        val value: Long? = when {
            text.isEmpty() -> null
            text == "-" -> return
            else -> text.toLongOrNull() ?: return
        }
        viewModelScope.launch { roundRepository.setScore(id, subjectId, value) }
    }

    /** Finishing order: places the next finisher, or takes a placed one off (later places move up). */
    fun onTapFinisher(subjectId: String) {
        val id = roundId ?: return
        val placed = uiState.value.fields.filter { it.place != null }.sortedBy { it.place }.map { it.subjectId }
        val order = if (subjectId in placed) placed - subjectId else placed + subjectId
        viewModelScope.launch { roundRepository.setFinishingOrder(id, order) }
    }

    /** Finishing order: clears every place in this round. */
    fun clearFinishingOrder() {
        val id = roundId ?: return
        viewModelScope.launch { roundRepository.setFinishingOrder(id, emptyList()) }
    }

    fun completeRound() {
        val id = roundId ?: return
        if (!uiState.value.canComplete) return
        local.update { it.copy(completing = true) }
        viewModelScope.launch {
            val done = roundRepository.completeRound(id)
            local.update { it.copy(completing = false, justCompleted = done) }
        }
    }

    private fun MatchDetail.toRoundState(local: LocalState): RoundUiState? {
        val round = rounds.firstOrNull { it.roundNumber == roundNumber } ?: return null
        val finishingOrder = event.scoreEntry == ScoreEntry.FINISHING_ORDER
        roundId = round.id
        val saved = scoresOf(round.id)
        val earlier = roundScores.filter { it.roundNumber != roundNumber }
        val officialBefore = ScoringEngine.leaderboard(subjectRefs, earlier, event.scoringDirection, RankBasis.OFFICIAL)
            .entries.associate { it.subjectId to (it.officialTotal ?: 0L) }
        val provisional = ScoringEngine.leaderboard(subjectRefs, roundScores, event.scoringDirection, RankBasis.PROVISIONAL)
        return RoundUiState(
            loading = false,
            matchTitle = event.title,
            gameIcon = game?.icon,
            roundNumber = round.roundNumber,
            totalRounds = event.totalRounds,
            status = if (event.status == EventStatus.COMPLETED && round.status != RoundStatus.COMPLETED) RoundStatus.COMPLETED else round.status,
            startedAt = round.startedAt,
            elapsedMillis = round.elapsedMillis,
            openedAt = round.openedAt,
            fields = subjects.map { s ->
                ScoreField(
                    subjectId = s.id,
                    name = s.name,
                    text = local.drafts[s.id] ?: saved[s.id]?.toString().orEmpty(),
                    totalBefore = officialBefore[s.id] ?: 0L,
                    place = if (finishingOrder) FinishingOrder.placeOf(saved[s.id], subjects.size) else null,
                    points = saved[s.id].takeIf { finishingOrder },
                )
            },
            finishingOrder = finishingOrder,
            leading = if (round.status == RoundStatus.COMPLETED) emptyList() else provisional.entries,
            missingCount = subjects.count { saved[it.id] == null },
            completing = local.completing,
            justCompleted = local.justCompleted,
        )
    }

    private companion object {
        const val MAX_DIGITS = 9

        /** Keeps an optional leading minus and up to [MAX_DIGITS] digits; null rejects the input. */
        fun sanitize(input: String): String? {
            val trimmed = input.trim()
            val digits = trimmed.removePrefix("-")
            if (digits.length > MAX_DIGITS || !digits.all(Char::isDigit)) return null
            return trimmed
        }
    }
}
