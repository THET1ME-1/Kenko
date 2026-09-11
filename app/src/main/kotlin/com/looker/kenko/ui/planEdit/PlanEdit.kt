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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.mutableStateOf
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
import com.looker.kenko.ui.components.RowAction
import com.looker.kenko.ui.components.ActionsSheet
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.looker.kenko.ui.components.KenkoBorderWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import com.looker.kenko.ui.components.rememberReorderState
import com.looker.kenko.ui.components.reorderHandle
import com.looker.kenko.ui.components.reorderableItem
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.DaySelectorChip
import com.looker.kenko.ui.components.ErrorSnackbar
import com.looker.kenko.ui.components.HorizontalDaySelector
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.SheetAction
import com.looker.kenko.ui.components.PrimaryBorder
import com.looker.kenko.ui.components.SwipeToDeleteBox
import com.looker.kenko.ui.components.unitLabel
import com.looker.kenko.ui.components.weightText
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
    val folds by viewModel.folds.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<PlanItem?>(null) }
    var supersetActionsFor by remember { mutableStateOf<Int?>(null) }
    BackHandler {
        viewModel.onBackPress(pageStage, onBackPress)
    }
    FullEdit(
        snackbarHostState = viewModel.snackbarState,
        stage = pageStage,
        fab = {
            if (pageStage == PlanEditStage.NameEdit) {
                PlanEditFAB(
                    pageStage = pageStage,
                    onClick = viewModel::saveName,
                )
            }
        },
        onBackPress = { viewModel.onBackPress(pageStage, onBackPress) },
        title = viewModel.planNameState.text.toString(),
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
                    folds = folds,
                    onFold = viewModel::setFolded,
                    onCopyDay = viewModel::copyCurrentDayTo,
                    onSelectDay = viewModel::setCurrentDay,
                    onAddExercise = viewModel::openSheet,
                    onMoreItemClick = { actionsFor = it },
                    onMoreSupersetClick = { supersetActionsFor = it },
                    onReorder = viewModel::reorderDay,
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

    actionsFor?.let { item ->
        ActionsSheet(
            title = item.exercise.displayName(),
            subtitle = stringResource(
                R.string.label_plan_goal_FORMAT,
                item.targetSets,
                item.repsLabel,
            ),
            actions = planItemActions(
                item = item,
                items = state.items,
                onDismiss = { actionsFor = null },
                onEditTargets = viewModel::editTargets,
                onReplace = viewModel::startReplacing,
                onMove = viewModel::moveItem,
                onTieWithNext = viewModel::tieWithNext,
                onRemove = viewModel::removeItem,
            ),
            onDismiss = { actionsFor = null },
        )
    }
    supersetActionsFor?.let { supersetId ->
        val members = state.items.filter { it.supersetId == supersetId }
        ActionsSheet(
            title = stringResource(R.string.label_superset),
            subtitle = members
                .map { it.exercise.displayName() }
                .joinToString(separator = ", "),
            actions = supersetActions(
                members = members,
                onDismiss = { supersetActionsFor = null },
                onEditTargets = viewModel::editTargets,
                onBreak = { viewModel.breakSuperset(supersetId) },
                onRemove = viewModel::removeItem,
            ),
            onDismiss = { supersetActionsFor = null },
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
        TargetsSheet(
            item = item,
            grips = grips,
            onDismiss = { viewModel.editTargets(null) },
            onSave = { targets -> viewModel.saveTargets(item, targets) },
        )
    }
}

/**
 * What can be done to an exercise of a day. The running session offers the same list.
 */
@Composable
private fun planItemActions(
    item: PlanItem,
    items: List<PlanItem>,
    onDismiss: () -> Unit,
    onEditTargets: (PlanItem) -> Unit,
    onReplace: (PlanItem) -> Unit,
    onMove: (PlanItem, Int) -> Unit,
    onTieWithNext: (PlanItem) -> Unit,
    onRemove: (PlanItem) -> Unit,
): List<RowAction> = buildList {
    val ordered = items.sortedBy { it.order }
    val index = ordered.indexOfFirst { it.id == item.id }
    add(
        RowAction(
            label = stringResource(R.string.label_edit_goal),
            hint = stringResource(
                R.string.label_plan_goal_FORMAT,
                item.targetSets,
                item.repsLabel,
            ),
        ) {
            onDismiss()
            onEditTargets(item)
        },
    )
    add(
        RowAction(label = stringResource(R.string.label_replace_exercise)) {
            onDismiss()
            onReplace(item)
        },
    )
    val next = ordered.getOrNull(index + 1)
    if (item.supersetId == null && next != null && next.supersetId == null) {
        add(
            RowAction(label = stringResource(R.string.label_merge_with_next)) {
                onDismiss()
                onTieWithNext(item)
            },
        )
    }
    if (index > 0) {
        add(
            RowAction(label = stringResource(R.string.label_move_up)) {
                onDismiss()
                onMove(item, -1)
            },
        )
    }
    if (index >= 0 && index < ordered.lastIndex) {
        add(
            RowAction(label = stringResource(R.string.label_move_down)) {
                onDismiss()
                onMove(item, 1)
            },
        )
    }
    add(
        RowAction(
            label = stringResource(R.string.label_remove_from_day),
            isDanger = true,
        ) {
            onDismiss()
            onRemove(item)
        },
    )
}

/**
 * A superset of the day: the goals of its exercises, and the way out of it.
 */
@Composable
private fun supersetActions(
    members: List<PlanItem>,
    onDismiss: () -> Unit,
    onEditTargets: (PlanItem) -> Unit,
    onBreak: () -> Unit,
    onRemove: (PlanItem) -> Unit,
): List<RowAction> = buildList {
    members.forEach { member ->
        add(
            RowAction(
                label = stringResource(
                    R.string.label_goal_of_FORMAT,
                    member.exercise.displayName(),
                ),
                hint = stringResource(
                    R.string.label_plan_goal_FORMAT,
                    member.targetSets,
                    member.repsLabel,
                ),
            ) {
                onDismiss()
                onEditTargets(member)
            },
        )
    }
    add(
        RowAction(label = stringResource(R.string.label_break_superset)) {
            onDismiss()
            onBreak()
        },
    )
    members.forEach { member ->
        add(
            RowAction(
                label = stringResource(
                    R.string.label_remove_of_FORMAT,
                    member.exercise.displayName(),
                ),
                isDanger = true,
            ) {
                onDismiss()
                onRemove(member)
            },
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
    title: String = "",
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
                title = {
                    if (stage == PlanEditStage.PlanEdit && title.isNotBlank()) {
                        Text(text = title)
                    }
                },
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
    folds: Map<String, Boolean>,
    onFold: (String, Boolean) -> Unit,
    onCopyDay: (Int) -> Unit,
    onSelectDay: (Int) -> Unit,
    onAddExercise: () -> Unit,
    onMoreItemClick: (PlanItem) -> Unit,
    onMoreSupersetClick: (Int) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isCurrentDayBlank by remember(state.items) { derivedStateOf { state.items.isEmpty() } }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(listState) { key ->
        key is String && (key.startsWith("item-") || key.startsWith("superset-"))
    }
    // While a finger holds a row the screen keeps its own order; the plan hears about it once.
    var order by remember(state.items) {
        mutableStateOf(state.items.toPreviewBlocks())
    }
    var copyingDay by remember { mutableStateOf(false) }
    if (copyingDay) {
        CopyDaySheet(
            currentDay = state.currentDay,
            dayCount = state.dayCount,
            onPick = { day ->
                copyingDay = false
                onCopyDay(day)
            },
            onDismiss = { copyingDay = false },
        )
    }
    PlanExercise(
        modifier = Modifier.fillMaxSize(),
        state = listState,
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
                DaySummaryRow(items = state.items, onCopyDay = { copyingDay = true })
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
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            } else {
                order.forEachIndexed { index, block ->
                    when (block) {
                        is SessionBlock.SingleExercise -> {
                            val item = block.plan ?: return@forEachIndexed
                            val key = "item-${item.id}"
                            item(key = key) {
                                PlanExerciseBlock(
                                    modifier = Modifier
                                        .animateItem()
                                        .reorderableItem(reorderState, key),
                                    item = item,
                                    number = index + 1,
                                    chains = block.chains,
                                    expanded = folds[key] != true,
                                    onToggleExpand = { onFold(key, folds[key] != true) },
                                    onMoreClick = {
                                        focusManager.clearFocus()
                                        onMoreItemClick(item)
                                    },
                                    handleModifier = Modifier.reorderHandle(
                                        state = reorderState,
                                        key = key,
                                        onMove = { from, to ->
                                            order = order.movedBetween(from, to)
                                        },
                                        onDropped = { onReorder(order.itemIds()) },
                                    ),
                                )
                            }
                        }

                        is SessionBlock.Superset -> {
                            val key = "superset-${block.id}"
                            item(key = key) {
                                val open = folds[key] != true
                                val supersetActions: @Composable RowScope.() -> Unit = {
                                    Icon(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .reorderHandle(
                                                state = reorderState,
                                                key = key,
                                                onMove = { from, to ->
                                                    order = order.movedBetween(from, to)
                                                },
                                                onDropped = { onReorder(order.itemIds()) },
                                            ),
                                        painter = KenkoIcons.Drag,
                                        contentDescription = stringResource(
                                            R.string.label_reorder,
                                        ),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    IconButton(onClick = { onMoreSupersetClick(block.id) }) {
                                        Icon(
                                            painter = KenkoIcons.More,
                                            contentDescription = stringResource(
                                                R.string.label_exercise_actions,
                                            ),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .animateItem()
                                        .reorderableItem(reorderState, key),
                                ) {
                                    AnimatedContent(
                                        targetState = open,
                                        label = "superset",
                                        transitionSpec = {
                                            fadeIn() togetherWith fadeOut() using
                                                SizeTransform(clip = false)
                                        },
                                    ) { isOpen ->
                                    if (isOpen) {
                                        SupersetCard(
                                            block = block,
                                            isEditable = true,
                                            isPlan = true,
                                            onCollapse = { onFold(key, true) },
                                            onCloseRound = {},
                                            onUndo = {},
                                            actions = supersetActions,
                                        )
                                    } else {
                                        SupersetRow(
                                            block = block,
                                            number = index + 1,
                                            isPlan = true,
                                            onExpand = { onFold(key, false) },
                                            actions = supersetActions,
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                DashedAddButton(
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    label = stringResource(R.string.label_add_exercise_short),
                    onClick = onAddExercise,
                )
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
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
) {
    val chain = chains.firstOrNull()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
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
                        "${weightText(item.targetWeight)} ${unitLabel()} · " +
                        stringResource(R.string.label_rest_short, formatRest(item.restSeconds)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Icon(
                modifier = handleModifier.padding(end = 4.dp),
                painter = KenkoIcons.Drag,
                contentDescription = stringResource(R.string.label_reorder),
                tint = MaterialTheme.colorScheme.outline,
            )
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = KenkoIcons.More,
                    contentDescription = stringResource(R.string.label_exercise_actions),
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
        }
    }
}

/**
 * Days of a plan are just numbers: one, two, three, and a new one whenever it is needed.
 * In week mode the same number reads as a day of the week.
 *
 * They stand in a row, so the whole plan is visible at once and any day is one tap away —
 * the two arrows they replaced showed one day and hid the rest.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayHeader(
    day: Int,
    dayCount: Int,
    isWeekMode: Boolean,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastDay = if (isWeekMode) 7 else maxOf(dayCount, day)
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (number in 1..lastDay) {
            DayPill(
                label = if (isWeekMode) {
                    kenkoDayName(DayOfWeek(number))
                } else {
                    number.toString()
                },
                selected = number == day,
                onClick = { onSelectDay(number) },
            )
        }
        if (!isWeekMode) {
            DayPill(
                label = stringResource(R.string.label_add_day),
                selected = false,
                dashed = true,
                onClick = { onSelectDay(lastDay + 1) },
            )
        }
    }
}

@Composable
private fun DayPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Text(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .border(
                width = KenkoBorderWidth,
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    dashed -> MaterialTheme.colorScheme.outlineVariant
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        text = label,
        style = MaterialTheme.typography.titleMedium.numbers(),
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/**
 * How much work the day holds, right under its title.
 */
@Composable
private fun DaySummaryRow(
    items: List<PlanItem>,
    onCopyDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = remember(items) { items.daySummary() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = stringResource(
                R.string.label_day_summary,
                pluralStringResource(
                    R.plurals.plural_exercises,
                    summary.exercises,
                    summary.exercises,
                ),
                pluralStringResource(R.plurals.plural_sets, summary.sets, summary.sets),
                summary.minutes,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        // A cycle is usually built from a day that already exists.
        if (items.isNotEmpty()) {
            TextButton(onClick = onCopyDay) {
                Text(text = stringResource(R.string.label_copy_day))
            }
        }
    }
}

/**
 * Where the day should land: any other day of the plan, the next one included.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyDaySheet(
    currentDay: Int,
    dayCount: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            for (day in 1..(dayCount + 1)) {
                if (day == currentDay) continue
                SheetAction(
                    text = stringResource(R.string.label_copy_day_to, day),
                    onClick = { onPick(day) },
                )
            }
        }
    }
}

/**
 * The day as the finger left it: the row that was held moves in front of the one it landed on.
 */
private fun List<SessionBlock>.movedBetween(from: Any, to: Any): List<SessionBlock> {
    val fromIndex = indexOfFirst { it.reorderKey() == from }
    val toIndex = indexOfFirst { it.reorderKey() == to }
    if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

private fun SessionBlock.reorderKey(): String = when (this) {
    is SessionBlock.SingleExercise -> "item-${plan?.id}"
    is SessionBlock.Superset -> "superset-$id"
}

/**
 * Plan items of the day in the order the blocks stand in.
 */
private fun List<SessionBlock>.itemIds(): List<Long> = flatMap { block ->
    when (block) {
        is SessionBlock.SingleExercise -> listOfNotNull(block.plan?.id)
        is SessionBlock.Superset -> block.plan.mapNotNull { it.id }
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
