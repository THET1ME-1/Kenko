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

package com.looker.kenko.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.SessionGroupKey
import com.looker.kenko.data.model.SessionGrouping
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.EmptyPage
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.planEdit.components.dayName
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import com.looker.kenko.utils.isToday
import kotlinx.datetime.LocalDate

@Composable
fun Sessions(
    viewModel: SessionsViewModel,
    onSessionClick: (LocalDate) -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Sessions(
        state = state,
        snackbarState = viewModel.snackbarState,
        onSessionClick = onSessionClick,
        onGroupingChange = viewModel::setGrouping,
        onSessionMove = viewModel::moveSession,
        onSessionRemove = viewModel::removeSession,
        onBackPress = onBackPress,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Sessions(
    state: SessionsUiData,
    onSessionClick: (LocalDate) -> Unit,
    onGroupingChange: (SessionGrouping) -> Unit,
    onBackPress: () -> Unit,
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
    onSessionMove: (Session, LocalDate) -> Unit = { _, _ -> },
    onSessionRemove: (Session) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // A long press opens what a written session can still do: move to the right day or go away.
    var acting by remember { mutableStateOf<Session?>(null) }
    var moving by remember { mutableStateOf<Session?>(null) }
    var deleting by remember { mutableStateOf<Session?>(null) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onClick = onBackPress)
                },
                title = {
                    Text(text = stringResource(id = R.string.label_sessions_title))
                },
                actions = {
                    GroupingMenu(
                        grouping = state.grouping,
                        onGroupingChange = onGroupingChange,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        if (state.isEmpty) {
            EmptyPage(stringResource(id = R.string.label_no_sessions))
        } else {
            // The newest group opens by itself: a year of training is otherwise a wall of headers.
            val isOpen = remember(state.grouping) { mutableStateMapOf<String, Boolean>() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding + PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                state.groups.forEachIndexed { index, group ->
                    val id = group.key.toString()
                    val open = isOpen[id] ?: (index == 0)
                    if (group.key != SessionGroupKey.All) {
                        item(key = "header-$id") {
                            GroupHeader(
                                key = group.key,
                                count = group.sessions.size,
                                isOpen = open,
                                onClick = { isOpen[id] = !open },
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                    }
                    if (open || group.key == SessionGroupKey.All) {
                        items(
                            items = group.sessions,
                            key = { it.id ?: it.date.toEpochDays() },
                        ) { session ->
                            SessionCard(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                session = session,
                                onClick = { onSessionClick(session.date) },
                                onLongClick = { acting = session },
                            )
                        }
                    }
                }
            }
        }
    }
    acting?.let { session ->
        SessionActions(
            onMove = {
                acting = null
                moving = session
            },
            onDelete = {
                acting = null
                deleting = session
            },
            onDismiss = { acting = null },
        )
    }
    moving?.let { session ->
        MoveSessionDialog(
            session = session,
            onPick = { date ->
                moving = null
                onSessionMove(session, date)
            },
            onDismiss = { moving = null },
        )
    }
    deleting?.let { session ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(text = stringResource(R.string.label_delete_session)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.label_delete_session_question,
                        formatDate(session.date, DateFormat.SessionLabel),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSessionRemove(session)
                        deleting = null
                    },
                ) {
                    Text(text = stringResource(R.string.label_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(text = stringResource(R.string.label_cancel))
                }
            },
        )
    }
}

/**
 * What can still be done to a session that is already written down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionActions(
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(
                modifier = Modifier.clickable(onClick = onMove),
                headlineContent = { Text(text = stringResource(R.string.label_move_session)) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onDelete),
                headlineContent = { Text(text = stringResource(R.string.label_delete_session)) },
                colors = ListItemDefaults.colors(
                    headlineColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

/**
 * The calendar for a session written on the wrong day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveSessionDialog(
    session: Session,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = session.date.toEpochDays() * MILLIS_IN_DAY,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton onDismiss()
                    onPick(LocalDate.fromEpochDays((millis / MILLIS_IN_DAY).toInt()))
                },
            ) {
                Text(text = stringResource(R.string.label_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.label_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

private const val MILLIS_IN_DAY = 86_400_000L

/**
 * Header of one group: what it holds and how many sessions are inside.
 */
@Composable
private fun GroupHeader(
    key: SessionGroupKey,
    count: Int,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isOpen) 90F else 0F),
                painter = KenkoIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                modifier = Modifier.weight(1F),
                text = groupTitle(key),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * The group in words: a month with its year, a year, or a day of the plan.
 */
@Composable
private fun groupTitle(key: SessionGroupKey): String = when (key) {
    SessionGroupKey.All -> ""
    is SessionGroupKey.Month -> formatDate(
        LocalDate(key.year, key.month, 1),
        DateFormat.MonthYear,
    ).replaceFirstChar { it.uppercase() }

    is SessionGroupKey.Year -> key.year.toString()
    is SessionGroupKey.PlanDay -> key.index
        ?.let { stringResource(R.string.label_plan_day, it) }
        ?: stringResource(R.string.label_sessions_without_day)
}

/**
 * How the log is cut: the choice lives in settings, so the screen opens the way it was left.
 */
@Composable
private fun GroupingMenu(
    grouping: SessionGrouping,
    onGroupingChange: (SessionGrouping) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { isExpanded = true }) {
        Icon(
            imageVector = KenkoIcons.Stack,
            contentDescription = stringResource(R.string.label_sessions_grouping),
        )
    }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
    ) {
        SessionGrouping.entries.forEach { entry ->
            DropdownMenuItem(
                text = { Text(text = groupingName(entry)) },
                trailingIcon = {
                    RadioButton(
                        selected = entry == grouping,
                        onClick = {
                            onGroupingChange(entry)
                            isExpanded = false
                        },
                    )
                },
                onClick = {
                    onGroupingChange(entry)
                    isExpanded = false
                },
            )
        }
    }
}

@Composable
private fun groupingName(grouping: SessionGrouping): String = when (grouping) {
    SessionGrouping.None -> stringResource(R.string.label_grouping_none)
    SessionGrouping.Month -> stringResource(R.string.label_grouping_month)
    SessionGrouping.Year -> stringResource(R.string.label_grouping_year)
    SessionGrouping.PlanDay -> stringResource(R.string.label_grouping_plan_day)
}

@Composable
fun SessionCard(
    session: Session,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val containerColor = if (session.date.isToday) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val containerShape = if (session.date.isToday) {
        CircleShape
    } else {
        MaterialTheme.shapes.extraLarge
    }
    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        color = containerColor,
        shape = containerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
        ) {
            val titleStyle = MaterialTheme.typography.titleLarge
            val secondaryEmphasis = MaterialTheme.colorScheme.outline
            val dayName = dayName(session.date.dayOfWeek)
            val string = remember(session.date, dayName) {
                buildAnnotatedString {
                    withStyle(titleStyle.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                        append(formatDate(session.date, dateTimeFormat = DateFormat.SessionLabel))
                    }
                    append(" ${Typography.bullet} ")
                    withStyle(titleStyle.toSpanStyle().copy(color = secondaryEmphasis)) {
                        append(dayName)
                    }
                }
            }
            Text(text = string)

            val exerciseNames = session.performExercises.map { it.displayName() }.joinToString()
            Text(
                text = exerciseNames,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 3,
            )
        }
    }
}

@Preview
@Composable
private fun SessionCardPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        SessionCard(
            session = Session(
                planId = 1,
                date = LocalDate(2024, 4, 15),
                sets = emptyList(),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun SessionsPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Sessions(
            state = SessionsUiData(),
            onBackPress = {},
            onSessionClick = {},
            onGroupingChange = {},
        )
    }
}
