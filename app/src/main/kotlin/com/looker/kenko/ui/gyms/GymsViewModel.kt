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
import com.looker.kenko.data.model.Gym
import com.looker.kenko.data.repository.GymRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class GymsViewModel @Inject constructor(
    private val repo: GymRepo,
) : ViewModel() {

    val state: StateFlow<GymsUiState> = combine(repo.gyms, repo.current) { gyms, current ->
        GymsUiState(gyms = gyms, currentId = current?.id)
    }.asStateFlow(GymsUiState())

    fun createGym(name: String, copyFrom: Int? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.createGym(name.trim(), copyFrom)
            repo.setCurrent(id)
        }
    }

    fun selectGym(gymId: Int?) {
        viewModelScope.launch {
            repo.setCurrent(gymId)
        }
    }

    fun deleteGym(gymId: Int) {
        viewModelScope.launch {
            repo.deleteGym(gymId)
        }
    }
}

@Stable
data class GymsUiState(
    val gyms: List<Gym> = emptyList(),
    val currentId: Int? = null,
)
