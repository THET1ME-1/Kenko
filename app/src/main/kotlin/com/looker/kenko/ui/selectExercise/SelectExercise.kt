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

package com.looker.kenko.ui.selectExercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.ExerciseCard
import com.looker.kenko.ui.components.LazyTargets
import com.looker.kenko.ui.components.TargetChip
import com.looker.kenko.ui.components.disableScrollConnection
import com.looker.kenko.ui.components.kenkoTextFieldColor
import com.looker.kenko.ui.exercises.string
import com.looker.kenko.ui.planEdit.components.ExerciseItem
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.start

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectExercise(
    onDone: (Exercise) -> Unit,
    onRequestNewExercise: (name: String?, target: MuscleGroups?) -> Unit,
    modifier: Modifier = Modifier,
    onBackPress: (() -> Unit)? = null,
) {
    val viewModel: SelectExerciseViewModel = hiltViewModel()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        val target by viewModel.targetMuscle.collectAsStateWithLifecycle()
        val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()

        if (onBackPress != null) {
            BackButton(
                onClick = onBackPress,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        AddExerciseHeader(modifier = Modifier.padding(horizontal = 16.dp))
        ExerciseSearchField(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = viewModel.searchQuery,
            onNameChange = viewModel::setSearch,
            onAddClick = {
                onRequestNewExercise(viewModel.searchQuery.ifBlank { null }, target)
            },
        )
        val gymName by viewModel.gymName.collectAsStateWithLifecycle()
        val showEverything by viewModel.showEverything.collectAsStateWithLifecycle()
        if (gymName != null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = gymName.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                TextButton(onClick = viewModel::toggleShowEverything) {
                    Text(
                        text = stringResource(
                            if (showEverything) {
                                R.string.label_only_gym
                            } else {
                                R.string.label_show_all_exercises
                            },
                        ),
                    )
                }
            }
        }
        LazyTargets(contentPadding = PaddingValues(horizontal = 8.dp)) {
            TargetChip(
                selected = target == it,
                onClick = { viewModel.setTarget(it) },
                text = stringResource(it.string),
                muscle = it,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1F),
        ) {
            when (searchResult) {
                SearchResult.Loading -> ContainedLoadingIndicator()
                SearchResult.NotFound -> SearchNotFound(
                    onAddNewExercise = { onRequestNewExercise(viewModel.searchQuery, target) },
                )

                is SearchResult.Success -> SearchResult(
                    searchResult = searchResult as SearchResult.Success,
                    isGymChosen = gymName != null,
                    onClick = onDone,
                    onAddToGym = viewModel::addToGym,
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchResult: SearchResult.Success,
    isGymChosen: Boolean,
    onClick: (Exercise) -> Unit,
    onAddToGym: (Exercise) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        items(searchResult.exercises) { exercise ->
            val missing = isGymChosen && exercise.id !in searchResult.availableIds
            ExerciseCard(
                exercise = exercise,
                onClick = { onClick(exercise) },
                trailing = {
                    if (missing) {
                        TextButton(onClick = { onAddToGym(exercise) }) {
                            Text(text = stringResource(R.string.label_add_to_gym))
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchNotFound(onAddNewExercise: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_cant_find_exercise),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddNewExercise,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Icon(painter = KenkoIcons.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.label_create_exercise))
            }
        }
    }
}

@Composable
private fun ExerciseSearchField(
    name: String,
    onNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = name,
            onValueChange = onNameChange,
            colors = kenkoTextFieldColor(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = MaterialTheme.shapes.large.end(8.dp),
            label = {
                Text(text = stringResource(R.string.label_search_exercise))
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalIconButton(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large.start(8.dp),
            onClick = onAddClick,
        ) {
            Icon(painter = KenkoIcons.Add, contentDescription = null)
        }
    }
}

@Composable
private fun AddExerciseHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.label_add_exercise_header),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Preview
@Composable
private fun ErrorPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        SearchNotFound(onAddNewExercise = {})
    }
}
