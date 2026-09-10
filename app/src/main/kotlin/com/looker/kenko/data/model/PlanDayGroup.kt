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

/**
 * Which day of the plan follows [lastDay] in a cycle of [dayCount] days.
 *
 * The cycle wraps: after the last day comes the first one again. With nothing performed yet,
 * or with a plan that has no days, training starts from day one.
 */
fun nextDayIndex(lastDay: Int?, dayCount: Int): Int {
    if (dayCount <= 0 || lastDay == null) return 1
    return if (lastDay >= dayCount) 1 else lastDay + 1
}

/**
 * The same exercises written into another day, or another plan: targets, order and supersets
 * travel along, ids do not.
 *
 * Superset ids are handed out afresh from the ones the target day does not use, so a copied
 * group never merges with a group already sitting there.
 */
fun List<PlanItem>.copiedTo(
    planId: Int,
    dayIndex: Int,
    takenSupersetIds: kotlin.collections.Set<Int>,
): List<PlanItem> {
    var free = (takenSupersetIds.maxOrNull() ?: 0) + 1
    val renamed = mutableMapOf<Int, Int>()
    return map { item ->
        val superset = item.supersetId?.let { old ->
            renamed.getOrPut(old) { free++ }
        }
        item.copy(
            planId = planId,
            dayIndex = dayIndex,
            supersetId = superset,
            id = null,
        )
    }
}
