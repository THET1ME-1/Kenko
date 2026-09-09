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

/**
 * A day of a plan reads as a list of groups: a lone exercise, or a superset of several.
 */
@Immutable
sealed interface PlanDayGroup {

    val items: List<PlanItem>

    data class Single(val item: PlanItem) : PlanDayGroup {
        override val items: List<PlanItem> get() = listOf(item)
    }

    data class Superset(
        val id: Int,
        override val items: List<PlanItem>,
    ) : PlanDayGroup
}

/**
 * Folds the items of one day into groups, keeping the order of the day.
 *
 * A superset takes the place of its first exercise, so tying two exercises together
 * never shuffles the day.
 */
fun List<PlanItem>.toDayGroups(): List<PlanDayGroup> {
    val groups = mutableListOf<PlanDayGroup>()
    val supersetPosition = mutableMapOf<Int, Int>()
    for (item in this) {
        val supersetId = item.supersetId
        if (supersetId == null) {
            groups += PlanDayGroup.Single(item)
            continue
        }
        val position = supersetPosition[supersetId]
        if (position == null) {
            supersetPosition[supersetId] = groups.size
            groups += PlanDayGroup.Superset(supersetId, listOf(item))
        } else {
            val group = groups[position] as PlanDayGroup.Superset
            groups[position] = group.copy(items = group.items + item)
        }
    }
    return groups
}

/**
 * What a day of the plan adds up to, shown under its title.
 */
@Immutable
data class PlanDaySummary(
    val exercises: Int,
    val sets: Int,
    val minutes: Int,
)

/**
 * Seconds a working set takes before the rest starts.
 */
private const val SECONDS_PER_SET = 40

fun List<PlanItem>.daySummary(): PlanDaySummary {
    val seconds = sumOf { item ->
        item.setCount * SECONDS_PER_SET + item.targetSets * item.restSeconds
    }
    return PlanDaySummary(
        exercises = size,
        sets = sumOf { it.setCount },
        minutes = (seconds + 59) / 60,
    )
}
