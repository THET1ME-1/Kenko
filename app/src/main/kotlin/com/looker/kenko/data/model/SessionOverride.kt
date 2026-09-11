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

package com.looker.kenko.data.model

import androidx.compose.runtime.Immutable

/**
 * Value of [SessionOverride.supersetId] that takes an exercise out of its superset for today.
 */
const val NO_SUPERSET = -1

/**
 * What the lifter changed about a planned exercise for one day only.
 *
 * The plan says what the training normally looks like; today it can look different without
 * rewriting the program: an exercise is skipped, swapped for another one, moved up the list or
 * given a goal of its own.
 */
@Immutable
data class SessionOverride(
    /**
     * Exercise of the plan the change belongs to, not the one standing in for it.
     */
    val exerciseId: Int,
    val skipped: Boolean = false,
    val replacement: Exercise? = null,
    /**
     * Position for today. Items without one keep the order of the plan behind those that have it.
     */
    val orderIndex: Int? = null,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetRepsMax: Int? = null,
    val barWeight: Float? = null,
    val leftWeight: Float? = null,
    val rightWeight: Float? = null,
    val restSeconds: Int? = null,
    val dropCount: Int? = null,
    /**
     * Superset the exercise belongs to today. [NO_SUPERSET] breaks the one the plan gave it.
     */
    val supersetId: Int? = null,
) {
    /**
     * Whether anything is left to keep. An override that changes nothing is deleted instead.
     */
    val isEmpty: Boolean
        get() = !skipped && replacement == null && orderIndex == null && targetSets == null &&
            targetReps == null && targetRepsMax == null && barWeight == null &&
            leftWeight == null && rightWeight == null && restSeconds == null &&
            dropCount == null && supersetId == null
}

/**
 * The day as it should be performed today: the plan with the changes of the day applied.
 */
fun List<PlanItem>.applyToday(overrides: List<SessionOverride>): List<PlanItem> {
    if (overrides.isEmpty()) return this
    val byExercise = overrides.associateBy { it.exerciseId }
    return this
        .mapNotNull { item ->
            val override = byExercise[item.exercise.id]
            if (override?.skipped == true) null else item to override
        }
        // Order is read before the swap: the change belongs to the planned exercise, not to the
        // movement standing in for it today.
        .sortedBy { (item, override) -> override?.orderIndex ?: item.order }
        .map { (item, override) ->
            if (override == null) return@map item
            item.copy(
                exercise = override.replacement ?: item.exercise,
                replacedExerciseId = override.replacement?.let { override.exerciseId },
                targetSets = override.targetSets ?: item.targetSets,
                targetReps = override.targetReps ?: item.targetReps,
                targetRepsMax = override.targetRepsMax ?: item.targetRepsMax,
                barWeight = override.barWeight ?: item.barWeight,
                leftWeight = override.leftWeight ?: item.leftWeight,
                rightWeight = override.rightWeight ?: item.rightWeight,
                restSeconds = override.restSeconds ?: item.restSeconds,
                dropCount = override.dropCount ?: item.dropCount,
                supersetId = when (override.supersetId) {
                    null -> item.supersetId
                    NO_SUPERSET -> null
                    else -> override.supersetId
                },
            )
        }
}
