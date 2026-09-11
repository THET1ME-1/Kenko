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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.periodUntil

private const val DAYS_IN_WEEK = 7

/**
 * Everything the profile says about the lifter in one pass over the journal: how long the
 * training has been going, how much has been lifted and what was beaten last.
 */
@Immutable
data class ProfileSummary(
    val sessions: Int = 0,
    val sets: Int = 0,
    val volume: Float = 0F,
    /**
     * Minutes of the sessions that were closed by hand; an open one has no length yet.
     */
    val minutes: Int = 0,
    val first: LocalDate? = null,
    val last: LocalDate? = null,
    val lastMinutes: Int? = null,
    /**
     * Weeks in a row that hold at least one session, counting back from this week.
     */
    val streakWeeks: Int = 0,
    val freshRecord: Record? = null,
)

fun List<Session>.profileSummary(today: LocalDate): ProfileSummary {
    if (isEmpty()) return ProfileSummary()
    var sets = 0
    var volume = 0F
    var minutes = 0
    forEach { session ->
        session.sets.forEach { set ->
            if (set.countsAsWork) {
                sets++
                volume += set.volume
            }
        }
        minutes += session.minutes ?: 0
    }
    val lastSession = maxByOrNull { it.date }
    return ProfileSummary(
        sessions = size,
        sets = sets,
        volume = volume,
        minutes = minutes,
        first = minOf { it.date },
        last = lastSession?.date,
        lastMinutes = lastSession?.minutes,
        streakWeeks = streakWeeks(today),
        freshRecord = records().maxWithOrNull(compareBy({ it.date }, { it.estimatedMax })),
    )
}

/**
 * A week counts when something is written into it. The streak starts at this week, and a week
 * that has only just begun does not break it — Monday is not a missed week yet.
 */
private fun List<Session>.streakWeeks(today: LocalDate): Int {
    val weeks = mapTo(mutableSetOf()) { it.date.weekStart() }
    val thisWeek = today.weekStart()
    var week = when {
        thisWeek in weeks -> thisWeek
        thisWeek - DAYS_IN_WEEK in weeks -> thisWeek - DAYS_IN_WEEK
        else -> return 0
    }
    var count = 0
    while (week in weeks) {
        count++
        week -= DAYS_IN_WEEK
    }
    return count
}

private fun LocalDate.weekStart(): Int = toEpochDays().toInt() - dayOfWeek.ordinal

/**
 * Whole months between the first session and today: a journal started this month reads as zero.
 */
fun trainingMonths(from: LocalDate, to: LocalDate): Int {
    val period = from.periodUntil(to)
    return period.years * 12 + period.months
}
