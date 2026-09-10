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

package com.looker.kenko.ui.sessionSummary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.DayPart
import com.looker.kenko.data.model.Record
import com.looker.kenko.data.model.SessionResult
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.BodyHeatMap
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.kenkoTextFieldColor
import com.looker.kenko.ui.components.rememberPhoto
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.stats.formatVolume
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.numbers
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The end of a session: what it added up to, and what the lifter wants to remember about it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummary(
    viewModel: SessionSummaryViewModel,
    onBackPress: () -> Unit,
    onSaved: () -> Unit,
) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved() }
    }

    // Имя подставляется сразу: пустое поле человек чаще пропустит, чем заполнит.
    val suggestedName = defaultName(viewModel.part)
    LaunchedEffect(suggestedName) {
        if (viewModel.name.isBlank()) viewModel.renameTo(suggestedName)
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        viewModel.setPhoto(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.title_finish_session)) },
            )
        },
        bottomBar = {
            // Кнопку надо поднимать самим: Scaffold отдаёт инсеты содержимому, а нижнюю
            // панель оставляет на совести того, кто её рисует, — «Сохранить» уходил под
            // кнопки телефона.
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                onClick = viewModel::save,
            ) {
                Text(text = stringResource(R.string.label_save_session))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            result?.let { ResultCard(result = it) }

            Spacer(Modifier.height(16.dp))

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.name,
                onValueChange = viewModel::renameTo,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = kenkoTextFieldColor(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = { Text(text = stringResource(R.string.label_session_name)) },
                placeholder = { Text(text = defaultName(viewModel.part)) },
            )

            Spacer(Modifier.height(10.dp))

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                value = viewModel.note,
                onValueChange = viewModel::writeNote,
                shape = MaterialTheme.shapes.large,
                colors = kenkoTextFieldColor(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = { Text(text = stringResource(R.string.label_session_note)) },
                placeholder = { Text(text = stringResource(R.string.label_session_note_hint)) },
            )

            Spacer(Modifier.height(12.dp))

            SessionPhoto(
                photoUri = viewModel.photoUri,
                onPick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemove = { viewModel.setPhoto(null) },
            )

            Spacer(Modifier.height(12.dp))

            StepperRow(
                title = stringResource(R.string.label_finished_at),
                value = formatTime(viewModel.finishedAt),
                onLess = { viewModel.shiftFinishedAt(-FINISH_STEP_MINUTES) },
                onMore = { viewModel.shiftFinishedAt(FINISH_STEP_MINUTES) },
            )

            Spacer(Modifier.height(8.dp))

            StepperRow(
                title = stringResource(R.string.label_session_length),
                value = pluralStringResource(
                    R.plurals.plural_minutes,
                    viewModel.minutes,
                    viewModel.minutes,
                ),
                onLess = { viewModel.shiftMinutes(-LENGTH_STEP_MINUTES) },
                onMore = { viewModel.shiftMinutes(LENGTH_STEP_MINUTES) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Five minutes is the smallest step worth arguing about after a session.
 */
private const val LENGTH_STEP_MINUTES = 5
private const val FINISH_STEP_MINUTES = 5

@Composable
private fun defaultName(part: DayPart): String = stringResource(
    when (part) {
        DayPart.Morning -> R.string.label_session_morning
        DayPart.Afternoon -> R.string.label_session_afternoon
        DayPart.Evening -> R.string.label_session_evening
        DayPart.Night -> R.string.label_session_night
    },
)

/**
 * What the session was worth: numbers first, then the body it landed on, then the records.
 */
@Composable
private fun ResultCard(result: SessionResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultTile(
                modifier = Modifier.weight(1F),
                value = formatVolume(result.volume),
                caption = stringResource(R.string.label_total_volume),
                accent = true,
            )
            ResultTile(
                modifier = Modifier.weight(1F),
                value = result.sets.toString(),
                caption = stringResource(R.string.label_sets_count),
            )
            ResultTile(
                modifier = Modifier.weight(1F),
                value = result.reps.toString(),
                caption = stringResource(R.string.label_reps_count),
            )
        }

        Spacer(Modifier.height(14.dp))

        BodyHeatMap(load = result.muscles, height = 220.dp)

        if (result.records.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.label_records_of_session).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(6.dp))
            result.records.forEach { record ->
                RecordLine(record = record)
            }
        }
    }
}

@Composable
private fun RecordLine(record: Record, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = record.exercise.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${record.reps} × ${formatWeight(record.weight)}",
            style = MaterialTheme.typography.titleMedium.numbers(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ResultTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (accent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.numbers(),
            color = if (accent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
    }
}

@Composable
private fun SessionPhoto(
    photoUri: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = rememberPhoto(photoUri)
    if (photo == null) {
        DashedAddButton(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            label = stringResource(R.string.label_add_session_photo),
            onClick = onPick,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onPick),
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                text = stringResource(R.string.label_remove_photo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * A value with a minus and a plus around it: the lifter rounds the number, not types it.
 */
@Composable
private fun StepperRow(
    title: String,
    value: String,
    onLess: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(
                width = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F).padding(start = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        StepKey(icon = KenkoIcons.Remove, onClick = onLess)
        Spacer(Modifier.width(8.dp))
        StepKey(icon = KenkoIcons.Add, onClick = onMore)
    }
}

@Composable
private fun StepKey(
    icon: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = null)
    }
}

/**
 * `22:17`, the way a clock on the wall shows it.
 */
private fun formatTime(epochSeconds: Long): String {
    val time = Instant.fromEpochSeconds(epochSeconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
