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
 * How the log is cut into pieces: a year of training is unreadable as one list.
 */
enum class SessionGrouping {
    None,
    Month,
    Year,
    PlanDay,
}

/**
 * What a group of sessions is called. The screen turns it into words, the model only names it.
 */
@Immutable
sealed interface SessionGroupKey {

    data object All : SessionGroupKey

    data class Month(val year: Int, val month: Int) : SessionGroupKey

    data class Year(val year: Int) : SessionGroupKey

    /**
     * Sessions written before days were numbered have no day of their own: [index] is null.
     */
    data class PlanDay(val index: Int?) : SessionGroupKey
}

@Immutable
data class SessionGroup(
    val key: SessionGroupKey,
    val sessions: List<Session>,
)

/**
 * Cuts the log the way the lifter asked for, newest first inside every group.
 *
 * Days of the plan run by their number instead, because that is how the plan reads them.
 */
fun List<Session>.groupSessions(grouping: SessionGrouping): List<SessionGroup> {
    val newestFirst = sortedByDescending { it.date }
    return when (grouping) {
        SessionGrouping.None -> if (isEmpty()) {
            emptyList()
        } else {
            listOf(SessionGroup(SessionGroupKey.All, newestFirst))
        }

        SessionGrouping.Month -> newestFirst
            .groupBy { SessionGroupKey.Month(it.date.year, it.date.monthNumber) }
            .map { (key, sessions) -> SessionGroup(key, sessions) }

        SessionGrouping.Year -> newestFirst
            .groupBy { SessionGroupKey.Year(it.date.year) }
            .map { (key, sessions) -> SessionGroup(key, sessions) }

        SessionGrouping.PlanDay -> newestFirst
            .groupBy { SessionGroupKey.PlanDay(it.dayIndex) }
            .toList()
            .sortedBy { (key, _) -> key.index ?: Int.MAX_VALUE }
            .map { (key, sessions) -> SessionGroup(key, sessions) }
    }
}
