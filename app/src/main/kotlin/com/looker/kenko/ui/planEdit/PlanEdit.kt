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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.BuildConfig
import com.looker.kenko.R
import com.looker.kenko.data.model.DROP_PERCENT_STEP
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.ExercisesPreviewParameter
import com.looker.kenko.data.model.MAX_DROP_COUNT
import com.looker.kenko.data.model.MAX_DROP_PERCENT
import com.looker.kenko.data.model.MIN_DROP_PERCENT
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.PlanDayGroup
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.SessionBlock
import com.looker.kenko.data.model.SetChain
import com.looker.kenko.data.model.daySummary
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.data.model.toDayGroups
import com.looker.kenko.data.model.toPreviewBlocks
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.DaySelectorChip
import com.looker.kenko.ui.components.ErrorSnackbar
import com.looker.kenko.ui.components.HorizontalDaySelector
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.PrimaryBorder
import com.looker.kenko.ui.components.SwipeToDeleteBox
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.planEdit.components.DaySwitcher
import com.looker.kenko.ui.planEdit.components.ExerciseItem
import com.looker.kenko.ui.planEdit.components.kenkoDayName
import com.looker.kenko.ui.selectExercise.SelectExercise
import com.looker.kenko.ui.sessionDetail.components.DropSetCard
import com.looker.kenko.ui.sessionDetail.components.SetItem
import com.looker.kenko.ui.sessionDetail.components.SupersetCard
import com.looker.kenko.ui.sessionDetail.components.SupersetRow
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
                    onAddExercise = viewModel::openSheet,
                    onStartSupersetMode = viewModel::startSupersetMode,
                    onReplaceItem = viewModel::startReplacing,
                    onMoveItem = viewModel::moveItem,
                )
            }
        }
    }

    if (state.exerciseSheetVisible) {
        SelectExercise(
            modifier = Modifier.fillMaxSize(),
            onBackPress = viewModel::closeSheet,
            onRequestNewExercise = onAddNewExerciseClick,
            onDone = { exercise ->
                viewModel.addExercise(exercise)
                viewModel.closeSheet()
            },
        )
    }

    val editedItem by viewModel.editedItem.collectAsStateWithLifecycle()
    val replacedItem by viewModel.replacedItem.collectAsStateWithLifecycle()
    replacedItem?.let { item ->
        SelectExercise(
            modifier = Modifier.fillMaxSize(),
            onBackPress = { viewModel.startReplacing(null) },
            onRequestNewExercise = onAddNewExerciseClick,
            onDone = { exercise -> viewModel.replaceExercise(item, exercise) },
        )
    }
    editedItem?.let { item ->
        val grips by viewModel.gripsOfEdited.collectAsStateWithLifecycle()
        TargetsScreen(
            modifier = Modifier.fillMaxSize(),
            item = item,
            grips = grips,
            onBackPress = { viewModel.editTargets(null) },
            onSave = { targets -> viewModel.saveTargets(item, targets) },
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
    onSelectDay: (Int) -> Unit,
    onRemoveItemClick: (PlanItem) -> Unit,
    onFullDaySelection: () -> Unit,
    onItemClick: (PlanItem) -> Unit,
    onItemLongClick: (PlanItem) -> Unit,
    onMakeSuperset: () -> Unit,
    onBreakSuperset: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onAddExercise: () -> Unit,
    onStartSupersetMode: () -> Unit,
    onReplaceItem: (PlanItem) -> Unit,
    onMoveItem: (PlanItem, Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isCurrentDayBlank by remember(state.items) { derivedStateOf { state.items.isEmpty() } }
    val expandedBlocks = remember { mutableStateMapOf<Long, Boolean>() }
    PlanExercise(
        modifier = Modifier.fillMaxSize(),
        header = {
            DayHeader(
                day = state.currentDay,
                dayCount = state.dayCount,
                isWeekMode = state.isWeekMode,
                onSelectDay = onSelectDay,
            )
        },
        items = {
            item {
                DaySummaryRow(items = state.items)
            }
            if (state.selectedItems.isNotEmpty() || state.supersetMode) {
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
                val blocks = state.items.toPreviewBlocks()
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is SessionBlock.SingleExercise -> {
                            val item = block.plan ?: return@forEachIndexed
                            item(key = "item-${item.id}") {
                                PlanExerciseBlock(
                                    modifier = Modifier.animateItem(),
                                    item = item,
                                    number = index + 1,
                                    chains = block.chains,
                                    selected = item.id in state.selectedItems,
                                    expanded = expandedBlocks[item.id] ?: true,
                                    onToggleExpand = {
                                        expandedBlocks[item.id ?: 0L] =
                                            !(expandedBlocks[item.id] ?: true)
                                    },
                                    onClick = {
                                        if (state.supersetMode) onItemLongClick(item) else onItemClick(item)
                                    },
                                    onLongClick = { onItemLongClick(item) },
                                    onReplace = { onReplaceItem(item) },
                                    onMoveUp = { onMoveItem(item, -1) },
                                    onMoveDown = { onMoveItem(item, 1) },
                                    onRemove = {
                                        focusManager.clearFocus()
                                        onRemoveItemClick(item)
                                    },
                                )
                            }
                        }

                        is SessionBlock.Superset -> {
                            item(key = "superset-${block.id}") {
                                val open = expandedBlocks[-block.id.toLong()] ?: true
                                Column(modifier = Modifier.animateItem()) {
                                    if (open) {
                                        SupersetCard(
                                            block = block,
                                            isEditable = true,
                                            isPlan = true,
                                            onCollapse = {
                                                expandedBlocks[-block.id.toLong()] = false
                                            },
                                            onCloseRound = {},
                                            onUndo = {},
                                        )
                                    } else {
                                        SupersetRow(
                                            block = block,
                                            number = index + 1,
                                            isPlan = true,
                                            onExpand = {
                                                expandedBlocks[-block.id.toLong()] = true
                                            },
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        block.plan.forEach { planItem ->
                                            TextButton(onClick = { onItemClick(planItem) }) {
                                                Text(
                                                    text = "${planItem.exercise.displayName()} ${planItem.targetSets}×${planItem.repsLabel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                        TextButton(onClick = { onBreakSuperset(block.id) }) {
                                            Text(text = stringResource(R.string.label_break_superset))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DashedAddButton(
                        modifier = Modifier.weight(1F),
                        label = stringResource(R.string.label_add_exercise_short),
                        onClick = onAddExercise,
                    )
                    DashedAddButton(
                        modifier = Modifier.weight(1F),
                        label = stringResource(R.string.label_add_superset),
                        accent = true,
                        onClick = onStartSupersetMode,
                    )
                }
            }
        },
    )
}

/**
 * An exercise inside a plan: the same rows the session will show, plus the tools to
 * set it up, swap it for another movement or move it around the day.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanExerciseBlock(
    item: PlanItem,
    number: Int,
    chains: List<SetChain>,
    selected: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReplace: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chain = chains.firstOrNull()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggleExpand, onLongClick = onLongClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = normalizeInt(number),
                style = MaterialTheme.typography.headlineSmall.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = item.exercise.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${item.targetSets} × ${item.repsLabel} · " +
                        "${formatWeight(item.targetWeight)} ${stringResource(R.string.label_kg)} · " +
                        stringResource(R.string.label_rest_short, formatRest(item.restSeconds)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (expanded) {
            if (item.dropCount > 0 && chain != null) {
                DropSetCard(
                    chain = chain,
                    number = number,
                    isEditable = false,
                    isPlan = true,
                    onCollapse = onToggleExpand,
                    onDropsChange = {},
                    onPercentChange = {},
                    onMarkStep = {},
                    onMarkGroup = {},
                    onUndo = {},
                )
            } else {
                chains.forEachIndexed { index, setChain ->
                    SetItem(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        set = setChain.set,
                        title = { Text(text = normalizeInt(index + 1)) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClick) {
                    Text(text = stringResource(R.string.label_edit_targets))
                }
                TextButton(onClick = onReplace) {
                    Text(text = stringResource(R.string.label_replace_exercise))
                }
                TextButton(onClick = onMoveUp) {
                    Text(text = stringResource(R.string.label_move_up))
                }
                TextButton(onClick = onMoveDown) {
                    Text(text = stringResource(R.string.label_move_down))
                }
                Spacer(Modifier.weight(1F))
                IconButton(onClick = onRemove) {
                    Icon(painter = KenkoIcons.Delete, contentDescription = null)
                }
            }
        }
    }
}

/**
 * Days of a plan are just numbers: one, two, three, and a new one whenever it is needed.
 * In week mode the same number reads as a day of the week.
 */
@Composable
private fun DayHeader(
    day: Int,
    dayCount: Int,
    isWeekMode: Boolean,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = stringResource(R.string.heading_select_plan_items),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onSelectDay(day - 1) },
                enabled = day > 1,
            ) {
                Icon(painter = KenkoIcons.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                modifier = Modifier
                    .weight(1F)
                    .padding(horizontal = 8.dp),
                text = if (isWeekMode) {
                    kenkoDayName(DayOfWeek(day.coerceIn(1, 7)))
                } else {
                    stringResource(R.string.label_day_number, day)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            FilledTonalIconButton(
                onClick = { onSelectDay(day + 1) },
                enabled = !isWeekMode || day < 7,
            ) {
                Icon(painter = KenkoIcons.KeyboardArrowRight, contentDescription = null)
            }
        }
        if (!isWeekMode && dayCount > 0) {
            Text(
                text = stringResource(
                    R.string.label_days_in_plan,
                    pluralStringResource(R.plurals.plural_days, dayCount, dayCount),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * How much work the day holds, right under its title.
 */
@Composable
private fun DaySummaryRow(
    items: List<PlanItem>,
    modifier: Modifier = Modifier,
) {
    val summary = remember(items) { items.daySummary() }
    Text(
        text = stringResource(
            R.string.label_day_summary,
            pluralStringResource(R.plurals.plural_exercises, summary.exercises, summary.exercises),
            pluralStringResource(R.plurals.plural_sets, summary.sets, summary.sets),
            summary.minutes,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(bottom = 12.dp),
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
        subtitle = buildString {
            append(stringResource(item.exercise.target.stringRes))
            if (item.dropCount > 0) {
                append(" · ")
                append(stringResource(R.string.label_drop_short, item.dropCount))
            }
            append(" · ")
            append(stringResource(R.string.label_rest_short, formatRest(item.restSeconds)))
        },
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingIcon = {
            Text(
                text = normalizeInt(position),
                style = LocalTextStyle.current.numbers(),
            )
        },
        trailing = {
            PresetPill(item = item, onClick = onClick)
        },
    )
}

/**
 * `3×10` — the preset of the day, edited by tapping it.
 */
@Composable
private fun PresetPill(
    item: PlanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${item.targetSets}×${item.targetReps}",
        style = MaterialTheme.typography.labelLarge.numbers(),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun SupersetGroup(
    number: Int,
    rounds: Int,
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
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = normalizeInt(number),
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(
                    R.string.label_superset_rounds,
                    pluralStringResource(R.plurals.plural_rounds, rounds, rounds),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1F),
            )
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
