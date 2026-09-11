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

package com.looker.kenko.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.looker.kenko.ui.components.RowAction
import com.looker.kenko.ui.components.ActionsSheet
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.ExercisesPreviewParameter
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.ErrorSnackbar
import com.looker.kenko.ui.components.ExerciseCard
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.LazyTargets
import com.looker.kenko.ui.components.SecondaryKenkoButton
import com.looker.kenko.ui.components.EmptyPage
import com.looker.kenko.ui.components.kenkoTextFieldColor
import com.looker.kenko.ui.components.SwipeToDeleteBox
import com.looker.kenko.ui.components.TargetChip
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter

@Composable
fun Exercises(
    viewModel: ExercisesViewModel,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: (target: MuscleGroups?) -> Unit,
    onAddToSessionClick: (Exercise) -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.exercises.collectAsStateWithLifecycle()
    val plan by viewModel.currentPlan.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<Exercise?>(null) }
    var planDayFor by remember { mutableStateOf<Exercise?>(null) }
    Exercises(
        state = state,
        query = viewModel.searchQuery,
        onQueryChange = viewModel::setSearch,
        snackbarState = viewModel.snackbarState,
        onBackPress = onBackPress,
        onExerciseClick = onExerciseClick,
        onCreateClick = onCreateClick,
        onSelectTarget = viewModel::setTarget,
        onReferenceClick = viewModel::onReferenceClick,
        onRemove = viewModel::removeExercise,
        onMoreClick = { actionsFor = it },
    )
    actionsFor?.let { exercise ->
        val inGym = exercise.id in state.gymExerciseIds
        ActionsSheet(
            title = exercise.displayName(),
            subtitle = stringResource(exercise.target.stringRes),
            actions = buildList {
                add(
                    RowAction(label = stringResource(R.string.label_open_exercise)) {
                        actionsFor = null
                        onExerciseClick(exercise.id)
                    },
                )
                val gymName = state.gymName
                if (gymName != null) {
                    add(
                        RowAction(
                            label = if (inGym) {
                                stringResource(R.string.label_remove_from_gym_FORMAT, gymName)
                            } else {
                                stringResource(R.string.label_add_to_gym_FORMAT, gymName)
                            },
                        ) {
                            actionsFor = null
                            viewModel.toggleInGym(exercise)
                        },
                    )
                }
                add(
                    RowAction(label = stringResource(R.string.label_add_to_session)) {
                        actionsFor = null
                        onAddToSessionClick(exercise)
                    },
                )
                plan?.let { target ->
                    add(
                        RowAction(
                            label = stringResource(R.string.label_add_to_plan),
                            hint = target.name,
                        ) {
                            actionsFor = null
                            planDayFor = exercise
                        },
                    )
                }
                add(
                    RowAction(label = stringResource(R.string.label_duplicate_exercise)) {
                        actionsFor = null
                        viewModel.duplicate(exercise)
                    },
                )
                add(
                    RowAction(
                        label = stringResource(R.string.label_remove),
                        isDanger = true,
                    ) {
                        actionsFor = null
                        viewModel.removeExercise(exercise.id)
                    },
                )
            },
            onDismiss = { actionsFor = null },
        )
    }
    planDayFor?.let { exercise ->
        val target = plan
        if (target == null) {
            planDayFor = null
        } else {
            ActionsSheet(
                title = exercise.displayName(),
                subtitle = target.name,
                actions = (1..maxOf(target.dayCount, 1) + 1).map { day ->
                    RowAction(
                        label = stringResource(R.string.label_add_to_plan_day_FORMAT, day),
                    ) {
                        planDayFor = null
                        viewModel.addToPlanDay(exercise, day)
                    }
                },
                onDismiss = { planDayFor = null },
            )
        }
    }
}

@Composable
private fun Exercises(
    state: ExercisesUiState,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    snackbarState: SnackbarHostState,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: (target: MuscleGroups?) -> Unit,
    onSelectTarget: (MuscleGroups?) -> Unit,
    onRemove: (Int?) -> Unit,
    onBackPress: () -> Unit,
    onReferenceClick: (String) -> Unit,
    onMoreClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxWidth(),
        floatingActionButton = {
            SecondaryKenkoButton(
                onClick = { onCreateClick(state.selected) },
                label = {
                    Text(stringResource(R.string.label_create_exercise))
                },
                icon = {
                    Icon(
                        painter = KenkoIcons.Add,
                        contentDescription = null,
                    )
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) {
                ErrorSnackbar(data = it)
            }
        },
        topBar = {
            Header(
                target = state.selected,
                query = query,
                onQueryChange = onQueryChange,
                onSelect = onSelectTarget,
                onBackPress = onBackPress,
            )
        },
    ) { innerPadding ->
        if (state.exercises.isEmpty()) {
            EmptyPage(
                text = stringResource(R.string.label_exercise_not_found),
                modifier = Modifier.padding(innerPadding),
                hero = {},
            )
        } else {
            ExercisesList(
                exercises = state.exercises,
                contentPadding = innerPadding + PaddingValues(bottom = 80.dp),
                onExerciseClick = onExerciseClick,
                onReferenceClick = onReferenceClick,
                onRemove = onRemove,
                onMoreClick = onMoreClick,
            )
        }
    }
}

@Composable
private fun ExercisesList(
    exercises: List<Exercise>,
    contentPadding: PaddingValues,
    onExerciseClick: (id: Int?) -> Unit,
    onRemove: (Int?) -> Unit,
    onReferenceClick: (String) -> Unit,
    onMoreClick: (Exercise) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(exercises, key = { it.id!! }) { exercise ->
            val exerciseId by rememberUpdatedState(exercise.id)
            SwipeToDeleteBox(
                modifier = Modifier.animateItem(),
                onDismiss = { onRemove(exerciseId) }
            ) {
                ExerciseCard(
                    exercise = exercise,
                    onClick = { onExerciseClick(exerciseId) },
                    trailing = {
                        if (exercise.reference != null) {
                            FilledTonalIconButton(
                                modifier = Modifier.size(48.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                onClick = { onReferenceClick(exercise.reference) }
                            ) {
                                Icon(painter = KenkoIcons.Lightbulb, contentDescription = null)
                            }
                        }
                        IconButton(onClick = { onMoreClick(exercise) }) {
                            Icon(
                                painter = KenkoIcons.More,
                                contentDescription = stringResource(
                                    R.string.label_exercise_actions,
                                ),
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    target: MuscleGroups?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (MuscleGroups?) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.label_browse_exercises))
            },
            navigationIcon = {
                BackButton(onClick = onBackPress)
            }
        )
        // Nine hundred exercises: the muscle chips alone left the rest to scrolling.
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = kenkoTextFieldColor(),
            placeholder = { Text(text = stringResource(R.string.label_search_exercise)) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painter = KenkoIcons.Close, contentDescription = null)
                    }
                }
            } else {
                null
            },
        )
        LazyTargets(contentPadding = PaddingValues(horizontal = 8.dp)) {
            TargetChip(
                selected = target == it,
                onClick = { onSelect(it) },
                text = stringResource(it.string),
                muscle = it,
            )
        }
        HorizontalDivider(thickness = KenkoBorderWidth)
    }
}

@Preview
@Composable
private fun ExercisesPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    val exercises = ExercisesPreviewParameter().values.first()
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        Exercises(
            state = ExercisesUiState(MuscleGroups.entries.flatMap { exercises }),
            snackbarState = SnackbarHostState(),
            onExerciseClick = {},
            onCreateClick = {},
            onSelectTarget = {},
            onBackPress = {},
            onMoreClick = {},
            onReferenceClick = {},
            onRemove = {}
        )
    }
}
