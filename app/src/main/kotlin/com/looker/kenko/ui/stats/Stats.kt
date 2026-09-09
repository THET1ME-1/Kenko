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

package com.looker.kenko.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.ExerciseLoad
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.MuscleLoad
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.ui.components.BodyHeatMap
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.MuscleIcon
import com.looker.kenko.ui.components.heatColor
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.formatDate

/**
 * Load of a period: the body first, the numbers under it, then the muscles and the exercises
 * that made those numbers. Everything on the screen opens something.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stats(
    viewModel: StatsViewModel,
    onBackPress: () -> Unit,
    onMuscleClick: (MuscleGroups, StatsPeriod) -> Unit,
    onExerciseClick: (String, StatsPeriod) -> Unit,
    onReportClick: (StatsPeriod) -> Unit,
) {
    val summary by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.title_stats)) },
                actions = {
                    IconButton(onClick = { onReportClick(period) }) {
                        Icon(
                            painter = KenkoIcons.Save,
                            contentDescription = stringResource(R.string.title_report),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RangePicker(
                    range = period.range,
                    onRangeClick = viewModel::setRange,
                )
            }

            item {
                PeriodBar(
                    period = period,
                    canGoForward = canGoForward,
                    onShift = viewModel::shift,
                )
            }

            item {
                BodyCard(
                    summary = summary,
                    onMuscleClick = { onMuscleClick(it, period) },
                )
            }

            item { SummaryGrid(summary = summary) }

            if (summary.days.size > 1) {
                item { VolumeChart(summary = summary) }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.label_muscle_load),
                    hint = stringResource(R.string.label_assist_hint),
                )
            }

            items(
                summary.muscles.filter { it.isTouched },
                key = { it.muscle.name },
            ) { load ->
                MuscleRow(
                    load = load,
                    share = summary.share(load),
                    intensity = summary.intensityOf(load.muscle),
                    onClick = { onMuscleClick(load.muscle, period) },
                )
            }

            if (summary.exerciseLoads.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.label_exercises_of_period),
                        hint = pluralStringResource(
                            R.plurals.plural_exercises,
                            summary.exercises,
                            summary.exercises,
                        ),
                    )
                }
                items(summary.exerciseLoads, key = { it.exercise.name }) { load ->
                    ExerciseRow(
                        load = load,
                        maxVolume = summary.exerciseLoads.first().volume,
                        onClick = { onExerciseClick(load.exercise.name, period) },
                    )
                }
            }

            if (summary.isEmpty) {
                item { EmptyPeriod() }
            }
        }
    }
}

/**
 * Every muscle against the busiest one of the period, `0..1`.
 */
fun StatsSummary.intensityOf(muscle: MuscleGroups): Float {
    val top = muscles.maxOfOrNull { it.volume } ?: 0F
    if (top <= 0F) return 0F
    val own = muscles.firstOrNull { it.muscle == muscle }?.volume ?: 0F
    return (own / top).coerceIn(0F, 1F)
}

@Composable
private fun RangePicker(
    range: StatsRange,
    onRangeClick: (StatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            StatsRange.Day to R.string.label_range_day,
            StatsRange.Week to R.string.label_range_week,
            StatsRange.Month to R.string.label_range_month,
            StatsRange.Year to R.string.label_range_year,
        ).forEach { (item, label) ->
            RangePill(
                label = stringResource(label),
                selected = range == item,
                onClick = { onRangeClick(item) },
            )
        }
    }
}

@Composable
private fun RangePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun PeriodBar(
    period: StatsPeriod,
    canGoForward: Boolean,
    onShift: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(
                painter = KenkoIcons.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.label_period_previous),
            )
        }
        Text(
            modifier = Modifier.weight(1F),
            text = periodLabel(period),
            style = MaterialTheme.typography.titleMedium.numbers(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = { onShift(1) },
            enabled = canGoForward,
        ) {
            Icon(
                painter = KenkoIcons.KeyboardArrowRight,
                contentDescription = stringResource(R.string.label_period_next),
            )
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod): String = if (period.from == period.to) {
    formatDate(period.from)
} else {
    "${formatDate(period.from)} — ${formatDate(period.to)}"
}

@Composable
private fun BodyCard(
    summary: StatsSummary,
    onMuscleClick: (MuscleGroups) -> Unit,
    modifier: Modifier = Modifier,
) {
    val load = MuscleGroups.entries.associateWith { summary.intensityOf(it) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        BodyHeatMap(
            load = load,
            onMuscleClick = onMuscleClick,
        )
        Spacer(Modifier.height(14.dp))
        HeatLegend()
        summary.heaviestMuscle?.let { top ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.label_heaviest_muscle,
                    stringResource(top.muscle.stringRes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeatLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_heat_cold),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.extraLarge),
        ) {
            listOf(0F, 0.25F, 0.5F, 0.75F, 1F).forEach { step ->
                Box(
                    modifier = Modifier
                        .weight(1F)
                        .height(10.dp)
                        .background(heatColor(step)),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.label_heat_hot),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SummaryGrid(summary: StatsSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                modifier = Modifier.weight(1F),
                value = formatVolume(summary.volume),
                caption = stringResource(R.string.label_total_volume),
                accent = true,
            )
            StatTile(
                modifier = Modifier.weight(1F),
                value = summary.sessions.toString(),
                caption = stringResource(R.string.label_sessions_count),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                modifier = Modifier.weight(1F),
                value = summary.sets.toString(),
                caption = stringResource(R.string.label_sets_count),
            )
            StatTile(
                modifier = Modifier.weight(1F),
                value = summary.reps.toString(),
                caption = stringResource(R.string.label_reps_count),
            )
            StatTile(
                modifier = Modifier.weight(1F),
                value = summary.repsPerSet.toString(),
                caption = stringResource(R.string.label_reps_per_set),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                modifier = Modifier.weight(1F),
                value = formatVolume(summary.volumePerSession),
                caption = stringResource(R.string.label_volume_per_session),
            )
            StatTile(
                modifier = Modifier.weight(1F),
                value = summary.setsPerSession.toString(),
                caption = stringResource(R.string.label_sets_per_session),
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (accent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.numbers(),
            color = if (accent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
    }
}

/**
 * Volume day by day. Bars keep their place even on rest days, so a week reads as a week.
 */
@Composable
private fun VolumeChart(summary: StatsSummary, modifier: Modifier = Modifier) {
    val top = summary.days.maxOfOrNull { it.volume } ?: 0F
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraLarge,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.label_by_day).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            summary.days.forEach { day ->
                val fraction = if (top <= 0F) 0F else (day.volume / top).coerceIn(0F, 1F)
                Box(
                    modifier = Modifier
                        .weight(1F)
                        .height((6 + 90 * fraction).dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (day.sets == 0) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                heatColor(fraction)
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDate(summary.days.first().date),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.weight(1F))
            Text(
                text = formatDate(summary.days.last().date),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1F))
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun MuscleRow(
    load: MuscleLoad,
    share: Float,
    intensity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MuscleIcon(
            muscle = load.muscle,
            height = 54.dp,
            tint = heatColor(intensity),
            body = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(load.muscle.stringRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (load.assistSets == 0) {
                    pluralStringResource(
                        R.plurals.plural_sets,
                        load.directSets,
                        load.directSets,
                    )
                } else {
                    stringResource(
                        R.string.label_muscle_sets_line,
                        load.directSets,
                        load.assistSets,
                    )
                },
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(6.dp))
            ShareBar(fraction = share, color = heatColor(intensity))
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatVolume(load.volume),
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${(share * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.02F, 1F))
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(color),
        )
    }
}

@Composable
private fun ExerciseRow(
    load: ExerciseLoad,
    maxVolume: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (maxVolume <= 0F) 0F else load.volume / maxVolume
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(
                width = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = load.exercise.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.label_exercise_stats_line,
                    load.sets,
                    load.reps,
                    formatVolume(load.topWeight),
                ),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(6.dp))
            ShareBar(fraction = fraction, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatVolume(load.volume),
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyPeriod(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        text = stringResource(R.string.label_no_sessions_in_period),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
    )
}

/**
 * Tonnage reads better in tonnes once it grows past a thousand kilograms.
 */
@Composable
fun formatVolume(volume: Float): String = if (volume >= 1000F) {
    stringResource(R.string.label_tonnes_value, String.format("%.1f", volume / 1000F))
} else {
    stringResource(R.string.label_kg_value, volume.toInt().toString())
}
