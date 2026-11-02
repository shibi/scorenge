package com.bzcode.scorenge.ui.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bzcode.scorenge.domain.scoring.CumulativeSeries
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.ui.theme.ScorengeTheme

/** Series colours, readable on the light and dark backgrounds; repeats after eight. */
private val SeriesColors = listOf(
    Color(0xFFE4572E), // orange
    Color(0xFF2F80C8), // blue
    Color(0xFF2E9E5B), // green
    Color(0xFF9B59B6), // purple
    Color(0xFFD9A400), // mustard
    Color(0xFF00A3A3), // teal
    Color(0xFFD6457A), // pink
    Color(0xFF8D6E63), // brown
)

internal fun seriesColor(index: Int): Color = SeriesColors[index % SeriesColors.size]

/**
 * Horizontal bars of each total, best first. Bars start from the lowest of zero and the smallest
 * total, so negative totals still read correctly.
 */
@Composable
fun TotalsBarChart(entries: List<LeaderboardEntry>, modifier: Modifier = Modifier) {
    val totals = entries.map { it.officialTotal ?: 0L }
    val floor = minOf(0L, totals.minOrNull() ?: 0L)
    val span = ((totals.maxOrNull() ?: 0L) - floor).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            val total = totals[index]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Text(
                    entry.rank?.toString() ?: "–",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 24.dp),
                )
                Text(
                    entry.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(88.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(20.dp),
                ) {
                    val fraction = ((total - floor).toFloat() / span).coerceIn(0.02f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(20.dp)
                            .background(
                                if (entry.rank == 1) ScorengeTheme.colors.brand else ScorengeTheme.colors.brand.copy(alpha = 0.45f),
                                RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                            ),
                    )
                }
                Text(
                    entry.totalText(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
        }
    }
}

/** Running totals after each completed round, one line per player or team, starting from 0. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreLineChart(series: List<CumulativeSeries>, modifier: Modifier = Modifier) {
    val rounds = series.maxOfOrNull { it.totals.size } ?: 0
    if (rounds == 0) return
    val values = series.flatMap { it.totals } + 0L
    val min = values.min()
    val max = values.max()
    val span = (max - min).coerceAtLeast(1L).toFloat()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val description = "Score after each round. " + series.joinToString(". ") { s ->
        "${s.subjectName}: ${s.totals.joinToString(", ")}"
    }

    Column(modifier) {
        Row {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .height(ChartHeight)
                    .padding(end = 6.dp),
            ) {
                AxisLabel(max.toString())
                AxisLabel(min.toString())
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(ChartHeight)
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                fun x(i: Int) = if (rounds == 0) 0f else size.width * i / rounds
                fun y(v: Long) = size.height - (v - min) / span * size.height

                // Frame and the zero line.
                drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                if (min < 0 && max > 0) {
                    drawLine(
                        gridColor,
                        Offset(0f, y(0)),
                        Offset(size.width, y(0)),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }

                series.forEachIndexed { index, s ->
                    val color = seriesColor(index)
                    val points = listOf(Offset(x(0), y(0))) + s.totals.mapIndexed { i, v -> Offset(x(i + 1), y(v)) }
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
                    points.drop(1).forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
                }
            }
        }
        Row(Modifier.padding(start = 32.dp, top = 4.dp)) {
            AxisLabel("Start", Modifier.weight(1f))
            AxisLabel("Round $rounds", textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            series.forEachIndexed { index, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(seriesColor(index), CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(s.subjectName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private val ChartHeight: Dp = 180.dp

@Composable
private fun AxisLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** First, second and third place: second on the left, first raised in the middle, third on the right. */
@Composable
fun Podium(entries: List<LeaderboardEntry>, modifier: Modifier = Modifier) {
    val top = entries.filter { it.rank != null }.take(3)
    if (top.isEmpty()) return
    val order = when (top.size) {
        1 -> listOf(top[0])
        2 -> listOf(top[1], top[0])
        else -> listOf(top[1], top[0], top[2])
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.fillMaxWidth(),
    ) {
        order.forEach { entry ->
            val first = entry.rank == 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    entry.subjectName,
                    style = if (first) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (first) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    entry.totalText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = if (first) ScorengeTheme.colors.brand else ScorengeTheme.colors.cream,
                    contentColor = if (first) Color.White else ScorengeTheme.colors.onCream,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(podiumHeight(entry.rank ?: 3)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            ordinal(entry.rank ?: 3),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun podiumHeight(rank: Int): Dp = when (rank) {
    1 -> 120.dp
    2 -> 88.dp
    else -> 64.dp
}

internal fun ordinal(rank: Int): String = when {
    rank % 100 in 11..13 -> "${rank}th"
    rank % 10 == 1 -> "${rank}st"
    rank % 10 == 2 -> "${rank}nd"
    rank % 10 == 3 -> "${rank}rd"
    else -> "${rank}th"
}
