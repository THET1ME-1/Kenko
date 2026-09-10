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
import kotlinx.datetime.LocalDate

@Immutable
data class Session(
    val date: LocalDate,
    val sets: List<Set>,
    val planId: Int?,
    /**
     * Which day of the plan was performed. Null for sessions written before days were numbered.
     */
    val dayIndex: Int? = null,
    /**
     * Gym the session was written in, so the same exercise can be told apart between gyms.
     */
    val gymId: Int? = null,
    /**
     * How the session is called: given by the lifter, or by the time of day it started.
     */
    val name: String? = null,
    val note: String? = null,
    val photoUri: String? = null,
    /**
     * Epoch seconds of the first set and of the moment the session was closed.
     */
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val id: Int? = null,
) {
    val performExercises: List<Exercise>
        get() = sets.map { it.exercise }.distinct()

    /**
     * A session stays open until it is closed by hand.
     */
    val isFinished: Boolean get() = finishedAt != null

    /**
     * Whether today's training is under way: something is written into it and it is still open.
     *
     * A record with no sets left in it is not a session in progress — deleting the last set
     * leaves the row behind.
     */
    val isRunning: Boolean get() = sets.isNotEmpty() && !isFinished

    /**
     * Minutes between the first set and the closing, when both are known.
     */
    val minutes: Int?
        get() {
            val start = startedAt ?: return null
            val end = finishedAt ?: return null
            return ((end - start) / 60).toInt().coerceAtLeast(0)
        }
}

fun Session(planId: Int, sets: List<Set>) = Session(planId = planId, date = localDate, sets = sets)

/**
 * The one answer both the home screen and the log ask for, so their buttons cannot disagree.
 */
val Session?.isRunning: Boolean
    get() = this?.let { it.sets.isNotEmpty() && !it.isFinished } == true
