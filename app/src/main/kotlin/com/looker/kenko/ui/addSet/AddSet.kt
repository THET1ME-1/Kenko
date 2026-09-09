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

package com.looker.kenko.ui.addSet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.WEIGHT_STEP
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.Loadout
import com.looker.kenko.ui.components.PlateCalculator
import com.looker.kenko.ui.components.loadoutFor
import com.looker.kenko.ui.components.WeightRuler
import com.looker.kenko.ui.components.rememberPhoto
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers

@Composable
fun AddSet(
    exerciseName: String,
    target: AddSetTarget,
    onDone: () -> Unit,
    setNumber: Int = 1,
) {
    val viewModel: AddSetViewModel =
        hiltViewModel<AddSetViewModel, AddSetViewModel.AddSetViewModelFactory>(
            key = "$exerciseName-${target.parentSetId}-${target.dropIndex}",
        ) {
            it.create(target)
        }
    val grips by viewModel.grips.collectAsStateWithLifecycle()
    val weight = viewModel.weights.text.toString().toFloatOrNull() ?: 0F
    var loadout by remember { mutableStateOf<Loadout?>(null) }

    val plates = loadout
    if (plates != null) {
        PlatesSheet(
            loadout = plates,
            onLoadoutChange = { loadout = it },
            onBack = { loadout = null },
            onApply = {
                viewModel.setWeight(it)
                loadout = null
            },
        )
        return
    }

    AddSetContent(
        exerciseName = exerciseName,
        isDrop = target.parentSetId != null,
        dropIndex = target.dropIndex,
        setNumber = setNumber,
        lastSet = viewModel.lastSet,
        weight = weight,
        reps = viewModel.reps,
        reserve = viewModel.repsInReserve,
        setType = viewModel.selectedSetType,
        grips = grips,
        selectedGripId = viewModel.selectedGripId,
        onSelectGrip = viewModel::selectGrip,
        onWeightChange = viewModel::setWeight,
        onRepsChanged = { viewModel.reps = it },
        onCycleType = viewModel::cycleSetType,
        onCycleReserve = viewModel::cycleReserve,
        onFailure = { viewModel.setReserve(0) },
        gymHint = viewModel.gymHint,
        onApplyGymHint = viewModel::applyGymHint,
        onDismissGymHint = viewModel::dismissGymHint,
        onOpenPlates = { loadout = loadoutFor(weight) },
        onDoneClick = {
            viewModel.addSet()
            onDone()
        },
    )
}

/**
 * The plate calculator: the same sheet, but showing a bar instead of the entry keys.
 */
@Composable
private fun PlatesSheet(
    loadout: Loadout,
    onLoadoutChange: (Loadout) -> Unit,
    onBack: () -> Unit,
    onApply: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = KenkoIcons.ArrowBack,
                    contentDescription = stringResource(R.string.label_back),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.title_plate_calculator),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        PlateCalculator(
            loadout = loadout,
            onLoadoutChange = onLoadoutChange,
            onApply = onApply,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Writing a set: the weight is read from one big number and changed by dragging the ruler
 * under it. Everything else stands in rows of equal keys, so nothing wraps or jumps.
 */
@Composable
private fun AddSetContent(
    exerciseName: String,
    weight: Float,
    reps: Int,
    reserve: Int,
    setType: SetType,
    onWeightChange: (Float) -> Unit,
    onRepsChanged: (Int) -> Unit,
    onCycleType: () -> Unit,
    onCycleReserve: () -> Unit,
    onFailure: () -> Unit,
    onOpenPlates: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDrop: Boolean = false,
    dropIndex: Int = 0,
    setNumber: Int = 1,
    lastSet: Set? = null,
    gymHint: GymHint? = null,
    onApplyGymHint: () -> Unit = {},
    onDismissGymHint: () -> Unit = {},
    grips: List<Grip> = emptyList(),
    selectedGripId: Int? = null,
    onSelectGrip: (Int?) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (isDrop) {
                stringResource(R.string.label_drop_number, dropIndex)
            } else {
                stringResource(
                    R.string.label_set_number,
                    setNumber.toString().padStart(2, '0'),
                )
            }.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = lastSet?.let {
                stringResource(R.string.label_last_time, formatWeight(it.weight), it.repsOrDuration)
            } ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )

        if (gymHint != null) {
            Spacer(Modifier.height(10.dp))
            GymHintRow(
                hint = gymHint,
                onApply = onApplyGymHint,
                onDismiss = onDismissGymHint,
            )
        }

        if (grips.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GripChip(
                    label = stringResource(R.string.label_no_grip),
                    photo = null,
                    selected = selectedGripId == null,
                    onClick = { onSelectGrip(null) },
                )
                grips.forEach { grip ->
                    GripChip(
                        label = grip.name,
                        photo = grip.photoUri,
                        selected = selectedGripId == grip.id,
                        onClick = { onSelectGrip(grip.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.align(CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatWeight(weight),
                style = MaterialTheme.typography.displayMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(R.string.label_kg).lowercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(8.dp))

        WeightRuler(weight = weight, onWeightChange = onWeightChange)

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WEIGHT_STEPS.forEach { step ->
                StepKey(
                    modifier = Modifier.weight(1F),
                    label = stepLabel(step),
                    accent = step == WEIGHT_STEP,
                    onClick = { onWeightChange((weight + step).coerceAtLeast(0F)) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.label_reps_caption).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepKey(
                modifier = Modifier.weight(1F),
                label = "−1",
                onClick = { onRepsChanged((reps - 1).coerceAtLeast(1)) },
            )
            DisplayKey(
                modifier = Modifier.weight(1F),
                label = reps.toString(),
            )
            StepKey(
                modifier = Modifier.weight(1F),
                label = "+1",
                onClick = { onRepsChanged(reps + 1) },
            )
            StepKey(
                modifier = Modifier.weight(1.8F),
                label = stringResource(R.string.label_to_failure),
                accent = reserve == 0,
                onClick = onFailure,
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onDoneClick,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = stringResource(
                    if (isDrop) R.string.label_write_drop else R.string.label_write_set,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineKey(
                modifier = Modifier.weight(1F),
                label = stringResource(R.string.label_plates),
                onClick = onOpenPlates,
            )
            if (!isDrop) {
                OutlineKey(
                    modifier = Modifier.weight(1F),
                    label = setTypeLabel(setType),
                    accent = setType != SetType.Standard,
                    onClick = onCycleType,
                )
            }
            OutlineKey(
                modifier = Modifier.weight(1F),
                label = if (reserve == 0) {
                    stringResource(R.string.label_to_failure)
                } else {
                    stringResource(R.string.label_rir_short, reserve)
                },
                onClick = onCycleReserve,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The same exercise weighs differently from gym to gym. The sheet says so and offers the
 * corrected weight instead of silently changing it.
 */
@Composable
private fun GymHintRow(
    hint: GymHint,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onApply)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = hint.gymName?.let {
                    stringResource(R.string.label_gym_shift_named, it, hint.percent)
                } ?: stringResource(R.string.label_gym_shift, hint.percent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.label_gym_shift_take,
                    formatWeight(hint.suggested),
                ),
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            text = stringResource(R.string.label_no),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * Steps of the bar a lifter actually uses. Order matters: minus on the left, plus on the right.
 */
private val WEIGHT_STEPS = listOf(-2.5F, -WEIGHT_STEP, WEIGHT_STEP, 2.5F)

/**
 * `−2.5` and `+1.25` — the same minus sign the reps row uses.
 */
private fun stepLabel(step: Float): String {
    val number = formatWeight(kotlin.math.abs(step)).removeSuffix(".0")
    return if (step > 0) "+$number" else "−$number"
}

/**
 * The current value, sitting in the same slot as the keys but not asking to be pressed.
 */
@Composable
private fun DisplayKey(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.numbers(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun StepKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (accent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = if (accent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun OutlineKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = KenkoBorderWidth,
                color = if (accent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.extraLarge,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (accent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * A handle to pick before the set: name, and its photo when there is one.
 */
@Composable
private fun GripChip(
    label: String,
    photo: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberPhoto(photo)
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(26.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun setTypeLabel(type: SetType): String = stringResource(
    when (type) {
        SetType.Standard -> R.string.label_set_type_standard
        SetType.Drop -> R.string.label_set_type_drop
        SetType.RestPause -> R.string.label_set_type_rest_pause
    },
)

@Preview
@Composable
private fun AddSetPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    var weight by remember { mutableFloatStateOf(47.5F) }
    var reps by remember { mutableStateOf(8) }
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Surface {
            AddSetContent(
                exerciseName = "Bench Press",
                weight = weight,
                reps = reps,
                reserve = 2,
                setType = SetType.Standard,
                onWeightChange = { weight = it },
                onRepsChanged = { reps = it },
                onCycleType = {},
                onCycleReserve = {},
                onFailure = {},
                onOpenPlates = {},
                onDoneClick = {},
            )
        }
    }
}
