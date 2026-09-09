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
 * A performed set together with the drops hanging under it.
 *
 * A plain set has an empty [drops] list, a drop set carries one entry per weight cut.
 */
@Immutable
data class SetChain(
    val set: Set,
    val drops: List<Set> = emptyList(),
) {
    val isDropSet: Boolean get() = drops.isNotEmpty()

    val totalRating: Rating
        get() = drops.fold(set.rating) { total, drop -> total + drop.rating }
}

/**
 * One round of a superset: the same position of every exercise in the group.
 */
@Immutable
data class SupersetRound(
    val index: Int,
    val chains: List<SetChain>,
)

/**
 * A session is read as a list of blocks: either a single exercise with its sets under it,
 * or a superset performed in rounds.
 */
@Immutable
sealed interface SessionBlock {

    val exercises: List<Exercise>

    data class SingleExercise(
        val exercise: Exercise,
        val chains: List<SetChain>,
        val plan: PlanItem? = null,
    ) : SessionBlock {
        override val exercises: List<Exercise> get() = listOf(exercise)

        /**
         * Sets of the plan still waiting to be performed.
         */
        val setsLeft: Int
            get() = ((plan?.targetSets ?: 0) - chains.size).coerceAtLeast(0)
    }

    data class Superset(
        val id: Int,
        override val exercises: List<Exercise>,
        val rounds: List<SupersetRound>,
        val plan: List<PlanItem> = emptyList(),
    ) : SessionBlock {

        /**
         * Rounds of the plan still waiting to be performed.
         */
        val roundsLeft: Int
            get() {
                val planned = plan.minOfOrNull { it.targetSets } ?: return 0
                return (planned - rounds.size).coerceAtLeast(0)
            }
    }
}

/**
 * Folds the flat set list of a session into blocks.
 *
 * Drops are attached to their parent set, exercises tied together in the plan become one superset,
 * and every exercise of [plannedItems] shows up even when nothing was performed yet.
 * Blocks follow the plan order, exercises outside the plan follow the order they were performed in.
 */
fun List<Set>.toSessionBlocks(plannedItems: List<PlanItem> = emptyList()): List<SessionBlock> {
    val dropsByParent = filter { it.parentSetId != null }
        .groupBy { it.parentSetId }
        .mapValues { (_, drops) -> drops.sortedBy { it.dropIndex } }

    val chains = filter { it.parentSetId == null }
        .map { set -> SetChain(set, dropsByParent[set.id].orEmpty()) }

    val chainsByExercise = chains.groupBy { it.set.exercise }
    val used = mutableSetOf<Exercise>()
    val blocks = mutableListOf<SessionBlock>()

    for (group in plannedItems.toDayGroups()) {
        when (group) {
            is PlanDayGroup.Single -> {
                val exercise = group.item.exercise
                used += exercise
                blocks += SessionBlock.SingleExercise(
                    exercise = exercise,
                    chains = chainsByExercise[exercise].orEmpty(),
                    plan = group.item,
                )
            }

            is PlanDayGroup.Superset -> {
                val exercises = group.items.map { it.exercise }
                used += exercises
                blocks += supersetBlock(
                    id = group.id,
                    exercises = exercises,
                    chains = exercises.flatMap { chainsByExercise[it].orEmpty() },
                    plan = group.items,
                )
            }
        }
    }

    val loose = chains.filter { it.set.exercise !in used }
    val looseSupersets = loose
        .filter { it.set.supersetId != null }
        .groupBy { it.set.supersetId!! }
    for ((supersetId, supersetChains) in looseSupersets) {
        blocks += supersetBlock(
            id = supersetId,
            exercises = supersetChains.map { it.set.exercise }.distinct(),
            chains = supersetChains,
            plan = emptyList(),
        )
    }
    val looseExercises = loose
        .filter { it.set.supersetId == null }
        .groupBy { it.set.exercise }
    for ((exercise, exerciseChains) in looseExercises) {
        blocks += SessionBlock.SingleExercise(exercise, exerciseChains, plan = null)
    }
    return blocks
}

private fun supersetBlock(
    id: Int,
    exercises: List<Exercise>,
    chains: List<SetChain>,
    plan: List<PlanItem>,
): SessionBlock.Superset {
    val rounds = chains
        .groupBy { it.set.roundIndex ?: 0 }
        .toSortedMap()
        .map { (index, roundChains) ->
            SupersetRound(
                index = index,
                chains = roundChains.sortedBy { chain ->
                    exercises.indexOf(chain.set.exercise).takeIf { it >= 0 } ?: Int.MAX_VALUE
                },
            )
        }
    return SessionBlock.Superset(
        id = id,
        exercises = exercises,
        rounds = rounds,
        plan = plan,
    )
}
