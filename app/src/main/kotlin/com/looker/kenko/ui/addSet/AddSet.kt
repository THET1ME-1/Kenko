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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.addSet.components.ITEMS
import com.looker.kenko.ui.addSet.components.ItemSize
import com.looker.kenko.ui.addSet.components.VerticalSelector
import com.looker.kenko.ui.addSet.components.WeightStepper
import com.looker.kenko.ui.addSet.components.WeightTextField
import com.looker.kenko.ui.components.WeightCalculator
import com.looker.kenko.ui.components.rememberPhoto
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.colorSchemes.JapanRed
import kotlinx.coroutines.launch

@Composable
fun AddSet(
    exerciseName: String,
    target: AddSetTarget,
    onDone: () -> Unit,
) {
    val viewModel: AddSetViewModel =
        hiltViewModel<AddSetViewModel, AddSetViewModel.AddSetViewModelFactory>(
            key = "$exerciseName-${target.parentSetId}",
        ) {
            it.create(target)
        }
    val grips by viewModel.grips.collectAsStateWithLifecycle()
    AddSetContent(
        exerciseName = exerciseName,
        isDrop = target.parentSetId != null,
        grips = grips,
        selectedGripId = viewModel.selectedGripId,
        onSelectGrip = viewModel::selectGrip,
        weights = viewModel.weights,
        reps = viewModel.reps,
        selectedSetType = viewModel.selectedSetType,
        onSelectSetType = viewModel::setSetType,
        onAddWeight = viewModel::addWeight,
        onRepsChanged = { viewModel.reps = it },
        onDoneClick = {
            viewModel.addSet()
            onDone()
        },
    )
}

@Composable
private fun AddSetContent(
    exerciseName: String,
    isDrop: Boolean = false,
    grips: List<Grip> = emptyList(),
    selectedGripId: Int? = null,
    onSelectGrip: (Int?) -> Unit = {},
    weights: TextFieldState,
    reps: Int,
    selectedSetType: SetType,
    onSelectSetType: (SetType) -> Unit,
    onAddWeight: (Float) -> Unit,
    onRepsChanged: (Int) -> Unit,
    onDoneClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .wrapContentHeight(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AddSetHeader(
            modifier = Modifier.fillMaxWidth(),
            exerciseName = exerciseName,
            isDrop = isDrop,
            onClick = onDoneClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isDrop) {
            SetTypeSelector(
                modifier = Modifier.align(CenterHorizontally),
                selected = selectedSetType,
                onSelect = onSelectSetType,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (grips.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier
                    .weight(1F)
                    .height(ItemSize * ITEMS),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeightTextField(
                    state = weights,
                    modifier = Modifier
                        .weight(3F)
                        .fillMaxWidth(),
                )
                WeightStepper(
                    onStep = { step ->
                        onAddWeight(step)
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    },
                    modifier = Modifier
                        .weight(2F)
                        .fillMaxWidth(),
                )
            }
            VerticalSelector(
                label = stringResource(R.string.label_reps),
                value = reps,
                onChanged = onRepsChanged,
                onChange = { haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        var calculatorOpen by rememberSaveable { mutableStateOf(false) }
        TextButton(
            modifier = Modifier.align(CenterHorizontally),
            onClick = { calculatorOpen = !calculatorOpen },
        ) {
            Text(text = stringResource(R.string.label_weight_calculator))
        }
        if (calculatorOpen) {
            var bar by remember { mutableFloatStateOf(20F) }
            var left by remember { mutableFloatStateOf(0F) }
            var right by remember { mutableFloatStateOf(0F) }
            WeightCalculator(
                bar = bar,
                left = left,
                right = right,
                onBarChange = {
                    bar = it
                    weights.setTextAndPlaceCursorAtEnd(formatWeight(bar + left + right))
                },
                onLeftChange = {
                    left = it
                    weights.setTextAndPlaceCursorAtEnd(formatWeight(bar + left + right))
                },
                onRightChange = {
                    right = it
                    weights.setTextAndPlaceCursorAtEnd(formatWeight(bar + left + right))
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
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
private fun AddSetHeader(
    exerciseName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDrop: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(
                    if (isDrop) R.string.label_add_drop_for else R.string.label_add_set_for,
                ).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        FilledTonalIconButton(onClick = onClick) {
            Icon(
                painter = KenkoIcons.Done,
                contentDescription = "",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTypeSelector(
    selected: SetType,
    onSelect: (SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(SetType.Standard, SetType.Drop, SetType.RestPause)
    Row(
        modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, type ->
            val interactionSource = remember { MutableInteractionSource() }
            val checked = selected == type
            OutlinedToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(type) },
                interactionSource = interactionSource,
                modifier = Modifier.semantics { role = Role.RadioButton },
                border = if (checked) ButtonDefaults.outlinedButtonBorder(true) else null,
                colors = OutlinedToggleButtonDefaults.colors(
                    checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    checkedContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                SetTypeIndicator(
                    selected = checked,
                    type = type,
                    interactionSource = interactionSource,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(text = setTypeLabel(type))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTypeIndicator(
    selected: Boolean,
    type: SetType,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphAnimatable = remember { Animatable(0F) }
    val morph = remember { Morph(MaterialShapes.Circle, setTypeShape(type)) }
    val path = remember { Path() }

    LaunchedEffect(isPressed || selected) {
        launch {
            if (isPressed || selected) {
                morphAnimatable.animateTo(1F)
            } else {
                morphAnimatable.animateTo(0F)
            }
        }
    }

    val color = setTypeColor(type)
    Canvas(modifier) {
        drawPath(
            color = color,
            path = processPath(
                path = morph.toPath(progress = morphAnimatable.value, path = path),
                size = size,
                scaleFactor = 1F,
            ),
        )
    }
}

private fun processPath(
    path: Path,
    size: Size,
    scaleFactor: Float,
    scaleMatrix: Matrix = Matrix(),
): Path {
    scaleMatrix.reset()

    scaleMatrix.apply { scale(x = size.width * scaleFactor, y = size.height * scaleFactor) }

    // Scale to the desired size.
    path.transform(scaleMatrix)

    // Translate the path to align its center with the available size center.
    path.translate(size.center - path.getBounds().center)
    return path
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun setTypeShape(type: SetType): RoundedPolygon = when (type) {
    SetType.Standard -> MaterialShapes.Ghostish
    SetType.Drop -> MaterialShapes.Arrow
    SetType.RestPause -> MaterialShapes.Bun
}

@Composable
private fun setTypeColor(type: SetType): Color = when (type) {
    SetType.Standard -> MaterialTheme.colorScheme.primary
    SetType.Drop -> MaterialTheme.colorScheme.tertiary
    SetType.RestPause -> JapanRed
}

fun setTypeLabel(type: SetType): String = when (type) {
    SetType.Standard -> "Standard"
    SetType.Drop -> "Drop"
    SetType.RestPause -> "Rest-Pause"
}

@Preview
@Composable
private fun AddSetPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Surface {
            AddSetContent(
                exerciseName = "Bench Press",
                weights = rememberTextFieldState("40.0"),
                reps = 12,
                selectedSetType = SetType.Standard,
                onSelectSetType = {},
                onAddWeight = {},
                onRepsChanged = {},
                onDoneClick = {},
            )
        }
    }
}
