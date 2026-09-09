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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Gym
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.SetTick
import com.looker.kenko.ui.components.TickState
import com.looker.kenko.ui.components.kenkoTextFieldColor
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.numbers

/**
 * Gyms of the lifter. The chosen one decides which exercises the rest of the app offers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gyms(
    viewModel: GymsViewModel,
    onGymClick: (Int) -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var gymToDelete by remember { mutableStateOf<Gym?>(null) }

    gymToDelete?.let { gym ->
        AlertDialog(
            onDismissRequest = { gymToDelete = null },
            title = { Text(text = gym.name) },
            text = { Text(text = stringResource(R.string.label_delete_gym_question)) },
            confirmButton = {
                Button(
                    onClick = {
                        gym.id?.let(viewModel::deleteGym)
                        gymToDelete = null
                    },
                ) {
                    Text(text = stringResource(R.string.label_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { gymToDelete = null }) {
                    Text(text = stringResource(R.string.label_no))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_gyms)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.label_gym_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                GymRow(
                    name = stringResource(R.string.label_gym_any),
                    subtitle = stringResource(R.string.label_gym_any_desc),
                    selected = state.currentId == null,
                    onClick = { viewModel.selectGym(null) },
                )
            }
            items(state.gyms, key = { it.id ?: 0 }) { gym ->
                GymRow(
                    name = gym.name,
                    subtitle = pluralStringResource(
                        R.plurals.plural_exercises,
                        gym.exerciseCount,
                        gym.exerciseCount,
                    ),
                    selected = state.currentId == gym.id,
                    onClick = { viewModel.selectGym(gym.id) },
                    onEdit = { gym.id?.let(onGymClick) },
                    onDelete = { gymToDelete = gym },
                )
            }
            item {
                if (creating) {
                    Column {
                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            colors = kenkoTextFieldColor(),
                            label = { Text(text = stringResource(R.string.label_gym_name)) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    viewModel.createGym(newName)
                                    newName = ""
                                    creating = false
                                },
                            ) {
                                Text(text = stringResource(R.string.label_save))
                            }
                            TextButton(
                                onClick = {
                                    newName = ""
                                    creating = false
                                },
                            ) {
                                Text(text = stringResource(R.string.label_cancel))
                            }
                        }
                    }
                } else {
                    DashedAddButton(
                        label = stringResource(R.string.label_new_gym),
                        accent = true,
                        onClick = { creating = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun GymRow(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = KenkoBorderWidth,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.extraLarge,
            )
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (selected) {
            SetTick(state = TickState.Done, size = 22.dp)
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(painter = KenkoIcons.Rename, contentDescription = null)
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(painter = KenkoIcons.Delete, contentDescription = null)
            }
        }
    }
}
