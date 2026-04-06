/*
 * Copyright (C) 2026 LooKeR & Contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private val CellSize: Dp = 12.dp
private val CellGap: Dp = 2.dp
private val CellShape = RoundedCornerShape(3.dp)
private val DayLabelWidth: Dp = 10.dp

@Immutable
private data class FrequencyCell(
    val epochDay: Int,
    val normalizedCount: Float,  // 0..1, 0 = no session
    val hasSession: Boolean,
    val isFuture: Boolean,
)

@Composable
fun FrequencyGraph(
    activity: Map<Int, Int>,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val weeks = remember(activity) { buildFrequencyGrid(activity, today) }

    if (weeks.isEmpty()) {
        empty()
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            DayLabels()
            Spacer(Modifier.width(4.dp))
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CellGap),
            ) {
                items(weeks) { week ->
                    WeekColumn(week)
                }
            }
        }
    }

}

@Composable
private fun DayLabels() {
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.outline
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(
        verticalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier.size(width = DayLabelWidth, height = CellSize),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekColumn(week: List<FrequencyCell>) {
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val sessionColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
        week.forEach { cell ->
            val color = if (!cell.hasSession || cell.isFuture) {
                emptyColor
            } else {
                sessionColor.copy(alpha = 0.2f + 0.8f * cell.normalizedCount)
            }
            Box(
                modifier = Modifier
                    .size(CellSize)
                    .clip(CellShape)
                    .background(color),
            )
        }
    }
}

private fun buildFrequencyGrid(
    activity: Map<Int, Int>,
    today: LocalDate,
): List<List<FrequencyCell>> {
    if (activity.isEmpty()) return emptyList()

    val maxCount = activity.values.max().coerceAtLeast(1)

    val todayEpoch = today.toEpochDays().toInt()
    val currentMondayEpoch = todayEpoch - today.dayOfWeek.ordinal

    val oldestEpoch = activity.keys.min()
    val oldestDate = LocalDate.fromEpochDays(oldestEpoch)
    val oldestMondayEpoch = oldestEpoch - oldestDate.dayOfWeek.ordinal

    val weekCount = ((currentMondayEpoch - oldestMondayEpoch) / 7 + 1).coerceAtLeast(1)

    return List(weekCount) { weekIndex ->
        val weekMondayEpoch = currentMondayEpoch - weekIndex * 7
        List(7) { dayIndex ->
            val dayEpoch = weekMondayEpoch + dayIndex
            val count = activity[dayEpoch] ?: 0
            FrequencyCell(
                epochDay = dayEpoch,
                normalizedCount = if (count > 0) count.toFloat() / maxCount else 0f,
                hasSession = activity.containsKey(dayEpoch),
                isFuture = dayEpoch > todayEpoch,
            )
        }
    }
}
