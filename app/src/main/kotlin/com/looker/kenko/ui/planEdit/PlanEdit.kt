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

package com.looker.kenko.ui.planEdit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.BuildConfig
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.ExercisesPreviewParameter
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.PlanDayGroup
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.toDayGroups
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.DaySelectorChip
import com.looker.kenko.ui.components.ErrorSnackbar
import com.looker.kenko.ui.components.HorizontalDaySelector
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.PrimaryBorder
import com.looker.kenko.ui.components.SwipeToDeleteBox
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.planEdit.components.DaySwitcher
import com.looker.kenko.ui.planEdit.components.ExerciseItem
import com.looker.kenko.ui.planEdit.components.kenkoDayName
import com.looker.kenko.ui.selectExercise.SelectExercise
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.minus
import com.looker.kenko.utils.plus
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

@Composable
fun PlanEdit(
    viewModel: PlanEditViewModel,
    onBackPress: () -> Unit,
    onAddNewExerciseClick: (name: String?, target: MuscleGroups?) -> Unit,
) {
    val pageStage by viewModel.pageState.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler {
        viewModel.onBackPress(pageStage, onBackPress)
    }
    FullEdit(
        snackbarHostState = viewModel.snackbarState,
        stage = pageStage,
        fab = {
            PlanEditFAB(
                pageStage = pageStage,
                onClick = {
                    if (pageStage == PlanEditStage.NameEdit) {
                        viewModel.saveName()
                    } else {
                        viewModel.openSheet()
                    }
                },
            )
        },
        onBackPress = { viewModel.onBackPress(pageStage, onBackPress) },
        onDebugMockClick = viewModel::debugFillMockData,
    ) { stage ->
        when (stage) {
            PlanEditStage.NameEdit -> {
                val isNameAlreadyUsed by viewModel.isNameAlreadyUsed.collectAsStateWithLifecycle()
                NameEdit(
                    state = viewModel.planNameState,
                    isNameAlreadyUsed = isNameAlreadyUsed,
                    onSaveClick = viewModel::saveName,
                )
            }

            PlanEditStage.PlanEdit -> {
                PlanEdit(
                    state = state,
                    onSelectDay = viewModel::setCurrentDay,
                    onRemoveItemClick = viewModel::removeItem,
                    onFullDaySelection = viewModel::openFullDaySelection,
                    onItemClick = viewModel::editTargets,
                    onItemLongClick = viewModel::toggleSelection,
                    onMakeSuperset = viewModel::makeSuperset,
                    onBreakSuperset = viewModel::breakSuperset,
                    onClearSelection = viewModel::clearSelection,
                )
            }
        }
    }

    if (state.exerciseSheetVisible) {
        AddExerciseSheet(
            onDismiss = viewModel::closeSheet,
            onDone = viewModel::addExercise,
            onAddNewExerciseClick = onAddNewExerciseClick,
        )
    }

    val editedItem by viewModel.editedItem.collectAsStateWithLifecycle()
    editedItem?.let { item ->
        TargetsSheet(
            item = item,
            onDismiss = { viewModel.editTargets(null) },
            onSave = { sets, reps, rest -> viewModel.saveTargets(item, sets, reps, rest) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullEdit(
    snackbarHostState: SnackbarHostState,
    stage: PlanEditStage,
    fab: @Composable () -> Unit,
    onBackPress: () -> Unit,
    onDebugMockClick: (() -> Unit)? = null,
    ui: @Composable (stage: PlanEditStage) -> Unit,
) {
    Scaffold(
        floatingActionButton = fab,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                ErrorSnackbar(data = it)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBackPress) },
                actions = {
                    if (BuildConfig.DEBUG && stage == PlanEditStage.PlanEdit) {
                        IconButton(onClick = { onDebugMockClick?.invoke() }) {
                            Icon(painter = KenkoIcons.Add, contentDescription = "Mock data")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            modifier = Modifier.padding(innerPadding + PaddingValues(horizontal = 16.dp)),
            targetState = stage,
            label = "Plan edit stage",
            transitionSpec = {
                when (targetState) {
                    PlanEditStage.NameEdit -> {
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 2 } + fadeOut()
                    }

                    PlanEditStage.PlanEdit -> {
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 2 } + fadeOut()
                    }
                } using SizeTransform(clip = false)
            },
        ) {
            ui(it)
        }
    }
}

@Composable
private fun PlanEditFAB(
    pageStage: PlanEditStage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KenkoButton(
        modifier = modifier,
        onClick = onClick,
        label = {
            AnimatedContent(
                targetState = pageStage,
                label = "FAB label",
                transitionSpec = {
                    when (targetState) {
                        PlanEditStage.NameEdit -> {
                            slideInVertically { it } + fadeIn() togetherWith
                                slideOutVertically { -it } + fadeOut()
                        }

                        PlanEditStage.PlanEdit -> {
                            slideInVertically { -it } + fadeIn() togetherWith
                                slideOutVertically { it } + fadeOut()
                        }
                    } using SizeTransform(clip = false)
                },
            ) {
                if (it == PlanEditStage.NameEdit) {
                    Text(stringResource(R.string.label_next))
                } else {
                    Text(stringResource(R.string.label_add))
                }
            }
        },
        icon = {
            AnimatedContent(
                targetState = pageStage,
                label = "FAB icon",
                transitionSpec = {
                    when (targetState) {
                        PlanEditStage.NameEdit -> {
                            slideInHorizontally { it * 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it * 2 } + fadeOut()
                        }

                        PlanEditStage.PlanEdit -> {
                            slideInHorizontally { -it * 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it * 2 } + fadeOut()
                        }
                    } using SizeTransform(clip = false)
                },
            ) {
                if (it == PlanEditStage.NameEdit) {
                    Icon(
                        painter = KenkoIcons.ArrowForward,
                        contentDescription = stringResource(R.string.label_next),
                    )
                } else {
                    Icon(
                        painter = KenkoIcons.Add,
                        contentDescription = stringResource(R.string.label_add),
                    )
                }
            }
        },
    )
}

@Composable
private fun NameEdit(
    state: TextFieldState,
    isNameAlreadyUsed: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanName(
        planName = state,
        error = isNameAlreadyUsed,
        onNext = { onSaveClick() },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun PlanEdit(
    state: PlanEditState,
    onSelectDay: (DayOfWeek) -> Unit,
    onRemoveItemClick: (PlanItem) -> Unit,
    onFullDaySelection: () -> Unit,
    onItemClick: (PlanItem) -> Unit,
    onItemLongClick: (PlanItem) -> Unit,
    onMakeSuperset: () -> Unit,
    onBreakSuperset: (Int) -> Unit,
    onClearSelection: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isCurrentDayBlank by remember(state.items) { derivedStateOf { state.items.isEmpty() } }
    val groups by remember(state.items) { derivedStateOf { state.items.toDayGroups() } }
    PlanExercise(
        modifier = Modifier.fillMaxSize(),
        header = {
            Header(
                isExpandedView = state.selectionMode,
                daySelector = {
                    HorizontalDaySelector(
                        item = { dayOfWeek ->
                            DaySelectorChip(
                                selected = dayOfWeek == state.currentDay,
                                onClick = { onSelectDay(dayOfWeek) },
                            ) {
                                Text(kenkoDayName(dayOfWeek))
                            }
                        },
                    )
                },
                daySwitcher = {
                    DaySwitcher(
                        selected = state.currentDay,
                        onNext = { onSelectDay(state.currentDay + 1) },
                        onPrevious = { onSelectDay(state.currentDay - 1) },
                        onClick = onFullDaySelection,
                    )
                },
            )
        },
        items = {
            item { Spacer(Modifier.height(12.dp)) }
            if (state.selectedItems.isNotEmpty()) {
                item {
                    SelectionBar(
                        count = state.selectedItems.size,
                        onMakeSuperset = onMakeSuperset,
                        onClearSelection = onClearSelection,
                    )
                }
            }
            if (isCurrentDayBlank) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_exercises_yet),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                var number = 0
                groups.forEach { group ->
                    when (group) {
                        is PlanDayGroup.Single -> {
                            number++
                            val position = number
                            item(key = group.item.id) {
                                SwipeToDeleteBox(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .animateItem(),
                                    onDismiss = {
                                        focusManager.clearFocus()
                                        onRemoveItemClick(group.item)
                                    },
                                ) {
                                    PlanItemRow(
                                        item = group.item,
                                        position = position,
                                        selected = group.item.id in state.selectedItems,
                                        onClick = { onItemClick(group.item) },
                                        onLongClick = { onItemLongClick(group.item) },
                                    )
                                }
                            }
                        }

                        is PlanDayGroup.Superset -> {
                            val positions = group.items.map { number++ + 1 }
                            item(key = "superset-${group.id}") {
                                SupersetGroup(
                                    modifier = Modifier.animateItem(),
                                    onBreak = { onBreakSuperset(group.id) },
                                ) {
                                    group.items.forEachIndexed { index, item ->
                                        PlanItemRow(
                                            item = item,
                                            position = positions[index],
                                            selected = item.id in state.selectedItems,
                                            onClick = { onItemClick(item) },
                                            onLongClick = { onItemLongClick(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        },
    )
}

@Composable
private fun PlanItemRow(
    item: PlanItem,
    position: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExerciseItem(
        modifier = modifier,
        exercise = item.exercise,
        subtitle = stringResource(
            R.string.label_targets,
            item.targetSets,
            item.targetReps,
            formatRest(item.restSeconds),
        ),
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingIcon = {
            Text(
                text = normalizeInt(position),
                style = LocalTextStyle.current.numbers(),
            )
        },
    )
}

@Composable
private fun SupersetGroup(
    onBreak: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                border = PrimaryBorder,
                shape = MaterialTheme.shapes.large,
            )
            .clip(MaterialTheme.shapes.large),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_superset).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1F))
            TextButton(onClick = onBreak) {
                Text(text = stringResource(R.string.label_break_superset))
            }
        }
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onMakeSuperset: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = normalizeInt(count),
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1F))
        TextButton(onClick = onClearSelection) {
            Text(text = stringResource(R.string.label_cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onMakeSuperset) {
            Text(text = stringResource(R.string.label_make_superset))
        }
    }
}

@Composable
private fun formatRest(seconds: Int): String = when {
    seconds < 60 -> "$seconds ${stringResource(R.string.label_seconds_short)}"
    seconds % 60 == 0 -> "${seconds / 60} ${stringResource(R.string.label_minutes_short)}"
    else -> "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetsSheet(
    item: PlanItem,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: Int, restSeconds: Int) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sets by remember(item.id) { mutableIntStateOf(item.targetSets) }
    var reps by remember(item.id) { mutableIntStateOf(item.targetReps) }
    var rest by remember(item.id) { mutableIntStateOf(item.restSeconds) }
    ModalBottomSheet(
        sheetState = state,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.label_edit_targets).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = item.exercise.name,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(16.dp))
            StepperRow(
                label = stringResource(R.string.label_sets),
                value = normalizeInt(sets),
                onDecrease = { sets = (sets - 1).coerceAtLeast(1) },
                onIncrease = { sets = (sets + 1).coerceAtMost(20) },
            )
            StepperRow(
                label = stringResource(R.string.label_reps),
                value = normalizeInt(reps),
                onDecrease = { reps = (reps - 1).coerceAtLeast(1) },
                onIncrease = { reps = (reps + 1).coerceAtMost(100) },
            )
            StepperRow(
                label = stringResource(R.string.label_rest),
                value = formatRest(rest),
                onDecrease = { rest = (rest - REST_STEP_SECONDS).coerceAtLeast(0) },
                onIncrease = { rest = (rest + REST_STEP_SECONDS).coerceAtMost(600) },
            )
            Spacer(Modifier.height(16.dp))
            KenkoButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { onSave(sets, reps, rest) },
                label = { Text(stringResource(R.string.label_save)) },
                icon = {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = KenkoIcons.Done,
                        contentDescription = null,
                    )
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val REST_STEP_SECONDS = 15

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1F),
        )
        FilledTonalIconButton(onClick = onDecrease) {
            Icon(painter = KenkoIcons.Remove, contentDescription = null)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.numbers(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        FilledTonalIconButton(onClick = onIncrease) {
            Icon(painter = KenkoIcons.Add, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseSheet(
    onDismiss: () -> Unit,
    onDone: (Exercise) -> Unit,
    onAddNewExerciseClick: (name: String?, target: MuscleGroups?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(sheetState = state, onDismissRequest = onDismiss) {
        SelectExercise(
            onRequestNewExercise = onAddNewExerciseClick,
            onDone = { exercise ->
                scope.launch {
                    onDone(exercise)
                    state.hide()
                }.invokeOnCompletion {
                    if (!state.isVisible) onDismiss()
                }
            },
        )
    }
}

@Preview
@Composable
private fun ExerciseItemPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    val exercise = ExercisesPreviewParameter().values.first().first()
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        ExerciseItem(exercise = exercise) {
            Text(
                text = normalizeInt(1),
                style = LocalTextStyle.current.numbers(),
            )
        }
    }
}
