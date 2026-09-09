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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Gym
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.GymRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Equipment list of one gym: every exercise the app knows, ticked or not.
 */
@HiltViewModel(assistedFactory = GymEditViewModel.Factory::class)
class GymEditViewModel @AssistedInject constructor(
    private val repo: GymRepo,
    exerciseRepo: ExerciseRepo,
    @Assisted private val routeData: Routes.GymEdit,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.GymEdit): GymEditViewModel
    }

    private val gymId: Int = routeData.id

    private val filter: MutableStateFlow<MuscleGroups?> = MutableStateFlow(null)

    val state: StateFlow<GymEditUiState> = combine(
        exerciseRepo.stream,
        repo.exercises(gymId),
        repo.gyms,
        filter,
    ) { all, inGym, gyms, muscle ->
        val present = inGym.mapNotNull { it.id }.toSet()
        GymEditUiState(
            gym = gyms.firstOrNull { it.id == gymId },
            exercises = all.filter { muscle == null || it.target == muscle },
            presentIds = present,
            filter = muscle,
        )
    }.asStateFlow(GymEditUiState())

    fun setFilter(muscle: MuscleGroups?) {
        viewModelScope.launch { filter.emit(muscle) }
    }

    fun toggle(exercise: Exercise, present: Boolean) {
        val exerciseId = exercise.id ?: return
        viewModelScope.launch {
            repo.setExercisePresent(gymId, exerciseId, present)
        }
    }

    /**
     * Marks everything currently on screen as present, or clears it.
     */
    fun setAllVisible(present: Boolean) {
        viewModelScope.launch {
            val visible = state.value.exercises.mapNotNull { it.id }
            if (present) {
                visible.forEach { repo.setExercisePresent(gymId, it, true) }
            } else {
                visible.forEach { repo.setExercisePresent(gymId, it, false) }
            }
        }
    }

    fun rename(name: String) {
        val gym = state.value.gym ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.renameGym(gym.copy(name = name.trim()))
        }
    }
}

@Stable
data class GymEditUiState(
    val gym: Gym? = null,
    val exercises: List<Exercise> = emptyList(),
    val presentIds: Set<Int> = emptySet(),
    val filter: MuscleGroups? = null,
)
