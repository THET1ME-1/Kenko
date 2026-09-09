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

package com.looker.kenko.data.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * One step of a drop set: the working set itself, then a cut per drop.
 *
 * The lifter types reps only. Weights are the app's job.
 */
@Immutable
data class DropStep(
    val index: Int,
    val weight: Float,
    val reps: Int,
    val isPerformed: Boolean = false,
) {
    val isAutoWeight: Boolean get() = index > 0
}

/**
 * Plates come in 1.25 steps, so every suggested weight lands on one.
 */
const val WEIGHT_STEP = 1.25F

const val MIN_DROP_COUNT = 1
const val MAX_DROP_COUNT = 4
const val MIN_DROP_PERCENT = 5
const val MAX_DROP_PERCENT = 35
const val DROP_PERCENT_STEP = 5
const val DEFAULT_DROP_PERCENT = 20

/**
 * Reps a lifter usually loses on each cut, and the floor they never fall below.
 */
private const val REPS_LOST_PER_DROP = 3
private const val MIN_DROP_REPS = 4

fun roundToStep(weight: Float, step: Float = WEIGHT_STEP): Float =
    if (weight <= 0F) 0F else (weight / step).roundToInt() * step

/**
 * Epley: the one-rep max a set is worth.
 */
fun oneRepMax(weight: Float, reps: Int): Float = weight * (1 + reps / 30F)

/**
 * Builds the whole group from the working set: `40 → 32.5 → 25 → 20` for three drops at 20%.
 */
fun buildDropChain(
    base: Float,
    drops: Int,
    reps: Int,
    percent: Int = DEFAULT_DROP_PERCENT,
): List<DropStep> {
    val count = drops.coerceIn(0, MAX_DROP_COUNT)
    val cut = percent.coerceIn(MIN_DROP_PERCENT, MAX_DROP_PERCENT)
    var weight = base
    return (0..count).map { index ->
        val step = DropStep(
            index = index,
            weight = roundToStep(weight),
            reps = if (index == 0) reps else (reps - REPS_LOST_PER_DROP * index).coerceAtLeast(MIN_DROP_REPS),
        )
        weight *= (1 - cut / 100F)
        step
    }
}

fun List<DropStep>.volume(): Float = sumOf { (it.weight * it.reps).toDouble() }.toFloat()

/**
 * `40.0 → 32.5 → 25.0 → 20.0`, the way the group reads in a single line.
 */
fun List<DropStep>.chainLabel(): String = joinToString(" → ") { formatWeight(it.weight) }

fun formatWeight(weight: Float): String {
    val rounded = (weight * 100).roundToInt() / 100F
    return when {
        rounded % 1F == 0F -> "${rounded.toInt()}.0"
        (rounded * 10) % 1F == 0F -> rounded.toString()
        else -> String.format(java.util.Locale.US, "%.2f", rounded)
    }
}
