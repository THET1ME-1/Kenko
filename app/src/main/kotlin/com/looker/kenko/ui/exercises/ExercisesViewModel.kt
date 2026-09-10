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

package com.looker.kenko.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.matchesSearch
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repo: ExerciseRepo,
    private val uriHandler: UriHandler,
    private val stringHandler: StringHandler,
) : ViewModel() {

    // null -> all
    private val selectedTarget: MutableStateFlow<MuscleGroups?> = MutableStateFlow(null)

    /**
     * What was typed into the search field. Nine hundred exercises are not a list to scroll.
     */
    var searchQuery: String by mutableStateOf("")
        private set

    private val searchQueryFlow = snapshotFlow { searchQuery }

    private val exercisesStream: Flow<List<Exercise>> = repo.stream

    val snackbarState = SnackbarHostState()

    val exercises: StateFlow<ExercisesUiState> = combine(
        exercisesStream,
        selectedTarget,
        searchQueryFlow,
    ) { exercises, target, query ->
        ExercisesUiState(
            exercises = exercises
                .filter { target == null || it.target == target }
                .filter { it.matchesSearch(query) },
            selected = target,
        )
    }.asStateFlow(ExercisesUiState())

    /**
     * Deleting is a swipe away, so it comes back the same way: the row is kept aside until
     * the snackbar goes, and «Undo» writes it back with its id, photo and Russian name.
     */
    fun removeExercise(id: Int?) {
        viewModelScope.launch {
            if (id == null) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_unknown))
                return@launch
            }
            val removed = repo.get(id)
            repo.remove(id)
            if (removed == null) return@launch
            val answer = snackbarState.showSnackbar(
                message = stringHandler.getString(R.string.label_exercise_removed),
                actionLabel = stringHandler.getString(R.string.label_undo_delete),
                duration = SnackbarDuration.Short,
            )
            if (answer == SnackbarResult.ActionPerformed) {
                repo.upsert(removed)
            }
        }
    }

    fun setSearch(value: String) {
        searchQuery = value
    }

    fun setTarget(value: MuscleGroups?) {
        viewModelScope.launch {
            selectedTarget.emit(value)
        }
    }

    fun onReferenceClick(reference: String) {
        viewModelScope.launch {
            try {
                uriHandler.openUri(reference)
            } catch (e: IllegalStateException) {
                snackbarState.showSnackbar(
                    e.message ?: stringHandler.getString(R.string.error_invalid_url)
                )
            }
        }
    }
}

val MuscleGroups?.string: Int
    @StringRes
    get() = this?.stringRes ?: R.string.label_all_muscle_groups

@Stable
class ExercisesUiState(
    val exercises: List<Exercise> = emptyList(),
    val selected: MuscleGroups? = null,
)
