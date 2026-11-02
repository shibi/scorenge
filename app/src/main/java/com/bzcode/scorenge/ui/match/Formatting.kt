package com.bzcode.scorenge.ui.match

import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** "4:07" or "1:02:09". */
internal fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

private val TimeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** Local clock time, such as "18:42". */
internal fun formatClockTime(epochMillis: Long): String =
    TimeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** "Round 2 of 5" or "Round 2". */
internal fun roundLabel(number: Int, total: Int?): String =
    if (total != null) "Round $number of $total" else "Round $number"

/** "Maya", "Maya & Leo", "Maya, Leo & Ana". */
internal fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> names.dropLast(1).joinToString(", ") + " & " + names.last()
}

internal fun LeaderboardEntry.totalText(provisional: Boolean = false): String =
    (if (provisional) projectedTotal else officialTotal)?.toString() ?: "–"
