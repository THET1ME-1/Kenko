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

package com.looker.kenko.ui.stats

import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.ui.navigation.Routes
import kotlinx.datetime.LocalDate

/**
 * The period travels between screens as three plain values, so a route stays serialisable.
 */
private fun period(from: Int, to: Int, range: String): StatsPeriod = StatsPeriod(
    from = LocalDate.fromEpochDays(from.toLong()),
    to = LocalDate.fromEpochDays(to.toLong()),
    range = runCatching { StatsRange.valueOf(range) }.getOrDefault(StatsRange.Custom),
)

fun Routes.MuscleStats.period(): StatsPeriod = period(from, to, range)

fun Routes.ExerciseStats.period(): StatsPeriod = period(from, to, range)

fun Routes.Report.period(): StatsPeriod = period(from, to, range)

fun muscleRoute(muscle: MuscleGroups, period: StatsPeriod) = Routes.MuscleStats(
    muscle = muscle.name,
    from = period.from.toEpochDays().toInt(),
    to = period.to.toEpochDays().toInt(),
    range = period.range.name,
)

fun exerciseRoute(name: String, period: StatsPeriod) = Routes.ExerciseStats(
    name = name,
    from = period.from.toEpochDays().toInt(),
    to = period.to.toEpochDays().toInt(),
    range = period.range.name,
)

fun reportRoute(period: StatsPeriod) = Routes.Report(
    from = period.from.toEpochDays().toInt(),
    to = period.to.toEpochDays().toInt(),
    range = period.range.name,
)
