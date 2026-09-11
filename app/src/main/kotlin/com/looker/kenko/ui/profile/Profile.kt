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

package com.looker.kenko.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.MuscleLoad
import com.looker.kenko.data.model.ProfileSummary
import com.looker.kenko.data.model.Record
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.HealthQuotes
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.weightText
import com.looker.kenko.ui.exercises.localizedExerciseName
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.profile.components.FrequencyGraph
import com.looker.kenko.ui.stats.formatVolume
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

private const val MINUTES_IN_HOUR = 60

@Composable
fun Profile(
    viewModel: ProfileViewModel,
    onBackPress: () -> Unit,
    onStatsClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onPlanClick: () -> Unit,
    onPlanEdit: (Int) -> Unit,
    onGymsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Profile(
        state = state,
        onBackPress = onBackPress,
        onStatsClick = onStatsClick,
        onRecordsClick = onRecordsClick,
        onSettingsClick = onSettingsClick,
        onPlanEdit = onPlanEdit,
        onPlanClick = onPlanClick,
        onExercisesClick = onExercisesClick,
        onGymsClick = onGymsClick,
        onHistoryClick = onHistoryClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Profile(
    state: ProfileUiState,
    onBackPress: () -> Unit,
    onStatsClick: () -> Unit = {},
    onRecordsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPlanClick: () -> Unit = {},
    onPlanEdit: (Int) -> Unit = {},
    onExercisesClick: () -> Unit = {},
    onGymsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.label_profile)) },
                navigationIcon = { BackButton(onBackPress) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(painter = KenkoIcons.Settings, contentDescription = null)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding + PaddingValues(horizontal = 16.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            LifterHeader(summary = state.summary)
            val record = state.summary.freshRecord
            if (record != null) {
                Spacer(Modifier.height(20.dp))
                FreshRecordCard(record = record, onClick = onRecordsClick)
            }
            Spacer(Modifier.height(24.dp))
            ActivitySection(activity = state.activity, streakWeeks = state.summary.streakWeeks)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.label_profile_plan),
                hint = planHint(state),
                onClick = if (state.isPlanAvailable) {
                    { onPlanEdit(state.planId) }
                } else {
                    onPlanClick
                },
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.title_stats),
                hint = statsHint(state),
                onClick = onStatsClick,
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.title_records),
                hint = recordsHint(record, state.today),
                onClick = onRecordsClick,
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.label_section_exercises),
                hint = stringResource(
                    R.string.label_profile_exercises_hint_FORMAT,
                    state.numberOfExercises,
                ),
                onClick = onExercisesClick,
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.label_gyms),
                hint = gymHint(state),
                onClick = onGymsClick,
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            ProfileRow(
                title = stringResource(R.string.label_profile_history),
                hint = historyHint(state),
                onClick = onHistoryClick,
            )
            HorizontalDivider(thickness = KenkoBorderWidth)
            Spacer(Modifier.height(28.dp))
            HealthQuotes(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The one line the screen exists for: how long the training has been going and how much of it
 * there already is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifterHeader(summary: ProfileSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        val first = summary.first
        Text(
            text = if (first == null) {
                stringResource(R.string.label_profile_empty_since)
            } else {
                stringResource(
                    R.string.label_profile_since_FORMAT,
                    formatDate(first, DateFormat.DayMonthYear),
                )
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = summary.sessions.toString(),
                style = MaterialTheme.typography.displayLarge.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = pluralStringResource(R.plurals.plural_sessions_word, summary.sessions),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = totalsLine(summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The last record beaten. Nothing else on the screen is worth opening the profile for.
 */
@Composable
private fun FreshRecordCard(
    record: Record,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.label_fresh_record),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = formatDate(record.date, DateFormat.DayMonth),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = localizedExerciseName(record.exercise.name, record.exercise.nameRu),
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = weightText(record.weight),
                    style = MaterialTheme.typography.displaySmall.numbers(),
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = stringResource(
                        R.string.label_profile_record_reps_FORMAT,
                        record.reps,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (record.previous > 0F) {
                Text(
                    text = stringResource(
                        R.string.label_profile_record_gain_FORMAT,
                        formatVolume(record.gain),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ActivitySection(
    activity: Map<Int, Int>,
    streakWeeks: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_activity),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            if (streakWeeks > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.plural_weeks_streak,
                        streakWeeks,
                        streakWeeks,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (activity.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FrequencyGraph(activity = activity) { EmptyPlot() }
        } else {
            EmptyPlot()
        }
    }
}

/**
 * A row that only leads somewhere: name, what is behind the door right now, an arrow.
 */
@Composable
private fun ProfileRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Icon(
                painter = KenkoIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyPlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.error_not_enough_activity),
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun totalsLine(summary: ProfileSummary): String {
    if (summary.sessions == 0) return stringResource(R.string.label_profile_empty_hint)
    val parts = buildList {
        if (summary.volume > 0F) add(formatVolume(summary.volume))
        if (summary.sets > 0) {
            add(pluralStringResource(R.plurals.plural_sets, summary.sets, summary.sets))
        }
        if (summary.minutes > 0) add(durationText(summary.minutes))
    }
    return parts.joinToString(separator = " · ")
}

@Composable
private fun durationText(minutes: Int): String = if (minutes >= MINUTES_IN_HOUR) {
    val hours = minutes / MINUTES_IN_HOUR
    val rest = minutes % MINUTES_IN_HOUR
    if (rest == 0) {
        stringResource(R.string.label_hours_value_FORMAT, hours)
    } else {
        stringResource(R.string.label_hours_minutes_FORMAT, hours, rest)
    }
} else {
    pluralStringResource(R.plurals.plural_minutes, minutes, minutes)
}

@Composable
private fun planHint(state: ProfileUiState): String {
    if (!state.isPlanAvailable) return stringResource(R.string.label_profile_plan_none)
    val days = state.planStat?.workDays ?: 0
    if (days == 0) return state.planName
    return stringResource(
        R.string.label_profile_two_parts_FORMAT,
        state.planName,
        pluralStringResource(R.plurals.plural_days, days, days),
    )
}

@Composable
private fun statsHint(state: ProfileUiState): String {
    val month = state.month
    if (month == null || month.isEmpty) return stringResource(R.string.label_profile_stats_empty)
    val volume = stringResource(
        R.string.label_profile_stats_hint_FORMAT,
        formatVolume(month.volume),
        formatDate(state.today, DateFormat.MonthOnly),
    )
    val top = month.heaviestMuscle ?: return volume
    return stringResource(
        R.string.label_profile_two_parts_FORMAT,
        volume,
        muscleShare(month, top),
    )
}

@Composable
private fun muscleShare(month: StatsSummary, load: MuscleLoad): String = stringResource(
    R.string.label_profile_muscle_share_FORMAT,
    stringResource(load.muscle.stringRes),
    (month.share(load) * 100).roundToInt(),
)

@Composable
private fun recordsHint(record: Record?, today: LocalDate): String {
    if (record == null) return stringResource(R.string.label_records_card_hint)
    return stringResource(
        R.string.label_profile_records_hint_FORMAT,
        localizedExerciseName(record.exercise.name, record.exercise.nameRu),
        weightText(record.weight),
        dayText(record.date, today),
    )
}

@Composable
private fun gymHint(state: ProfileUiState): String {
    val gym = state.gym ?: return stringResource(R.string.label_profile_gym_none)
    if (gym.exerciseCount == 0) return gym.name
    return stringResource(
        R.string.label_profile_two_parts_FORMAT,
        gym.name,
        pluralStringResource(R.plurals.plural_exercises, gym.exerciseCount, gym.exerciseCount),
    )
}

@Composable
private fun historyHint(state: ProfileUiState): String {
    val last = state.summary.last ?: return stringResource(R.string.label_profile_history_empty)
    val day = dayText(last, state.today)
    val minutes = state.summary.lastMinutes ?: return day
    if (minutes == 0) return day
    return stringResource(R.string.label_profile_two_parts_FORMAT, day, durationText(minutes))
}

/**
 * Dates close to today read better by name than by number.
 */
@Composable
private fun dayText(date: LocalDate, today: LocalDate): String {
    val daysAgo = date.toEpochDays().toInt().let { today.toEpochDays().toInt() - it }
    return when (daysAgo) {
        0 -> stringResource(R.string.label_today)
        1 -> stringResource(R.string.label_yesterday)
        else -> formatDate(date, DateFormat.DayMonth)
    }
}

private val previewSummary = ProfileSummary(
    sessions = 128,
    sets = 1840,
    volume = 412_000F,
    minutes = 5640,
    first = LocalDate(2025, 7, 14),
    last = LocalDate(2026, 9, 10),
    lastMinutes = 72,
    streakWeeks = 6,
)

@Preview
@Composable
private fun ProfilePreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Profile(
            state = ProfileUiState(
                numberOfExercises = 876,
                isPlanAvailable = true,
                planId = 1,
                planName = "Push-Pull-Leg",
                activity = mapOf(20_340 to 2, 20_342 to 1, 20_345 to 3),
                summary = previewSummary,
                today = LocalDate(2026, 9, 11),
            ),
            onBackPress = { },
        )
    }
}

@Preview
@Composable
private fun ProfileEmptyPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Profile(
            state = ProfileUiState(today = LocalDate(2026, 9, 11)),
            onBackPress = { },
        )
    }
}
