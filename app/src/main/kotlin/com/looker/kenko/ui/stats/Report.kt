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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.SetTick
import com.looker.kenko.ui.components.TickState
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.formatDate

/**
 * The report: pick what goes in, then write it into a file of the lifter's choosing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Report(
    viewModel: ReportViewModel,
    onBackPress: () -> Unit,
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val savedMessage = stringResource(R.string.label_report_saved)
    val failedMessage = stringResource(R.string.label_report_failed)

    // Каждому формату свой контракт: тип документа задаётся при создании и потом не меняется,
    // иначе система дописывает к имени чужое расширение.
    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ReportFormat.Csv.mime),
    ) { uri ->
        if (uri != null) viewModel.save(uri, ReportFormat.Csv)
    }
    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ReportFormat.Pdf.mime),
    ) { uri ->
        if (uri != null) viewModel.save(uri, ReportFormat.Pdf)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(
                when (event) {
                    ReportEvent.Saved -> savedMessage
                    ReportEvent.Failed -> failedMessage
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.title_report)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "${formatDate(viewModel.period.from)} — ${formatDate(viewModel.period.to)}",
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.label_report_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailTile(
                    modifier = Modifier.weight(1F),
                    value = summary.sessions.toString(),
                    caption = stringResource(R.string.label_sessions_count),
                )
                DetailTile(
                    modifier = Modifier.weight(1F),
                    value = summary.sets.toString(),
                    caption = stringResource(R.string.label_sets_count),
                )
                DetailTile(
                    modifier = Modifier.weight(1F),
                    value = formatVolume(summary.volume),
                    caption = stringResource(R.string.label_total_volume),
                    accent = true,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.label_report_sections).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))

            SectionToggle(
                label = stringResource(R.string.label_section_summary),
                hint = stringResource(R.string.label_report_rows, 6),
                checked = sections.summary,
                onClick = viewModel::toggleSummary,
            )
            SectionToggle(
                label = stringResource(R.string.label_section_muscles),
                hint = stringResource(
                    R.string.label_report_rows,
                    summary.muscles.count { it.isTouched },
                ),
                checked = sections.muscles,
                onClick = viewModel::toggleMuscles,
            )
            SectionToggle(
                label = stringResource(R.string.label_section_exercises),
                hint = stringResource(R.string.label_report_rows, summary.exerciseLoads.size),
                checked = sections.exercises,
                onClick = viewModel::toggleExercises,
            )
            SectionToggle(
                label = stringResource(R.string.label_section_sets),
                hint = stringResource(R.string.label_report_rows, summary.sets),
                checked = sections.sets,
                onClick = viewModel::toggleSets,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                onClick = { csvPicker.launch(viewModel.suggestedName(ReportFormat.Csv)) },
            ) {
                Text(text = stringResource(R.string.label_save_csv))
            }
            Text(
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                text = stringResource(R.string.label_csv_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .border(
                        width = KenkoBorderWidth,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    .clickable { pdfPicker.launch(viewModel.suggestedName(ReportFormat.Pdf)) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_save_pdf),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                text = stringResource(R.string.label_pdf_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionToggle(
    label: String,
    hint: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall.numbers(),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.width(12.dp))
        SetTick(
            state = if (checked) TickState.Done else TickState.Ahead,
            onClick = onClick,
        )
    }
}
