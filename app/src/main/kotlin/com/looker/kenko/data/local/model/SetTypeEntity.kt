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

package com.looker.kenko.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("set_type")
data class SetTypeEntity(
    @PrimaryKey
    val type: SetType,
    val modifier: Float,
)

/**
 * What kind of work the set was.
 *
 * The type is not a label: warm-ups stay out of the tonnage and the records, a set taken to
 * failure says the reps were not planned, and a cluster says the reps came in pieces.
 */
enum class SetType(val ratingModifier: Float) {
    /** Getting the weight moving. Counted nowhere. */
    Warmup(WARMUP_SET_RATING_MODIFIER),
    Standard(STANDARD_SET_RATING_MODIFIER),
    Drop(DROP_SET_RATING_MODIFIER),
    RestPause(REST_PAUSE_SET_RATING_MODIFIER),

    /** Planned pieces with short pauses: 3+3+3 instead of nine in a row. */
    Cluster(CLUSTER_SET_RATING_MODIFIER),

    /** As many reps as there were, not as many as the plan asked for. */
    Amrap(AMRAP_SET_RATING_MODIFIER),
}

/**
 * Why the weight of this set cannot be compared with a clean one.
 *
 * Such sets still move iron, so they count towards the tonnage — but they never claim a record
 * and never set the correction between gyms.
 */
enum class WeightNote {
    /** Cut range of motion. */
    Partials,

    /** Only the lowering half, usually heavier. */
    Negatives,

    /** Somebody helped on the way up. */
    Assisted,

    /** Held at the bottom, usually lighter. */
    Paused,
}

private const val WARMUP_SET_RATING_MODIFIER: Float = 0.0F
private const val STANDARD_SET_RATING_MODIFIER: Float = 1.0F
private const val DROP_SET_RATING_MODIFIER: Float = 1.35F
private const val REST_PAUSE_SET_RATING_MODIFIER: Float = 1.2F
private const val CLUSTER_SET_RATING_MODIFIER: Float = 1.25F
private const val AMRAP_SET_RATING_MODIFIER: Float = 1.15F

fun defaultSetTypes() = listOf(
    SetTypeEntity(SetType.Warmup, WARMUP_SET_RATING_MODIFIER),
    SetTypeEntity(SetType.Standard, STANDARD_SET_RATING_MODIFIER),
    SetTypeEntity(SetType.Drop, DROP_SET_RATING_MODIFIER),
    SetTypeEntity(SetType.RestPause, REST_PAUSE_SET_RATING_MODIFIER),
    SetTypeEntity(SetType.Cluster, CLUSTER_SET_RATING_MODIFIER),
    SetTypeEntity(SetType.Amrap, AMRAP_SET_RATING_MODIFIER),
)
