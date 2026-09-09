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

package com.looker.kenko.ui.addSet

import androidx.compose.runtime.Immutable
import com.looker.kenko.data.model.Set
import kotlin.math.roundToInt

/**
 * What the add-set sheet is about to write: a new set of an exercise, or a drop under a set.
 */
@Immutable
data class AddSetTarget(
    val exerciseId: Int,
    val parentSetId: Int? = null,
    val supersetId: Int? = null,
    val suggestion: Suggestion? = null,
) {
    @Immutable
    data class Suggestion(val reps: Int, val weight: Float)
}

/**
 * Share of the weight kept on the next drop. Twenty percent off is the usual first cut.
 */
private const val DROP_WEIGHT_FACTOR = 0.8F

/**
 * Reps a lifter usually loses on the next drop.
 */
private const val DROP_REPS_STEP = 2

fun dropTargetOf(parent: Set, previousDrop: Set?): AddSetTarget {
    val from = previousDrop ?: parent
    return AddSetTarget(
        exerciseId = requireNotNull(parent.exercise.id),
        parentSetId = requireNotNull(parent.id),
        suggestion = AddSetTarget.Suggestion(
            reps = (from.repsOrDuration - DROP_REPS_STEP).coerceAtLeast(1),
            weight = roundToStep(from.weight * DROP_WEIGHT_FACTOR),
        ),
    )
}

/**
 * Gyms rack in 2.5 steps, so a suggested weight that cannot be loaded helps nobody.
 */
private fun roundToStep(weight: Float, step: Float = 2.5F): Float =
    if (weight <= 0F) 0F else ((weight / step).roundToInt() * step).coerceAtLeast(step)
