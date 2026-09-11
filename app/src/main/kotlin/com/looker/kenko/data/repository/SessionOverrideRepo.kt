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

import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.SessionOverrideDao
import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.local.model.SessionOverrideEntity
import com.looker.kenko.data.local.model.toExternal
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.NO_SUPERSET
import com.looker.kenko.data.model.SessionOverride
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Changes a lifter makes to the plan for one day: what they skipped, swapped, moved or decided
 * to do differently today. The program behind the session is never touched here.
 */
@Singleton
class SessionOverrideRepo @Inject constructor(
    private val dao: SessionOverrideDao,
    private val exerciseDao: ExerciseDao,
    private val setsDao: SetsDao,
) {

    fun stream(sessionId: Int): Flow<List<SessionOverride>> = dao.stream(sessionId)
        .map { entities -> entities.map { it.toModel() } }

    /**
     * Drops the exercise from today: the work written into it goes with it.
     */
    suspend fun skip(sessionId: Int, exerciseId: Int) {
        setsDao.deleteExerciseSets(sessionId, exerciseId)
        edit(sessionId, exerciseId) { it.copy(skipped = true, replacementId = null) }
    }

    /**
     * Puts another movement in its place and carries the written sets over to it.
     */
    suspend fun replace(sessionId: Int, exerciseId: Int, replacement: Exercise) {
        val replacementId = replacement.id ?: return
        setsDao.moveSetsToExercise(sessionId, exerciseId, replacementId)
        edit(sessionId, exerciseId) { it.copy(replacementId = replacementId, skipped = false) }
    }

    /**
     * Order of the exercises for today, as the list stands after the move.
     */
    suspend fun reorder(sessionId: Int, exerciseIds: List<Int>) {
        exerciseIds.forEachIndexed { index, exerciseId ->
            edit(sessionId, exerciseId) { it.copy(orderIndex = index) }
        }
    }

    suspend fun setTargets(
        sessionId: Int,
        exerciseId: Int,
        targetSets: Int?,
        targetReps: Int?,
        targetRepsMax: Int?,
        barWeight: Float?,
        leftWeight: Float?,
        rightWeight: Float?,
        restSeconds: Int?,
        dropCount: Int?,
    ) {
        edit(sessionId, exerciseId) {
            it.copy(
                targetSets = targetSets,
                targetReps = targetReps,
                targetRepsMax = targetRepsMax,
                barWeight = barWeight,
                leftWeight = leftWeight,
                rightWeight = rightWeight,
                restSeconds = restSeconds,
                dropCount = dropCount,
            )
        }
    }

    /**
     * Ties exercises into a superset for today only.
     */
    suspend fun tieSuperset(sessionId: Int, exerciseIds: List<Int>, supersetId: Int) {
        exerciseIds.forEach { exerciseId ->
            edit(sessionId, exerciseId) { it.copy(supersetId = supersetId) }
        }
    }

    /**
     * Takes the exercises out of their superset until tomorrow.
     */
    suspend fun breakSuperset(sessionId: Int, exerciseIds: List<Int>) {
        exerciseIds.forEach { exerciseId ->
            edit(sessionId, exerciseId) { it.copy(supersetId = NO_SUPERSET) }
        }
    }

    /**
     * Gives the exercise back everything the plan says about it.
     */
    suspend fun reset(sessionId: Int, exerciseId: Int) {
        dao.delete(sessionId, exerciseId)
    }

    /**
     * Reads the row, changes it, and writes it back — or deletes it when nothing is left to say.
     */
    private suspend fun edit(
        sessionId: Int,
        exerciseId: Int,
        block: (SessionOverrideEntity) -> SessionOverrideEntity,
    ) {
        val current = dao.get(sessionId, exerciseId)
            ?: SessionOverrideEntity(sessionId = sessionId, exerciseId = exerciseId)
        val next = block(current)
        if (next.toModel().isEmpty) {
            dao.delete(sessionId, exerciseId)
        } else {
            dao.upsert(next)
        }
    }

    private suspend fun SessionOverrideEntity.toModel(): SessionOverride = SessionOverride(
        exerciseId = exerciseId,
        skipped = skipped,
        replacement = replacementId?.let { exerciseDao.get(it)?.toExternal() },
        orderIndex = orderIndex,
        targetSets = targetSets,
        targetReps = targetReps,
        targetRepsMax = targetRepsMax,
        barWeight = barWeight,
        leftWeight = leftWeight,
        rightWeight = rightWeight,
        restSeconds = restSeconds,
        dropCount = dropCount,
        supersetId = supersetId,
    )
}
