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

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.PhotoStore
import com.looker.kenko.data.model.DayPart
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.SessionResult
import com.looker.kenko.data.model.dayPartOf
import com.looker.kenko.data.model.result
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Everything the closing screen shows and everything it lets a lifter change.
 */
@HiltViewModel(assistedFactory = SessionSummaryViewModel.Factory::class)
class SessionSummaryViewModel @AssistedInject constructor(
    private val sessionRepo: SessionRepo,
    private val photoStore: PhotoStore,
    @Assisted routeData: Routes.SessionSummary,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.SessionSummary): SessionSummaryViewModel
    }

    private val date: LocalDate = LocalDate.fromEpochDays(routeData.epochDays.toLong())

    /**
     * The moment the screen was opened: the session ends here unless the lifter moves it.
     */
    private val openedAt: Long = Clock.System.now().epochSeconds

    var name: String by mutableStateOf("")
        private set

    var note: String by mutableStateOf("")
        private set

    var photoUri: String? by mutableStateOf(null)
        private set

    /**
     * How long the session took. Counted from the first set, then the lifter can round it.
     */
    var minutes: Int by mutableIntStateOf(0)
        private set

    var finishedAt: Long by mutableStateOf(openedAt)
        private set

    val part: DayPart = dayPartOf(
        kotlin.time.Instant.fromEpochSeconds(openedAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .hour,
    )

    private val _events = MutableSharedFlow<Unit>()

    /**
     * Fires once the session is closed and the screen can step aside.
     */
    val saved: SharedFlow<Unit> = _events

    val result: StateFlow<SessionResult?> = sessionRepo.stream
        .map { sessions ->
            val session = sessions.firstOrNull { it.date == date } ?: return@map null
            session.result(history = sessions, minutes = minutesOf(session))
        }
        .asStateFlow(null)

    init {
        viewModelScope.launch {
            val session = sessionRepo.stream.first().firstOrNull { it.date == date } ?: return@launch
            name = session.name.orEmpty()
            note = session.note.orEmpty()
            photoUri = session.photoUri
            minutes = minutesOf(session)
            session.finishedAt?.let { finishedAt = it }
        }
    }

    private fun minutesOf(session: Session): Int {
        session.minutes?.let { return it }
        val start = session.startedAt ?: return 0
        return ((openedAt - start) / 60).toInt().coerceIn(0, MAX_MINUTES)
    }

    fun renameTo(value: String) {
        name = value
    }

    fun writeNote(value: String) {
        note = value
    }

    fun shiftMinutes(by: Int) {
        minutes = (minutes + by).coerceIn(0, MAX_MINUTES)
    }

    fun shiftFinishedAt(byMinutes: Int) {
        finishedAt += byMinutes * 60L
    }

    fun setPhoto(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                photoStore.delete(photoUri)
                photoUri = null
                return@launch
            }
            photoUri = photoStore.save(uri)
        }
    }

    fun save() {
        viewModelScope.launch {
            val session = sessionRepo.stream.first().firstOrNull { it.date == date }
            val id = session?.id ?: return@launch
            sessionRepo.finishSession(
                sessionId = id,
                name = name,
                note = note,
                photoUri = photoUri,
                finishedAt = finishedAt,
                minutes = minutes,
            )
            _events.emit(Unit)
        }
    }
}

/**
 * Nobody trains for longer than this, and a wrong number should not travel into the stats.
 */
private const val MAX_MINUTES = 8 * 60
