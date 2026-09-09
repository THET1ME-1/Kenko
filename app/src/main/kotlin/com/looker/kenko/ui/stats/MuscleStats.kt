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
import com.looker.kenko.ui.components.BodyHeatMap
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.formatDate

/**
 * One muscle inside the period: how much it took, from which exercises, set by set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleStats(
    viewModel: MuscleStatsViewModel,
    onBackPress: () -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val load = state.load

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(state.muscle.stringRes)) },
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

            item {
                MuscleOnBody(muscle = state.muscle)
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
                            value = load.directSets.toString(),
                            caption = stringResource(R.string.label_sets_own),
                        )
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = load.assistSets.toString(),
                            caption = stringResource(R.string.label_assist_set),
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
                            value = load.reps.toString(),
                            caption = stringResource(R.string.label_reps_count),
                        )
                        DetailTile(
                            modifier = Modifier.weight(1F),
                            value = load.exercises.toString(),
                            caption = stringResource(R.string.label_exercises_of_period),
                        )
                    }
                }
            }

            if (state.exercises.isNotEmpty()) {
                item { StatsSection(title = stringResource(R.string.label_muscle_exercises)) }
                items(state.exercises, key = { it.exercise.name }) { exerciseLoad ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onExerciseClick(exerciseLoad.exercise.name) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1F)) {
                            Text(
                                text = exerciseLoad.exercise.displayName(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.label_exercise_stats_line,
                                    exerciseLoad.sets,
                                    exerciseLoad.reps,
                                    formatVolume(exerciseLoad.topWeight),
                                ),
                                style = MaterialTheme.typography.labelSmall.numbers(),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Text(
                            text = formatVolume(exerciseLoad.volume),
                            style = MaterialTheme.typography.titleMedium.numbers(),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (state.rows.isNotEmpty()) {
                item { StatsSection(title = stringResource(R.string.label_sets_of_muscle)) }
                items(state.rows.size) { index ->
                    val row = state.rows[index]
                    SetLine(
                        date = formatDate(row.date),
                        title = row.set.exercise.displayName(),
                        weight = "${formatWeight(row.set.weight)} ${stringResource(R.string.label_kg)}",
                        reps = row.set.repsOrDuration,
                        assisting = row.isAssisting,
                    )
                }
            }

            if (state.rows.isEmpty()) {
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

/**
 * The muscle alone on the body, so there is no doubt which one the numbers are about.
 */
@Composable
private fun MuscleOnBody(muscle: MuscleGroups, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 16.dp, horizontal = 16.dp),
    ) {
        BodyHeatMap(
            load = mapOf(muscle to 1F),
            height = 280.dp,
            selected = muscle,
        )
    }
}

@Composable
internal fun StatsSection(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(top = 8.dp),
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun DetailTile(
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
 * One written set as the report will print it: day, exercise, weight, reps.
 */
@Composable
internal fun SetLine(
    date: String,
    title: String,
    weight: String,
    reps: Int,
    modifier: Modifier = Modifier,
    assisting: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall.numbers(),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (assisting) {
                Text(
                    text = stringResource(R.string.label_assist_set),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$reps × $weight",
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun ChartSpacer() {
    Spacer(Modifier.height(4.dp))
}
