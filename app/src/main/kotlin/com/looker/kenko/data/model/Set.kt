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
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.local.model.WeightNote
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Set(
    val repsOrDuration: Int,
    val weight: Float,
    val type: SetType,
    val exercise: Exercise,
    val rir: RepsInReserve,
    /**
     * Why this weight is not comparable with a clean set, when it is not.
     */
    val weightNote: WeightNote? = null,
    /**
     * Set this to the id of the parent set to make this set a drop of that set.
     */
    val parentSetId: Int? = null,
    /**
     * Position inside a drop chain. `0` for a normal set, `1` for the first drop and so on.
     */
    val dropIndex: Int = 0,
    /**
     * Sets sharing a [supersetId] inside a session are performed as one superset.
     */
    val supersetId: Int? = null,
    /**
     * Round of the superset this set belongs to, starting at `0`.
     */
    val roundIndex: Int? = null,
    /**
     * How many cuts this working set is meant to have. Zero means a plain set.
     */
    val dropCount: Int = 0,
    /**
     * Percent taken off the bar on every cut.
     */
    val dropPercent: Int = DEFAULT_DROP_PERCENT,
    /**
     * Handle the set was done with, when the exercise has several.
     */
    val gripId: Int? = null,
    val id: Int? = null,
)

val Set.isDrop: Boolean
    get() = parentSetId != null

/**
 * Warm-ups move the joints, not the numbers: they stay out of tonnage, records and suggestions.
 */
val Set.isWarmup: Boolean
    get() = type == SetType.Warmup

/**
 * Sets that add up to the work of the day.
 */
val Set.countsAsWork: Boolean
    get() = !isWarmup

/**
 * Sets whose weight can be compared with any other: no warm-up, no help, no half a rep.
 */
val Set.countsAsClean: Boolean
    get() = countsAsWork && weightNote == null

val Set.rating: Rating
    get() = Rating(repsOrDuration * weight * type.ratingModifier * rir.modifier)

