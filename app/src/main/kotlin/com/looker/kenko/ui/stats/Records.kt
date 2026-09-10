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

package com.looker.kenko.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Record
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.MuscleIcon
import com.looker.kenko.ui.components.unitLabel
import com.looker.kenko.ui.components.weightText
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.formatDate

/**
 * Records of every exercise: the best set, when it happened and how long it has been standing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Records(
    viewModel: RecordsViewModel,
    onBackPress: () -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.title_records)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (records.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        text = stringResource(R.string.label_no_records),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(records, key = { it.exercise.name }) { record ->
                RecordRow(
                    record = record,
                    standingDays = record.standingFor(viewModel.today),
                    onClick = { onExerciseClick(record.exercise.name) },
                )
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: Record,
    standingDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MuscleIcon(
            muscle = record.exercise.target,
            height = 52.dp,
            tint = MaterialTheme.colorScheme.primary,
            body = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = record.exercise.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${record.reps} × ${weightText(record.weight)} " +
                    unitLabel(),
                style = MaterialTheme.typography.labelMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.label_record_standing,
                    pluralStringResource(R.plurals.plural_days, standingDays, standingDays),
                    formatDate(record.date),
                ),
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = weightText(record.estimatedMax),
                style = MaterialTheme.typography.titleLarge.numbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.label_estimated_max),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
