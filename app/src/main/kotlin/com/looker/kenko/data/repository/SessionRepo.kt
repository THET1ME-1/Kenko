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

package com.looker.kenko.data.repository

import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.DEFAULT_DROP_PERCENT
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SessionRepo {

    val stream: Flow<List<Session>>

    val setsCount: Flow<Int>

    val sessionsCount: Flow<Int>

    suspend fun addSet(sessionId: Int, set: Set)

    suspend fun addSet(
        sessionId: Int,
        exerciseId: Int,
        weight: Float,
        reps: Int,
        setType: SetType,
        rir: RepsInReserve,
        supersetId: Int? = null,
        dropCount: Int = 0,
        dropPercent: Int = DEFAULT_DROP_PERCENT,
        gripId: Int? = null,
    )

    /**
     * Adds a weight cut under [parentSetId], turning that set into a drop set.
     */
    suspend fun addDrop(
        parentSetId: Int,
        weight: Float,
        reps: Int,
        rir: RepsInReserve,
        dropIndex: Int = 0,
    )

    /**
     * How many cuts the group is meant to have and how much weight each one takes off.
     */
    suspend fun setDropSettings(setId: Int, count: Int, percent: Int)

    /**
     * Writes down one cut with the weight the app computed for it.
     */
    suspend fun markDropStep(parentSetId: Int, stepIndex: Int)

    /**
     * Writes down every cut the group is still missing.
     */
    suspend fun markWholeDropGroup(parentSetId: Int)

    /**
     * Takes back the cuts of a group, leaving the working set alone.
     */
    suspend fun clearDrops(parentSetId: Int)

    /**
     * Writes a set for every exercise of the superset that the current round is missing.
     */
    suspend fun closeSupersetRound(sessionId: Int, supersetId: Int, items: List<PlanItem>)

    /**
     * Takes back the last round of a superset.
     */
    suspend fun clearLastSupersetRound(sessionId: Int, supersetId: Int)

    suspend fun removeSet(setId: Int)

    suspend fun getSessionIdOrCreate(date: LocalDate): Int

    fun streamByDate(date: LocalDate): Flow<Session?>

    suspend fun getSets(sessionId: Int): List<Set>

    suspend fun getLastSetByExerciseId(exerciseId: Int): Set?
}
