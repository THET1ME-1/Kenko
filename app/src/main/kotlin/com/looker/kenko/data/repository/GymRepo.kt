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

package com.looker.kenko.data.repository

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Gym
import kotlinx.coroutines.flow.Flow

/**
 * Gyms and what each one can do.
 *
 * While no gym is chosen the app behaves as before: every exercise is available.
 */
interface GymRepo {

    val gyms: Flow<List<Gym>>

    val current: Flow<Gym?>

    /**
     * Exercises available right now: the current gym's equipment, or all of them when
     * no gym is chosen.
     */
    val availableExercises: Flow<List<Exercise>>

    fun exercises(gymId: Int): Flow<List<Exercise>>

    suspend fun createGym(name: String, copyFrom: Int? = null): Int

    suspend fun renameGym(gym: Gym)

    suspend fun deleteGym(gymId: Int)

    suspend fun setCurrent(gymId: Int?)

    suspend fun setExercisePresent(gymId: Int, exerciseId: Int, present: Boolean)

    suspend fun setAllExercises(gymId: Int, exerciseIds: List<Int>)

    /**
     * True when the exercise can be done at the current gym, or when no gym is chosen.
     */
    suspend fun isAvailable(exerciseId: Int): Boolean
}
