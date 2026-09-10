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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * The last time this day of the plan was trained: the number today has to beat.
 */
@Immutable
data class LastTime(
    val date: LocalDate,
    val volume: Float,
    val topSets: List<Set>,
)

/**
 * How many of the heaviest sets the home screen names.
 */
private const val TOP_SETS = 3

/**
 * The previous run of the same day of the plan, warm-ups left out.
 *
 * Without a plan day there is nothing to match, so the last session with work in it answers.
 */
fun List<Session>.lastTimeOf(dayIndex: Int?, before: LocalDate): LastTime? {
    val session = asSequence()
        .filter { it.date < before }
        .filter { dayIndex == null || it.dayIndex == dayIndex }
        .filter { session -> session.sets.any { it.countsAsWork } }
        .maxByOrNull { it.date }
        ?: return null
    val work = session.sets.filter { it.countsAsWork }
    return LastTime(
        date = session.date,
        volume = work.sumOf { it.volume.toDouble() }.toFloat(),
        topSets = work
            .groupBy { it.exercise.name }
            .values
            .mapNotNull { sets -> sets.maxByOrNull { oneRepMax(it.weight, it.repsOrDuration) } }
            .sortedByDescending { oneRepMax(it.weight, it.repsOrDuration) }
            .take(TOP_SETS),
    )
}

/**
 * How many weeks in a row ended with at least one session.
 *
 * The running week is not lost yet: while it is still empty, the week before holds the streak.
 */
fun List<Session>.weekStreak(today: LocalDate): Int {
    val weeks = asSequence()
        .filter { session -> session.sets.any { it.countsAsWork } }
        .map { weekStart(it.date) }
        .toSet()
    var week = weekStart(today)
    if (week !in weeks) week = week.minus(DatePeriod(days = 7))
    var streak = 0
    while (week in weeks) {
        streak++
        week = week.minus(DatePeriod(days = 7))
    }
    return streak
}

private fun weekStart(date: LocalDate): LocalDate =
    date.minus(DatePeriod(days = date.dayOfWeek.ordinal))
