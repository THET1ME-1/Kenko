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

package com.looker.kenko.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.LastTime
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import com.looker.kenko.ui.components.BodyHeatMap
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.LiftingQuotes
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.stats.formatVolume
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import kotlinx.datetime.LocalDate

@Composable
fun Home(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSelectPlanClick: () -> Unit,
    onExploreSessionsClick: () -> Unit,
    onAllPlansClick: () -> Unit,
    onStartSessionClick: (planId: Int?, dayIndex: Int?) -> Unit,
    onCurrentPlanClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weekLoad by viewModel.weekLoad.collectAsStateWithLifecycle()
    val picker by viewModel.picker.collectAsStateWithLifecycle()
    val isPickerVisible by viewModel.isPickerVisible.collectAsStateWithLifecycle()
    Home(
        state = state,
        weekLoad = weekLoad,
        onStatsClick = onStatsClick,
        onProfileClick = onProfileClick,
        onSelectPlanClick = onSelectPlanClick,
        onExploreSessionsClick = onExploreSessionsClick,
        onAllPlansClick = onAllPlansClick,
        onStartSessionClick = { onStartSessionClick(null, null) },
        onPickDayClick = viewModel::showPicker,
        onCurrentPlanClick = onCurrentPlanClick,
    )
    if (isPickerVisible) {
        PlanPickerSheet(
            picker = picker,
            todayDayIndex = state.dayIndex,
            onPlanClick = viewModel::showDaysOf,
            onDayClick = { planId, dayIndex ->
                viewModel.hidePicker()
                onStartSessionClick(planId, dayIndex)
            },
            onFreeSessionClick = {
                viewModel.hidePicker()
                onStartSessionClick(null, null)
            },
            onDismiss = viewModel::hidePicker,
        )
    }
}

@Composable
private fun Home(
    state: HomeUiData,
    weekLoad: WeekLoad = WeekLoad(),
    onStatsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSelectPlanClick: () -> Unit = {},
    onExploreSessionsClick: () -> Unit = {},
    onAllPlansClick: () -> Unit = {},
    onStartSessionClick: () -> Unit = {},
    onPickDayClick: () -> Unit = {},
    onCurrentPlanClick: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            KenkoTopBar {
                FilledTonalIconButton(onClick = onProfileClick) {
                    Icon(painter = KenkoIcons.Person, contentDescription = null)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            HorizontalDivider(thickness = KenkoBorderWidth)
            Row(
                modifier = Modifier
                    .align(CenterHorizontally)
                    .widthIn(240.dp, 420.dp)
                    .height(120.dp),
            ) {
                AllPlansCard(
                    onClick = onAllPlansClick,
                    modifier = Modifier.weight(1F),
                )
                VerticalDivider()
                SessionHistoryCard(
                    onClick = onExploreSessionsClick,
                    modifier = Modifier.weight(1F),
                )
            }
            HorizontalDivider(thickness = KenkoBorderWidth)
            Column(
                modifier = Modifier
                    .align(CenterHorizontally)
                    .widthIn(240.dp, 420.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TodayLine(
                    state = state,
                    onClick = { state.currentPlanId?.let(onCurrentPlanClick) },
                )
                LastTimeCard(
                    lastTime = state.lastTime,
                    exercisesToday = state.todayExercises,
                )
                KenkoButton(
                    modifier = Modifier.align(CenterHorizontally),
                    onClick = onStartSessionClick,
                    label = {
                        Text(text = startLabel(state))
                    },
                    icon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = KenkoIcons.ArrowOutward,
                            contentDescription = null,
                        )
                    },
                )
                Box(modifier = Modifier.align(CenterHorizontally)) {
                    if (state.isPlanSelected) {
                        TextButton(onClick = onPickDayClick) {
                            Text(text = stringResource(R.string.label_home_other_day))
                        }
                    } else {
                        TextButton(onClick = onSelectPlanClick) {
                            Text(text = stringResource(R.string.label_select_plan_one))
                        }
                    }
                }
            }
            WeekLoadCard(
                load = weekLoad,
                onClick = onStatsClick,
                modifier = Modifier
                    .align(CenterHorizontally)
                    .widthIn(240.dp, 420.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LiftingQuotes(Modifier.align(CenterHorizontally))
        }
    }
}

/**
 * What the button starts: today's day of the plan, a session already running, or a free run.
 */
@Composable
private fun startLabel(state: HomeUiData): String = when {
    state.isSessionStarted -> stringResource(R.string.label_continue_session)
    !state.isPlanSelected || state.isTodayEmpty -> stringResource(R.string.label_start_session)
    else -> stringResource(R.string.label_start_day, state.dayIndex)
}

/**
 * Which day of the plan is due, and how much is written into it.
 */
@Composable
private fun TodayLine(
    state: HomeUiData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPlan = state.isPlanSelected && !state.isTodayEmpty
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (hasPlan) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val content = if (hasPlan) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        if (hasPlan) {
            Text(
                text = "%02d".format(state.dayIndex),
                style = MaterialTheme.typography.headlineSmall.numbers(),
                color = content,
            )
        }
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = when {
                    !state.isPlanSelected -> stringResource(R.string.label_free_session_heading)
                    state.isTodayEmpty -> stringResource(R.string.label_nothing_today)
                    else -> state.planName.orEmpty()
                },
                style = MaterialTheme.typography.titleMedium,
                color = content,
                maxLines = 2,
            )
            if (hasPlan) {
                Text(
                    text = pluralStringResource(
                        R.plurals.plural_exercises,
                        state.todayExercises.size,
                        state.todayExercises.size,
                    ) + " " + Typography.bullet + " " + pluralStringResource(
                        R.plurals.plural_sets,
                        state.todaySets,
                        state.todaySets,
                    ),
                    style = MaterialTheme.typography.labelMedium.numbers(),
                    color = content,
                )
            }
        }
    }
}

/**
 * The number today has to beat: what the same day of the plan weighed last time.
 *
 * A day trained for the first time has nothing to race, so it shows what is ahead instead.
 */
@Composable
private fun LastTimeCard(
    lastTime: LastTime?,
    exercisesToday: List<Exercise>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (lastTime == null) {
            Text(
                text = stringResource(R.string.label_home_first_run).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = exercisesToday.take(4).map { it.displayName() }.joinToString()
                    .ifEmpty { stringResource(R.string.label_home_nothing_planned) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = stringResource(
                    R.string.label_home_last_time,
                    formatDate(lastTime.date, DateFormat.DayMonth),
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = formatVolume(lastTime.volume),
                style = MaterialTheme.typography.displaySmall.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = lastTime.topSets
                    .map { set ->
                        "${set.exercise.displayName()} ${set.weight.trimZero()}×${set.repsOrDuration}"
                    }
                    .joinToString(" ${Typography.bullet} "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Weight reads as `70`, not `70.0`, and keeps the half-kilo when there is one.
 */
private fun Float.trimZero(): String =
    if (this == toInt().toFloat()) toInt().toString() else toString()

/**
 * The running week in one card: where the load landed and how much of it there was.
 *
 * The body is the point — the numbers only say how big the week was.
 */
@Composable
private fun WeekLoadCard(
    load: WeekLoad,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(R.string.label_week_load).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = formatVolume(load.volume),
                style = MaterialTheme.typography.displaySmall.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.plural_sets, load.sets, load.sets))
                    if (load.streak > 0) {
                        append(" ${Typography.bullet} ")
                        append(
                            pluralStringResource(
                                R.plurals.plural_week_streak,
                                load.streak,
                                load.streak,
                            ),
                        )
                    }
                },
                style = MaterialTheme.typography.labelMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        BodyHeatMap(
            load = load.heat,
            height = 110.dp,
            modifier = Modifier.width(120.dp),
        )
    }
}

/**
 * The choice the app used to make alone: which plan and which of its days is being trained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanPickerSheet(
    picker: PlanPicker,
    todayDayIndex: Int,
    onPlanClick: (Int) -> Unit,
    onDayClick: (planId: Int?, dayIndex: Int?) -> Unit,
    onFreeSessionClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.label_home_pick_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (picker.plans.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    picker.plans.forEach { plan ->
                        val id = plan.id ?: return@forEach
                        PlanPill(
                            name = plan.name,
                            isSelected = id == picker.shownPlanId,
                            onClick = { onPlanClick(id) },
                        )
                    }
                }
            }
            picker.days.forEach { day ->
                DayRow(
                    day = day,
                    isToday = day.index == todayDayIndex,
                    onClick = { onDayClick(picker.shownPlanId, day.index) },
                )
            }
            if (picker.days.isEmpty()) {
                Text(
                    text = stringResource(R.string.label_home_plan_has_no_days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                modifier = Modifier.align(CenterHorizontally),
                onClick = onFreeSessionClick,
            ) {
                Text(text = stringResource(R.string.label_free_session_heading))
            }
        }
    }
}

@Composable
private fun PlanPill(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun DayRow(
    day: PlanDayOption,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isToday) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val content = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = "%02d".format(day.index),
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = content,
            )
            Text(
                modifier = Modifier.weight(1F),
                text = stringResource(R.string.label_start_day, day.index),
                style = MaterialTheme.typography.bodyLarge,
                color = content,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.plural_exercises,
                    day.exercises,
                    day.exercises,
                ),
                style = MaterialTheme.typography.labelMedium.numbers(),
                color = content,
            )
        }
    }
}

@Composable
private fun AllPlansCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HelperCards(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.label_all_plans))
        Icon(
            painter = KenkoIcons.ArrowOutward,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .align(TopEnd),
        )
    }
}

@Composable
private fun SessionHistoryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HelperCards(onClick = onClick, modifier = modifier) {
        Text(text = stringResource(R.string.label_session_history_home))
        Icon(
            painter = KenkoIcons.History,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .align(TopEnd),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KenkoTopBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_icon),
                    contentDescription = null,
                    modifier = Modifier.clip(CircleShape)
                )
                Text(
                    text = "KENKO",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        actions = actions,
        modifier = modifier,
    )
}

@Composable
private fun HelperCards(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        shape = shape,
        color = color,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ProvideTextStyle(textStyle) { content() }
        }
    }
}

private val previewExercise = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)

private fun previewSet(weight: Float, reps: Int) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = SetType.Standard,
    exercise = previewExercise,
    rir = RepsInReserve(2),
)

@Preview
@Composable
private fun HomePreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Home(
            state = HomeUiData(
                planName = "Push Pull Legs",
                dayIndex = 3,
                todayExercises = listOf(previewExercise),
                todaySets = 18,
                lastTime = LastTime(
                    date = LocalDate(2026, 9, 3),
                    volume = 4320F,
                    topSets = listOf(previewSet(80F, 8)),
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun FirstRunHomePreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Home(
            state = HomeUiData(
                planName = "Push Pull Legs",
                dayIndex = 1,
                todayExercises = listOf(previewExercise),
                todaySets = 12,
            ),
        )
    }
}

@Preview
@Composable
private fun NoPlanHomePreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Home(state = HomeUiData(isPlanSelected = false))
    }
}
