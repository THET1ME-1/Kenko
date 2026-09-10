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

package com.looker.kenko.ui.sessions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.SessionGroup
import com.looker.kenko.data.model.SessionGrouping
import com.looker.kenko.data.model.groupSessions
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repo: SessionRepo,
    private val settingsRepo: SettingsRepo,
    private val stringHandler: StringHandler,
) : ViewModel() {

    val snackbarState = SnackbarHostState()
    private val sessionsStream: Flow<List<Session>> = repo.stream

    private val groupingStream: Flow<SessionGrouping> = settingsRepo.get { sessionGrouping }

    val state: StateFlow<SessionsUiData> = combine(
        sessionsStream,
        groupingStream,
    ) { sessions, grouping ->
        val written = sessions.filter { it.sets.isNotEmpty() }
        SessionsUiData(
            groups = written.groupSessions(grouping),
            grouping = grouping,
            isEmpty = written.isEmpty(),
        )
    }.asStateFlow(SessionsUiData())

    /**
     * Throws a session away with everything written into it.
     */
    fun removeSession(session: Session) {
        val id = session.id ?: return
        viewModelScope.launch {
            repo.removeSession(id)
        }
    }

    /**
     * Moves a session to another day, unless that day already holds one.
     */
    fun moveSession(session: Session, date: LocalDate) {
        val id = session.id ?: return
        viewModelScope.launch {
            if (!repo.moveSession(id, date)) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.label_date_taken))
            }
        }
    }

    fun setGrouping(grouping: SessionGrouping) {
        viewModelScope.launch {
            settingsRepo.setSessionGrouping(grouping)
        }
    }
}

@Stable
data class SessionsUiData(
    val groups: List<SessionGroup> = emptyList(),
    val grouping: SessionGrouping = SessionGrouping.Month,
    val isEmpty: Boolean = true,
)
