/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.ui.gyms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.ExerciseCard
import com.looker.kenko.ui.components.HorizontalTargetChips
import com.looker.kenko.ui.components.SetTick
import com.looker.kenko.ui.components.TickState
import com.looker.kenko.ui.components.kenkoTextFieldColor

/**
 * What this gym can do: every exercise the app knows, ticked when the place has the gear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymEdit(
    viewModel: GymEditViewModel,
    onBackPress: () -> Unit,
) {
    // Имя сохраняется на выходе, а не по букве: иначе каждая буква едет в базу.
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember(state.gym?.id) { mutableStateOf(state.gym?.name.orEmpty()) }

    // Системный «назад» уходит мимо кнопки в шапке, и правка имени пропадала.
    BackHandler {
        viewModel.rename(name)
        onBackPress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(
                        onClick = {
                            viewModel.rename(name)
                            onBackPress()
                        },
                    )
                },
                title = { Text(text = stringResource(R.string.label_gym_equipment)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors = kenkoTextFieldColor(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.rename(name) }),
                        label = { Text(text = stringResource(R.string.label_gym_name)) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1F)
                                .padding(top = 14.dp),
                            text = pluralStringResource(
                                R.plurals.plural_exercises,
                                state.presentIds.size,
                                state.presentIds.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        TextButton(onClick = { viewModel.setAllVisible(true) }) {
                            Text(text = stringResource(R.string.label_gym_all_on))
                        }
                        TextButton(onClick = { viewModel.setAllVisible(false) }) {
                            Text(text = stringResource(R.string.label_gym_all_off))
                        }
                    }
                }
            }
            item {
                HorizontalTargetChips(
                    target = state.filter,
                    onSelect = viewModel::setFilter,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
            items(state.exercises, key = { it.id ?: 0 }) { exercise ->
                val present = exercise.id in state.presentIds
                ExerciseCard(
                    exercise = exercise,
                    onClick = { viewModel.toggle(exercise, !present) },
                    trailing = {
                        SetTick(
                            state = if (present) TickState.Done else TickState.Ahead,
                            onClick = { viewModel.toggle(exercise, !present) },
                        )
                    },
                )
            }
        }
    }
}
