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

package com.looker.kenko.ui.sessionDetail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.SessionBlock
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.SetChain
import com.looker.kenko.data.model.formatSeconds
import com.looker.kenko.ui.addSet.AddSet
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.OnSurfaceVariantBorder
import com.looker.kenko.ui.components.PrimaryBorder
import com.looker.kenko.ui.components.SwipeToDeleteBox
import com.looker.kenko.ui.components.TypingText
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.planEdit.components.dayName
import com.looker.kenko.ui.selectExercise.SelectExercise
import com.looker.kenko.ui.sessionDetail.components.AddDropRow
import com.looker.kenko.ui.sessionDetail.components.DropRow
import com.looker.kenko.ui.sessionDetail.components.DropSetCard
import com.looker.kenko.ui.sessionDetail.components.DropSetRow
import com.looker.kenko.ui.sessionDetail.components.SetItem
import com.looker.kenko.ui.sessionDetail.components.SupersetCard
import com.looker.kenko.ui.sessionDetail.components.SupersetRow
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun SessionDetails(
    viewModel: SessionDetailViewModel,
    onBackPress: () -> Unit,
    onHistoryClick: (LocalDate) -> Unit,
    onEditPlanClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    SessionDetail(
        state = state,
        rest = rest,
        onRestShift = viewModel::shiftRest,
        onRestStop = viewModel::stopRest,
        onBackPress = onBackPress,
        onEditPlanClick = onEditPlanClick,
        onRemoveSet = viewModel::removeSet,
        onReferenceClick = viewModel::openReference,
        onAddSetClick = { exercise, supersetId ->
            viewModel.showAddSetSheet(exercise, supersetId)
        },
        groupActions = GroupActions(
            onDropsChange = viewModel::setDropCount,
            onPercentChange = viewModel::setDropPercent,
            onMarkStep = viewModel::markDropStep,
            onMarkGroup = viewModel::markDropGroup,
            onUndoDrops = viewModel::undoDrops,
            onCloseRound = viewModel::closeSupersetRound,
            onUndoRound = viewModel::undoSupersetRound,
        ),
        onAddDropClick = viewModel::showAddDropSheet,
        onAddExerciseClick = viewModel::showExercisePicker,
        onHistoryClick = { onHistoryClick(viewModel.previousSessionDate) },
    )
    val pickerVisible by viewModel.exercisePickerVisible.collectAsStateWithLifecycle()
    if (pickerVisible) {
        SelectExercise(
            modifier = Modifier.fillMaxSize(),
            onBackPress = viewModel::hideExercisePicker,
            onRequestNewExercise = { _, _ -> },
            onDone = { exercise ->
                viewModel.hideExercisePicker()
                viewModel.showAddSetSheet(exercise)
            },
        )
    }
    val sheetTarget by viewModel.sheetTarget.collectAsStateWithLifecycle()
    sheetTarget?.let { sheet ->
        AddSetSheet(
            sheet = sheet,
            onDismiss = viewModel::hideSheet,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetail(
    state: SessionDetailState,
    rest: RestUiState? = null,
    onRestShift: (Int) -> Unit = {},
    onRestStop: () -> Unit = {},
    onBackPress: () -> Unit = {},
    onEditPlanClick: (Int) -> Unit = {},
    onRemoveSet: (Int?) -> Unit = {},
    onReferenceClick: (String) -> Unit = {},
    onAddSetClick: (Exercise, Int?) -> Unit = { _, _ -> },
    onAddDropClick: (SetChain) -> Unit = {},
    onAddExerciseClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    groupActions: GroupActions = GroupActions(),
) {
    when (state) {
        is SessionDetailState.Error -> {
            Column(Modifier.statusBarsPadding()) {
                TopAppBar(
                    navigationIcon = {
                        BackButton(onClick = onBackPress)
                    },
                    title = {},
                )
                SessionError(
                    title = stringResource(state.title),
                    message = stringResource(state.errorMessage),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SessionDetailState.Loading -> {
            Column(Modifier.statusBarsPadding()) {
                TopAppBar(
                    navigationIcon = {
                        BackButton(onClick = onBackPress)
                    },
                    title = {},
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is SessionDetailState.Success -> {
            val data = state.data
            Box(modifier = Modifier.fillMaxSize()) {
            SetsList(
                date = data.date,
                blocks = data.blocks,
                planId = data.planId,
                isEditable = data.isToday,
                hasPreviousSession = data.hasPreviousSession,
                onBackPress = onBackPress,
                onEditPlanClick = onEditPlanClick,
                onRemoveSet = onRemoveSet,
                onReferenceClick = onReferenceClick,
                onAddSetClick = onAddSetClick,
                onAddDropClick = onAddDropClick,
                onAddExerciseClick = onAddExerciseClick,
                onHistoryClick = onHistoryClick,
                groupActions = groupActions,
            )
            if (rest != null) {
                RestBar(
                    rest = rest,
                    onShift = onRestShift,
                    onStop = onRestStop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            }
        }
    }
}

private const val REST_SHIFT_SECONDS = 30

/**
 * Rest between sets: the clock is the whole point, so it gets the biggest type on the screen.
 */
@Composable
private fun RestBar(
    rest: RestUiState,
    onShift: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(rest.isDone) {
        if (rest.isDone) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (rest.isDone) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (rest.isDone) PrimaryBorder else OnSurfaceVariantBorder,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.label_rest).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = formatSeconds(rest.secondsLeft),
                style = MaterialTheme.typography.displayMedium.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = rest.exerciseName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onShift(-REST_SHIFT_SECONDS) }) {
                    Text(text = "-$REST_SHIFT_SECONDS")
                }
                TextButton(onClick = { onShift(REST_SHIFT_SECONDS) }) {
                    Text(text = "+$REST_SHIFT_SECONDS")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onStop) {
                    Text(
                        text = stringResource(
                            if (rest.isDone) R.string.label_done else R.string.label_skip_rest,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetsList(
    date: LocalDate,
    blocks: List<SessionBlock>,
    planId: Int?,
    isEditable: Boolean,
    hasPreviousSession: Boolean,
    onBackPress: () -> Unit,
    onEditPlanClick: (Int) -> Unit,
    onRemoveSet: (Int?) -> Unit,
    onReferenceClick: (String) -> Unit,
    onAddSetClick: (Exercise, Int?) -> Unit,
    onAddDropClick: (SetChain) -> Unit,
    onAddExerciseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    groupActions: GroupActions,
) {
    val expanded = rememberSaveable(saver = expandedSaver) { mutableStateMapOf() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(360.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        contentPadding = WindowInsets.navigationBars.asPaddingValues(LocalDensity.current) +
                PaddingValues(bottom = 12.dp),
    ) {

        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Header(
                performedOn = date,
                onBackPress = onBackPress,
                actions = {
                    if (hasPreviousSession) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(
                                painter = KenkoIcons.History,
                                contentDescription = null,
                            )
                        }
                    }
                    if (planId != null) {
                        IconButton(onClick = { onEditPlanClick(planId) }) {
                            Icon(
                                painter = KenkoIcons.Rename,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        }
        if (isEditable && blocks.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    text = stringResource(R.string.label_free_session_hint),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is SessionBlock.SingleExercise -> singleExerciseBlock(
                    block = block,
                    isEditable = isEditable,
                    expanded = expanded,
                    onRemoveSet = onRemoveSet,
                    onReferenceClick = onReferenceClick,
                    onAddSetClick = onAddSetClick,
                    groupActions = groupActions,
                )

                is SessionBlock.Superset -> supersetBlock(
                    block = block,
                    number = blockIndex + 1,
                    isEditable = isEditable,
                    expanded = expanded,
                    onAddSetClick = onAddSetClick,
                    groupActions = groupActions,
                )
            }
        }
        if (isEditable) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DashedAddButton(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    label = stringResource(R.string.label_add_exercise_short),
                    onClick = onAddExerciseClick,
                    accent = true,
                )
            }
        }
    }
}

/**
 * What a group block can do. Kept together so the list does not grow a tail of callbacks.
 */
@Immutable
data class GroupActions(
    val onDropsChange: (SetChain, Int) -> Unit = { _, _ -> },
    val onPercentChange: (SetChain, Int) -> Unit = { _, _ -> },
    val onMarkStep: (SetChain, Int) -> Unit = { _, _ -> },
    val onMarkGroup: (SetChain) -> Unit = {},
    val onUndoDrops: (SetChain) -> Unit = {},
    val onCloseRound: (SessionBlock.Superset) -> Unit = {},
    val onUndoRound: (SessionBlock.Superset) -> Unit = {},
)

private val expandedSaver = listSaver<SnapshotStateMap<String, Boolean>, String>(
    save = { map -> map.filterValues { it }.keys.toList() },
    restore = { keys ->
        mutableStateMapOf<String, Boolean>().apply {
            keys.forEach { key -> put(key, true) }
        }
    },
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyGridScope.singleExerciseBlock(
    block: SessionBlock.SingleExercise,
    isEditable: Boolean,
    expanded: SnapshotStateMap<String, Boolean>,
    onRemoveSet: (Int?) -> Unit,
    onReferenceClick: (String) -> Unit,
    onAddSetClick: (Exercise, Int?) -> Unit,
    groupActions: GroupActions,
) {
    val exercise = block.exercise
    item(
        span = { GridItemSpan(maxLineSpan) },
    ) {
        StickyHeader(
            name = exercise.displayName(),
            subtitle = block.plan?.let { plan ->
                stringResource(
                    R.string.label_sets_left_range,
                    pluralStringResource(R.plurals.plural_sets, block.setsLeft, block.setsLeft),
                    plan.repsLabel,
                )
            },
        ) {
            if (!exercise.reference.isNullOrBlank()) {
                FilledTonalIconButton(onClick = { onReferenceClick(exercise.reference) }) {
                    Icon(painter = KenkoIcons.Lightbulb, contentDescription = null)
                }
            }
            if (isEditable) {
                FilledTonalIconButton(
                    shapes = IconButtonShapes(
                        shape = MaterialShapes.Circle.toShape(),
                        pressedShape = MaterialShapes.Cookie6Sided.toShape(),
                    ),
                    onClick = { onAddSetClick(exercise, null) },
                ) {
                    Icon(painter = KenkoIcons.Add, contentDescription = null)
                }
            }
        }
    }
    itemsIndexed(
        items = block.chains,
        span = { _, _ -> GridItemSpan(maxLineSpan) },
    ) { index, chain ->
        val number = index + 1
        val key = "drop-${chain.set.id}"
        if (chain.isDropSet) {
            val isOpen = expanded[key] ?: (isEditable && chain.performedSteps <= chain.set.dropCount)
            if (isOpen) {
                DropSetCard(
                    modifier = Modifier.animateItem(),
                    chain = chain,
                    number = number,
                    isEditable = isEditable,
                    onCollapse = { expanded[key] = false },
                    onDropsChange = { groupActions.onDropsChange(chain, it) },
                    onPercentChange = { groupActions.onPercentChange(chain, it) },
                    onMarkStep = { groupActions.onMarkStep(chain, it) },
                    onMarkGroup = { groupActions.onMarkGroup(chain) },
                    onUndo = { groupActions.onUndoDrops(chain) },
                )
            } else {
                DropSetRow(
                    modifier = Modifier.animateItem(),
                    chain = chain,
                    number = number,
                    onExpand = { expanded[key] = true },
                )
            }
        } else {
            ChainItem(
                chain = chain,
                number = number,
                onRemoveSet = onRemoveSet,
                modifier = Modifier.animateItem(),
            )
        }
    }
    val plan = block.plan
    if (isEditable && plan != null && block.setsLeft > 0) {
        items(
            count = block.setsLeft,
            span = { GridItemSpan(maxLineSpan) },
        ) { index ->
            PlannedSetRow(
                number = block.chains.size + index + 1,
                reps = plan.repsLabel,
                onClick = { onAddSetClick(exercise, null) },
            )
        }
    }
    if (block.chains.isNotEmpty()) {
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            VolumeCard(block = block)
        }
    }
}

private fun LazyGridScope.supersetBlock(
    block: SessionBlock.Superset,
    number: Int,
    isEditable: Boolean,
    expanded: SnapshotStateMap<String, Boolean>,
    onAddSetClick: (Exercise, Int?) -> Unit,
    groupActions: GroupActions,
) {
    item(
        span = { GridItemSpan(maxLineSpan) },
    ) {
        val key = "superset-${block.id}"
        val isOpen = expanded[key] ?: (isEditable && block.roundsLeft > 0)
        if (isOpen) {
            SupersetCard(
                block = block,
                isEditable = isEditable,
                onCollapse = { expanded[key] = false },
                onCloseRound = { groupActions.onCloseRound(block) },
                onUndo = { groupActions.onUndoRound(block) },
            )
        } else {
            SupersetRow(
                block = block,
                number = number,
                onExpand = { expanded[key] = true },
            )
        }
    }
    if (isEditable) {
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.exercises.forEach { exercise ->
                    TextButton(onClick = { onAddSetClick(exercise, block.id) }) {
                        Text(text = exercise.displayName())
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = KenkoIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * What the exercise added up to, drop sets counted separately.
 */
@Composable
private fun VolumeCard(
    block: SessionBlock.SingleExercise,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(OnSurfaceVariantBorder, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(R.string.label_exercise_volume),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(R.string.label_volume_kg, block.volume.roundToInt()),
                style = MaterialTheme.typography.headlineMedium.numbers(),
            )
        }
        if (block.dropVolume > 0F) {
            Text(
                text = stringResource(R.string.label_of_them_drop, block.dropVolume.roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ChainItem(
    chain: SetChain,
    number: Int,
    onRemoveSet: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeToDeleteBox(
        modifier = modifier,
        onDismiss = { onRemoveSet(chain.set.id) },
    ) {
        SetItem(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            set = chain.set,
            title = { Text(text = normalizeInt(number)) },
        )
    }
}

/**
 * A set the plan asks for but nobody has done yet: same shape as a performed set, drawn hollow.
 */
@Composable
private fun PlannedSetRow(
    number: Int?,
    reps: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    name: String? = null,
) {
    Row(
        modifier = modifier
            .widthIn(240.dp, 420.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.large)
            .border(OnSurfaceVariantBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name ?: normalizeInt(number ?: 0),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "$reps ×",
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun RoundLabel(number: Int) {
    Text(
        text = stringResource(R.string.label_round_number, number).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    performedOn: LocalDate,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit),
) {
    val date = remember {
        formatDate(performedOn, DateFormat.SessionLabel)
    }
    TopAppBar(
        modifier = modifier,
        actions = actions,
        navigationIcon = { BackButton(onClick = onBackPress) },
        title = {
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                var startAnimatingDate by remember {
                    mutableStateOf(false)
                }
                TypingText(
                    text = dayName(performedOn.dayOfWeek),
                    onCompleteListener = {
                        startAnimatingDate = true
                    },
                )
                TypingText(
                    text = date,
                    startTyping = startAnimatingDate,
                    initialDelay = 0.milliseconds,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },

    )
}

@Composable
private fun StickyHeader(
    name: String,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(24.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (actions != null) {
                actions()
            }
        }
    }
}

@Composable
private fun SessionError(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSetSheet(
    sheet: SetSheetTarget,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = state,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        AddSet(
            exerciseName = sheet.exerciseName,
            target = sheet.target,
            onDone = {
                scope.launch { state.hide() }.invokeOnCompletion {
                    if (!state.isVisible) onDismiss()
                }
            },
        )
    }
}

@Preview
@Composable
private fun SessionDetailPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        val data = remember {
            SessionDetailState.Success(
                SessionUiData(
                    date = LocalDate(2024, 4, 15),
                    blocks = emptyList(),
                    isToday = true,
                ),
            )
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            SessionDetail(state = data)
        }
    }
}

@Preview
@Composable
private fun SessionErrorPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        val data = remember {
            SessionDetailState.Error.InvalidSession
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            SessionDetail(state = data)
        }
    }
}
