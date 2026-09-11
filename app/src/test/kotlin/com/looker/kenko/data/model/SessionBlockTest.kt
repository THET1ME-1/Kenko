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

import com.looker.kenko.data.local.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBlockTest {

    private val bench = Exercise("Bench Press", MuscleGroups.Chest, id = 1)
    private val pullUp = Exercise("Pull-ups", MuscleGroups.Lats, id = 2)
    private val dips = Exercise("Dips", MuscleGroups.Triceps, id = 3)

    private fun set(
        id: Int,
        exercise: Exercise,
        reps: Int = 10,
        weight: Float = 40F,
        type: SetType = SetType.Standard,
        parentSetId: Int? = null,
        dropIndex: Int = 0,
        supersetId: Int? = null,
        roundIndex: Int? = null,
    ) = Set(
        repsOrDuration = reps,
        weight = weight,
        type = type,
        exercise = exercise,
        rir = RepsInReserve(2),
        parentSetId = parentSetId,
        dropIndex = dropIndex,
        supersetId = supersetId,
        roundIndex = roundIndex,
        id = id,
    )

    @Test
    fun `plain sets of one exercise become one block`() {
        val sets = listOf(set(1, bench), set(2, bench), set(3, bench))

        val blocks = sets.toSessionBlocks()

        assertEquals(1, blocks.size)
        val block = blocks.single() as SessionBlock.SingleExercise
        assertEquals(bench, block.exercise)
        assertEquals(3, block.chains.size)
        assertTrue(block.chains.none { it.isDropSet })
    }

    @Test
    fun `drops hang under their parent set instead of standing alone`() {
        val sets = listOf(
            set(1, bench),
            set(2, bench, type = SetType.Drop),
            set(3, bench, reps = 8, weight = 30F, type = SetType.Drop, parentSetId = 2, dropIndex = 1),
            set(4, bench, reps = 6, weight = 20F, type = SetType.Drop, parentSetId = 2, dropIndex = 2),
        )

        val block = sets.toSessionBlocks().single() as SessionBlock.SingleExercise

        assertEquals(2, block.chains.size)
        val dropChain = block.chains[1]
        assertTrue(dropChain.isDropSet)
        assertEquals(listOf(1, 2), dropChain.drops.map { it.dropIndex })
        assertEquals(listOf(30F, 20F), dropChain.drops.map { it.weight })
    }

    @Test
    fun `drops are ordered by drop index even when stored out of order`() {
        val sets = listOf(
            set(1, bench),
            set(3, bench, weight = 20F, parentSetId = 1, dropIndex = 2),
            set(2, bench, weight = 30F, parentSetId = 1, dropIndex = 1),
        )

        val chain = (sets.toSessionBlocks().single() as SessionBlock.SingleExercise).chains.single()

        assertEquals(listOf(30F, 20F), chain.drops.map { it.weight })
    }

    @Test
    fun `drop rating adds up over the whole chain`() {
        val parent = set(1, bench, reps = 10, weight = 40F, type = SetType.Drop)
        val drop = set(2, bench, reps = 8, weight = 30F, type = SetType.Drop, parentSetId = 1, dropIndex = 1)

        val chain = SetChain(parent, listOf(drop))

        assertEquals(parent.rating.value + drop.rating.value, chain.totalRating.value, 0.001F)
    }

    private fun planItem(
        exercise: Exercise,
        supersetId: Int? = null,
        targetSets: Int = 3,
        id: Long = exercise.id!!.toLong(),
    ) = PlanItem(
        dayIndex = 1,
        exercise = exercise,
        planId = 1,
        supersetId = supersetId,
        targetSets = targetSets,
        id = id,
    )

    @Test
    fun `sets sharing a superset id become rounds of one block`() {
        val sets = listOf(
            set(1, pullUp, supersetId = 1, roundIndex = 0),
            set(2, dips, supersetId = 1, roundIndex = 0),
            set(3, pullUp, supersetId = 1, roundIndex = 1),
            set(4, dips, supersetId = 1, roundIndex = 1),
        )

        val block = sets.toSessionBlocks().single() as SessionBlock.Superset

        assertEquals(listOf(pullUp, dips), block.exercises)
        assertEquals(2, block.rounds.size)
        assertEquals(listOf(0, 1), block.rounds.map { it.index })
        assertTrue(block.rounds.all { round -> round.chains.size == 2 })
    }

    @Test
    fun `planned exercise without sets still gets a block`() {
        val blocks = listOf(set(1, bench))
            .toSessionBlocks(plannedItems = listOf(planItem(bench), planItem(pullUp)))

        assertEquals(2, blocks.size)
        val empty = blocks[1] as SessionBlock.SingleExercise
        assertEquals(pullUp, empty.exercise)
        assertTrue(empty.chains.isEmpty())
    }

    @Test
    fun `blocks follow the plan order, not the order sets were added`() {
        val sets = listOf(set(1, dips), set(2, bench))

        val blocks = sets.toSessionBlocks(
            plannedItems = listOf(planItem(bench), planItem(pullUp), planItem(dips)),
        )

        assertEquals(
            listOf(bench, pullUp, dips),
            blocks.map { (it as SessionBlock.SingleExercise).exercise },
        )
    }

    @Test
    fun `an exercise outside the plan lands after the planned ones`() {
        val sets = listOf(set(1, dips), set(2, bench))

        val blocks = sets.toSessionBlocks(plannedItems = listOf(planItem(bench)))

        assertEquals(
            listOf(bench, dips),
            blocks.map { (it as SessionBlock.SingleExercise).exercise },
        )
    }

    @Test
    fun `a superset takes the plan position of its first exercise`() {
        val sets = listOf(
            set(1, bench),
            set(2, pullUp, supersetId = 1, roundIndex = 0),
            set(3, dips, supersetId = 1, roundIndex = 0),
        )

        val blocks = sets.toSessionBlocks(
            plannedItems = listOf(
                planItem(pullUp, supersetId = 1),
                planItem(dips, supersetId = 1),
                planItem(bench),
            ),
        )

        assertTrue(blocks.first() is SessionBlock.Superset)
        assertEquals(bench, (blocks[1] as SessionBlock.SingleExercise).exercise)
    }

    @Test
    fun `a superset planned but not started still shows its exercises`() {
        val blocks = emptyList<Set>().toSessionBlocks(
            plannedItems = listOf(
                planItem(pullUp, supersetId = 4),
                planItem(dips, supersetId = 4),
            ),
        )

        val superset = blocks.single() as SessionBlock.Superset
        assertEquals(listOf(pullUp, dips), superset.exercises)
        assertTrue(superset.rounds.isEmpty())
        assertEquals(3, superset.roundsLeft)
    }

    @Test
    fun `sets left counts down as the plan is worked through`() {
        val blocks = listOf(set(1, bench), set(2, bench))
            .toSessionBlocks(plannedItems = listOf(planItem(bench, targetSets = 4)))

        assertEquals(2, (blocks.single() as SessionBlock.SingleExercise).setsLeft)
    }

    @Test
    fun `rounds left counts the shortest planned exercise of the superset`() {
        val sets = listOf(
            set(1, pullUp, supersetId = 4, roundIndex = 0),
            set(2, dips, supersetId = 4, roundIndex = 0),
        )

        val blocks = sets.toSessionBlocks(
            plannedItems = listOf(
                planItem(pullUp, supersetId = 4, targetSets = 4),
                planItem(dips, supersetId = 4, targetSets = 3),
            ),
        )

        assertEquals(2, (blocks.single() as SessionBlock.Superset).roundsLeft)
    }

    @Test
    fun `an exercise done outside the plan keeps its sets`() {
        val blocks = listOf(set(1, dips), set(2, dips)).toSessionBlocks()

        val block = blocks.single() as SessionBlock.SingleExercise
        assertEquals(dips, block.exercise)
        assertEquals(2, block.chains.size)
        assertEquals(0, block.setsLeft)
    }

    @Test
    fun `exercises outside the plan follow the order of the day`() {
        val sets = listOf(
            set(1, bench),
            set(2, dips),
            set(3, pullUp),
        )

        val blocks = sets.toSessionBlocks(todayOrder = mapOf(2 to 0, 3 to 1, 1 to 2))

        assertEquals(listOf(pullUp, dips, bench), blocks.map { it.exercises.first() })
    }

    @Test
    fun `an exercise added today can stand above the planned ones`() {
        val plan = listOf(planItem(bench), planItem(pullUp))
        val sets = listOf(
            set(1, bench),
            set(2, dips),
        )

        val blocks = sets.toSessionBlocks(
            plannedItems = plan,
            todayOrder = mapOf(3 to 0, 1 to 1, 2 to 2),
        )

        assertEquals(listOf(dips, bench, pullUp), blocks.map { it.exercises.first() })
    }

    @Test
    fun `a superset built during a session always offers the next round`() {
        val sets = listOf(
            set(1, pullUp, supersetId = 3, roundIndex = 0),
            set(2, dips, supersetId = 3, roundIndex = 0),
        )

        val block = sets.toSessionBlocks().filterIsInstance<SessionBlock.Superset>().single()

        assertEquals(1, block.closedRounds)
        assertEquals(2, block.plannedRounds)
        assertEquals(1, block.roundsLeft)
    }

    @Test
    fun `a planned superset stops at the rounds the plan asks for`() {
        val plan = listOf(
            planItem(pullUp, supersetId = 3, targetSets = 2),
            planItem(dips, supersetId = 3, targetSets = 2),
        )
        val sets = listOf(
            set(1, pullUp, supersetId = 3, roundIndex = 0),
            set(2, dips, supersetId = 3, roundIndex = 0),
            set(3, pullUp, supersetId = 3, roundIndex = 1),
            set(4, dips, supersetId = 3, roundIndex = 1),
        )

        val block = sets.toSessionBlocks(plan).filterIsInstance<SessionBlock.Superset>().single()

        assertEquals(2, block.closedRounds)
        assertEquals(2, block.plannedRounds)
        assertEquals(0, block.roundsLeft)
    }
}
