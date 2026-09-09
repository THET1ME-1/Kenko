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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.heatColor
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.exercises.localizedExerciseName
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.formatDate

/**
 * One exercise inside the period: the record line, how the top weight moved, and every set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseStats(
    viewModel: ExerciseStatsViewModel,
    onBackPress: () -> Unit,
    onMuscleClick: (MuscleGroups) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val load = state.load

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = {
                    Text(
                        text = load?.exercise?.displayName() ?: localizedExerciseName(state.name),
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "${formatDate(viewModel.period.from)} — ${formatDate(viewModel.period.to)}",
                    style = MaterialTheme.typography.labelMedium.numbers(),
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (load != null) {
                item {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = formatVolume(load.volume),
                            caption = stringResource(R.string.label_total_volume),
                            accent = true,
                        )
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = load.sets.toString(),
                            caption = stringResource(R.string.label_sets_count),
                        )
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = load.reps.toString(),
                            caption = stringResource(R.string.label_reps_count),
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = "${load.topReps} × ${formatWeight(load.topWeight)}",
                            caption = stringResource(R.string.label_best_set),
                        )
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = formatWeight(load.estimatedMax),
                            caption = stringResource(R.string.label_estimated_max),
                        )
                    }
                }

                item { MuscleChips(load.exercise.target, load.exercise.secondaryTargets, onMuscleClick) }
            }

            if (state.bestPerSession.size > 1) {
                item { ProgressChart(points = state.bestPerSession.map { it.second }) }
            }

            if (state.rows.isNotEmpty()) {
                item { StatsSection(title = stringResource(R.string.label_all_sets)) }
                items(state.rows.size) { index ->
                    val row = state.rows[index]
                    SetLine(
                        date = formatDate(row.date),
                        title = setTypeCaption(row),
                        weight = "${formatWeight(row.set.weight)} ${stringResource(R.string.label_kg)}",
                        reps = row.set.repsOrDuration,
                    )
                }
            } else {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        text = stringResource(R.string.label_no_sessions_in_period),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun setTypeCaption(row: SetRow): String = when {
    row.set.parentSetId != null ->
        stringResource(R.string.label_drop_number, row.set.dropIndex)
    else -> stringResource(R.string.label_set_type_standard)
}

/**
 * The muscles this exercise works, each one a way back to its own screen.
 */
@Composable
private fun MuscleChips(
    target: MuscleGroups,
    assisting: List<MuscleGroups>,
    onMuscleClick: (MuscleGroups) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MuscleChip(muscle = target, primary = true, onClick = { onMuscleClick(target) })
        assisting.take(3).forEach { muscle ->
            MuscleChip(muscle = muscle, primary = false, onClick = { onMuscleClick(muscle) })
        }
    }
}

@Composable
private fun MuscleChip(
    muscle: MuscleGroups,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(muscle.stringRes),
        style = MaterialTheme.typography.labelLarge,
        color = if (primary) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (primary) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/**
 * Top weight of every session in the period, oldest on the left.
 */
@Composable
private fun ProgressChart(points: List<Float>, modifier: Modifier = Modifier) {
    val top = points.maxOrNull() ?: 0F
    val bottom = points.minOrNull() ?: 0F
    val span = (top - bottom).takeIf { it > 0F } ?: 1F
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
            text = stringResource(R.string.label_progress_by_session).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { value ->
                val fraction = ((value - bottom) / span).coerceIn(0F, 1F)
                Box(
                    modifier = Modifier
                        .weight(1F)
                        .height((16 + 70 * fraction).dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(heatColor(fraction)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatWeight(bottom),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.weight(1F))
            Text(
                text = formatWeight(top),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
