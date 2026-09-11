/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.ui.planEdit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.model.DROP_PERCENT_STEP
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.MAX_DROP_COUNT
import com.looker.kenko.data.model.MAX_DROP_PERCENT
import com.looker.kenko.data.model.MIN_DROP_PERCENT
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.buildDropChain
import com.looker.kenko.data.model.chainLabel
import com.looker.kenko.data.model.formatSeconds
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.ExercisePhoto
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.OnSurfaceVariantBorder
import com.looker.kenko.ui.components.WeightCalculator
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.exercises.string
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.numbers

/**
 * Everything the plan says about one exercise, carried in one piece.
 */
data class PlanTargets(
    val gripId: Int? = null,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val dropCount: Int,
    val dropPercent: Int,
    val barWeight: Float,
    val leftWeight: Float,
    val rightWeight: Float,
)

/**
 * Rest lengths a lifter actually uses, offered as one tap instead of eight.
 */
private val restPresets = listOf(45, 60, 90, 120, 180)

private const val REST_STEP = 15
private const val MAX_REST = 600
private const val MAX_SETS = 20
private const val MAX_REPS = 100

/**
 * How an exercise is set up inside a plan: sets, reps, rest and drops, on one screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(
    item: PlanItem,
    onSave: (targets: PlanTargets) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    grips: List<Grip> = emptyList(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_edit_targets)) },
            )
        },
    ) { innerPadding ->
        TargetsForm(
            item = item,
            grips = grips,
            onSave = onSave,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The same goal controls inside a sheet: the session edits the goal of the day without leaving
 * the list it is writing into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsSheet(
    item: PlanItem,
    onSave: (targets: PlanTargets) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    grips: List<Grip> = emptyList(),
    title: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        TargetsForm(
            item = item,
            grips = grips,
            onSave = onSave,
        )
    }
}

/**
 * Sets, reps, rest, weight and drops of one exercise.
 */
@Composable
private fun TargetsForm(
    item: PlanItem,
    onSave: (targets: PlanTargets) -> Unit,
    modifier: Modifier = Modifier,
    grips: List<Grip> = emptyList(),
) {
    var sets by remember(item.id) { mutableIntStateOf(item.targetSets) }
    var repsMin by remember(item.id) { mutableIntStateOf(item.targetReps) }
    var repsMax by remember(item.id) { mutableIntStateOf(maxOf(item.targetRepsMax, item.targetReps)) }
    var rest by remember(item.id) { mutableIntStateOf(item.restSeconds) }
    var drops by remember(item.id) { mutableIntStateOf(item.dropCount) }
    var dropPercent by remember(item.id) { mutableIntStateOf(item.dropPercent) }
    var bar by remember(item.id) { mutableFloatStateOf(item.barWeight) }
    var left by remember(item.id) { mutableFloatStateOf(item.leftWeight) }
    var right by remember(item.id) { mutableFloatStateOf(item.rightWeight) }
    var gripId by remember(item.id) { mutableStateOf(item.gripId) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExercisePhoto(exercise = item.exercise, size = 72.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        text = item.exercise.displayName(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(item.exercise.target.string),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (grips.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.label_grip_default).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestPreset(
                        label = stringResource(R.string.label_no_grip),
                        selected = gripId == null,
                        onClick = { gripId = null },
                    )
                    grips.forEach { grip ->
                        RestPreset(
                            label = grip.name,
                            selected = gripId == grip.id,
                            onClick = { gripId = grip.id },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            CounterCard(
                caption = stringResource(R.string.label_sets),
                value = normalizeInt(sets),
                onDecrease = { sets = (sets - 1).coerceAtLeast(1) },
                onIncrease = { sets = (sets + 1).coerceAtMost(MAX_SETS) },
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CounterCard(
                    modifier = Modifier.weight(1F),
                    caption = stringResource(R.string.label_reps_from),
                    value = normalizeInt(repsMin),
                    onDecrease = {
                        repsMin = (repsMin - 1).coerceAtLeast(1)
                        repsMax = maxOf(repsMax, repsMin)
                    },
                    onIncrease = {
                        repsMin = (repsMin + 1).coerceAtMost(MAX_REPS)
                        repsMax = maxOf(repsMax, repsMin)
                    },
                )
                CounterCard(
                    modifier = Modifier.weight(1F),
                    caption = stringResource(R.string.label_reps_to),
                    value = normalizeInt(repsMax),
                    onDecrease = { repsMax = (repsMax - 1).coerceAtLeast(repsMin) },
                    onIncrease = { repsMax = (repsMax + 1).coerceAtMost(MAX_REPS) },
                )
            }

            Spacer(Modifier.height(16.dp))

            CounterCard(
                caption = stringResource(R.string.label_rest),
                value = formatSeconds(rest),
                onDecrease = { rest = (rest - REST_STEP).coerceAtLeast(0) },
                onIncrease = { rest = (rest + REST_STEP).coerceAtMost(MAX_REST) },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                restPresets.forEach { preset ->
                    RestPreset(
                        seconds = preset,
                        selected = rest == preset,
                        onClick = { rest = preset },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.label_weight_default).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))
            WeightCalculator(
                bar = bar,
                left = left,
                right = right,
                onBarChange = { bar = it },
                onLeftChange = { left = it },
                onRightChange = { right = it },
            )

            Spacer(Modifier.height(24.dp))

            DropSection(
                drops = drops,
                percent = dropPercent,
                onDropsChange = { drops = it.coerceIn(0, MAX_DROP_COUNT) },
                onPercentChange = {
                    dropPercent = it.coerceIn(MIN_DROP_PERCENT, MAX_DROP_PERCENT)
                },
            )

            Spacer(Modifier.height(24.dp))

            Summary(sets = sets, reps = repsMin, rest = rest, drops = drops)

            Spacer(Modifier.height(24.dp))

            KenkoButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    onSave(
                        PlanTargets(
                            gripId = gripId,
                            sets = sets,
                            repsMin = repsMin,
                            repsMax = repsMax,
                            restSeconds = rest,
                            dropCount = drops,
                            dropPercent = dropPercent,
                            barWeight = bar,
                            leftWeight = left,
                            rightWeight = right,
                        ),
                    )
                },
                label = { Text(text = stringResource(R.string.label_save)) },
                icon = {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = KenkoIcons.Save,
                        contentDescription = null,
                    )
                },
            )

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * One number, big, with a key on each side.
 */
@Composable
private fun CounterCard(
    caption: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(OnSurfaceVariantBorder, MaterialTheme.shapes.extraLarge)
            .padding(vertical = 14.dp, horizontal = 16.dp),
    ) {
        Text(
            text = caption.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CounterKey(label = "−", onClick = onDecrease)
            Text(
                modifier = Modifier.weight(1F),
                text = value,
                style = MaterialTheme.typography.displaySmall.numbers(),
                textAlign = TextAlign.Center,
            )
            CounterKey(label = "+", onClick = onIncrease)
        }
    }
}

@Composable
private fun CounterKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RestPreset(
    selected: Boolean,
    onClick: () -> Unit,
    seconds: Int? = null,
    label: String? = null,
) {
    Text(
        text = label ?: formatSeconds(seconds ?: 0),
        style = MaterialTheme.typography.labelLarge.numbers(),
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * Drops of the plan, with the ladder of weights they will produce.
 */
@Composable
private fun DropSection(
    drops: Int,
    percent: Int,
    onDropsChange: (Int) -> Unit,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = KenkoBorderWidth,
                color = if (drops > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.extraLarge,
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.label_drop_in_plan).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CounterKey(label = "−", onClick = { onDropsChange(drops - 1) })
            Text(
                modifier = Modifier.weight(1F),
                text = if (drops == 0) {
                    stringResource(R.string.label_no_drops)
                } else {
                    stringResource(R.string.label_drop_short, drops)
                },
                style = MaterialTheme.typography.displaySmall.numbers(),
                textAlign = TextAlign.Center,
            )
            CounterKey(label = "+", onClick = { onDropsChange(drops + 1) })
        }
        if (drops > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = stringResource(R.string.label_drop_step),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CounterKey(label = "−", onClick = { onPercentChange(percent - DROP_PERCENT_STEP) })
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = "−$percent%",
                    style = MaterialTheme.typography.titleMedium.numbers(),
                )
                CounterKey(label = "+", onClick = { onPercentChange(percent + DROP_PERCENT_STEP) })
            }
            Spacer(Modifier.height(10.dp))
            val ladder = remember(drops, percent) {
                buildDropChain(base = 100F, drops = drops, reps = 10, percent = percent)
            }
            Text(
                text = stringResource(R.string.label_drop_ladder, ladder.chainLabel()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * What the choices add up to for the day.
 */
@Composable
private fun Summary(
    sets: Int,
    reps: Int,
    rest: Int,
    drops: Int,
    modifier: Modifier = Modifier,
) {
    val total = sets * (drops + 1)
    val minutes = (total * 40 + sets * rest + 59) / 60
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 14.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(R.string.label_in_this_day).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(
                    R.string.label_sets_and_time,
                    pluralStringResource(R.plurals.plural_sets, total, total),
                    reps,
                    minutes,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
