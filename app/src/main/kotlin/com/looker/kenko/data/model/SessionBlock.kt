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
    val isDropSet: Boolean get() = set.dropCount > 0 || drops.isNotEmpty()

    val totalRating: Rating
        get() = drops.fold(set.rating) { total, drop -> total + drop.rating }

    /**
     * Volume of the whole group in kilograms, drops included.
     */
    val volume: Float
        get() = set.repsOrDuration * set.weight +
            drops.sumOf { (it.repsOrDuration * it.weight).toDouble() }.toFloat()

    /**
     * The group as it should be read on screen: what was performed, then what is still planned.
     *
     * Cuts nobody did yet carry the weight the app computed for them.
     */
    val steps: List<DropStep>
        get() {
            val planned = buildDropChain(
                base = set.weight,
                drops = maxOf(set.dropCount, drops.size),
                reps = set.repsOrDuration,
                percent = set.dropPercent,
            )
            return planned.mapIndexed { index, step ->
                when {
                    index == 0 -> step.copy(isPerformed = true)
                    index <= drops.size -> {
                        val drop = drops[index - 1]
                        step.copy(
                            weight = drop.weight,
                            reps = drop.repsOrDuration,
                            isPerformed = true,
                        )
                    }

                    else -> step
                }
            }
        }

    val performedSteps: Int get() = drops.size + 1

    val plannedSteps: Int get() = maxOf(set.dropCount, drops.size) + 1
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

        /**
         * Kilograms moved in this exercise, drops counted in.
         */
        val volume: Float
            get() = chains.sumOf { it.volume.toDouble() }.toFloat()

        /**
         * Of that, how much came from drop sets.
         */
        val dropVolume: Float
            get() = chains.filter { it.isDropSet }.sumOf { it.volume.toDouble() }.toFloat()
    }

    data class Superset(
        val id: Int,
        override val exercises: List<Exercise>,
        val rounds: List<SupersetRound>,
        val plan: List<PlanItem> = emptyList(),
    ) : SessionBlock {

        /**
         * How many rounds the plan asks for, never fewer than what was already done.
         */
        val plannedRounds: Int
            get() = maxOf(plan.minOfOrNull { it.targetSets } ?: 0, rounds.size).coerceAtLeast(1)

        /**
         * A round counts as closed once every exercise of the group has a set in it.
         */
        val closedRounds: Int
            get() = rounds.count { round -> round.chains.size >= exercises.size && exercises.isNotEmpty() }

        val roundsLeft: Int
            get() = (plannedRounds - closedRounds).coerceAtLeast(0)

        /**
         * Kilograms of the last round that has any work in it.
         */
        val roundVolume: Float
            get() = rounds.lastOrNull { it.chains.isNotEmpty() }
                ?.chains
                ?.sumOf { it.volume.toDouble() }
                ?.toFloat()
                ?: 0F

        val totalVolume: Float
            get() = rounds.sumOf { round -> round.chains.sumOf { it.volume.toDouble() } }.toFloat()
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
