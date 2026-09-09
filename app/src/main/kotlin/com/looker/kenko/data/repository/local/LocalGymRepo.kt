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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.GymDao
import com.looker.kenko.data.local.model.GymExerciseEntity
import com.looker.kenko.data.local.model.toEntity
import com.looker.kenko.data.local.model.toExternal
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Gym
import com.looker.kenko.data.repository.GymRepo
import com.looker.kenko.data.repository.SettingsRepo
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class LocalGymRepo @Inject constructor(
    private val dao: GymDao,
    private val exerciseDao: ExerciseDao,
    private val settingsRepo: SettingsRepo,
) : GymRepo {

    override val gyms: Flow<List<Gym>> = dao.gymsFlow().flatMapLatest { entities ->
        if (entities.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            combine(entities.map { gym -> dao.exerciseCountFlow(gym.id) }) { counts ->
                entities.mapIndexed { index, gym -> gym.toExternal(counts[index]) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val current: Flow<Gym?> = settingsRepo.get { currentGymId }
        .flatMapLatest { gymId ->
            if (gymId == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                gyms.map { list -> list.firstOrNull { it.id == gymId } }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val availableExercises: Flow<List<Exercise>> = settingsRepo.get { currentGymId }
        .flatMapLatest { gymId ->
            if (gymId == null) {
                exerciseDao.stream().map { list -> list.map { it.toExternal() } }
            } else {
                exercises(gymId)
            }
        }

    override fun exercises(gymId: Int): Flow<List<Exercise>> =
        dao.exercisesFlow(gymId).map { list -> list.map { it.toExternal() } }

    override suspend fun createGym(name: String, copyFrom: Int?): Int {
        val id = dao.insertGym(Gym(name = name).toEntity()).toInt()
        if (copyFrom != null) {
            val links = dao.getExerciseIds(copyFrom).map { GymExerciseEntity(id, it) }
            dao.addExercises(links)
        }
        return id
    }

    override suspend fun renameGym(gym: Gym) {
        dao.updateGym(gym.toEntity())
    }

    override suspend fun deleteGym(gymId: Int) {
        dao.deleteGym(gymId)
        if (settingsRepo.stream.first().currentGymId == gymId) {
            settingsRepo.setCurrentGym(null)
        }
    }

    override suspend fun setCurrent(gymId: Int?) {
        settingsRepo.setCurrentGym(gymId)
    }

    override suspend fun setExercisePresent(gymId: Int, exerciseId: Int, present: Boolean) {
        if (present) {
            dao.addExercise(GymExerciseEntity(gymId, exerciseId))
        } else {
            dao.removeExercise(gymId, exerciseId)
        }
    }

    override suspend fun setAllExercises(gymId: Int, exerciseIds: List<Int>) {
        dao.clearExercises(gymId)
        dao.addExercises(exerciseIds.map { GymExerciseEntity(gymId, it) })
    }

    override suspend fun isAvailable(exerciseId: Int): Boolean {
        val gymId = settingsRepo.stream.first().currentGymId ?: return true
        return dao.hasExercise(gymId, exerciseId)
    }
}
