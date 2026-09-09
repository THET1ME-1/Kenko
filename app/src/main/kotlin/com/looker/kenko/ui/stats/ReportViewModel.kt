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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.export.ReportSections
import com.looker.kenko.data.export.ReportWriter
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * File the report is written to. The lifter picks the place, the app fills the file.
 */
enum class ReportFormat(val extension: String, val mime: String) {
    Csv("csv", "text/csv"),
    Pdf("pdf", "application/pdf"),
}

/**
 * The report screen: which parts go into the file, and writing that file where the lifter says.
 */
@HiltViewModel(assistedFactory = ReportViewModel.Factory::class)
class ReportViewModel @AssistedInject constructor(
    private val sessionRepo: SessionRepo,
    @ApplicationContext private val context: Context,
    @Assisted routeData: Routes.Report,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.Report): ReportViewModel
    }

    val period: StatsPeriod = routeData.period()

    private val writer = ReportWriter(context)

    private val _sections = MutableStateFlow(ReportSections())
    val sections: StateFlow<ReportSections> = _sections

    private val _events = MutableSharedFlow<ReportEvent>()
    val events: SharedFlow<ReportEvent> = _events

    val summary: StateFlow<StatsSummary> = sessionRepo.stream
        .map { it.summarize(period) }
        .asStateFlow(emptyList<Session>().summarize(period))

    fun toggleSummary() {
        _sections.value = _sections.value.copy(summary = !_sections.value.summary)
    }

    fun toggleMuscles() {
        _sections.value = _sections.value.copy(muscles = !_sections.value.muscles)
    }

    fun toggleExercises() {
        _sections.value = _sections.value.copy(exercises = !_sections.value.exercises)
    }

    fun toggleSets() {
        _sections.value = _sections.value.copy(sets = !_sections.value.sets)
    }

    fun suggestedName(format: ReportFormat): String = writer.fileName(period, format.extension)

    fun save(uri: Uri, format: ReportFormat) {
        viewModelScope.launch {
            val sessions = sessionRepo.stream.first()
            val done = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: error("no stream for $uri")
                    when (format) {
                        ReportFormat.Csv -> writer.writeCsv(
                            output = stream,
                            sessions = sessions,
                            period = period,
                            sections = _sections.value,
                        )

                        ReportFormat.Pdf -> writer.writePdf(
                            output = stream,
                            sessions = sessions,
                            period = period,
                            sections = _sections.value,
                        )
                    }
                }.isSuccess
            }
            _events.emit(if (done) ReportEvent.Saved else ReportEvent.Failed)
        }
    }
}

sealed interface ReportEvent {
    data object Saved : ReportEvent
    data object Failed : ReportEvent
}
