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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDayGroupTest {

    private val bench = Exercise("Bench Press", MuscleGroups.Chest, id = 1)
    private val pullUp = Exercise("Pull-ups", MuscleGroups.Lats, id = 2)
    private val dips = Exercise("Dips", MuscleGroups.Triceps, id = 3)

    private fun item(
        id: Long,
        exercise: Exercise,
        supersetId: Int? = null,
        order: Int = id.toInt(),
    ) = PlanItem(
        dayIndex = 1,
        exercise = exercise,
        planId = 1,
        supersetId = supersetId,
        order = order,
        id = id,
    )

    @Test
    fun `lone exercises each get their own group`() {
        val groups = listOf(item(1, bench), item(2, pullUp)).toDayGroups()

        assertEquals(2, groups.size)
        assertTrue(groups.all { it is PlanDayGroup.Single })
    }

    @Test
    fun `exercises sharing a superset id collapse into one group`() {
        val groups = listOf(
            item(1, pullUp, supersetId = 7),
            item(2, dips, supersetId = 7),
        ).toDayGroups()

        val superset = groups.single() as PlanDayGroup.Superset
        assertEquals(7, superset.id)
        assertEquals(listOf(pullUp, dips), superset.items.map { it.exercise })
    }

    @Test
    fun `a superset keeps the place of its first exercise`() {
        val groups = listOf(
            item(1, pullUp, supersetId = 7),
            item(2, bench),
            item(3, dips, supersetId = 7),
        ).toDayGroups()

        assertEquals(2, groups.size)
        assertTrue(groups[0] is PlanDayGroup.Superset)
        assertEquals(bench, (groups[1] as PlanDayGroup.Single).item.exercise)
    }

    @Test
    fun `two supersets in one day stay apart`() {
        val groups = listOf(
            item(1, pullUp, supersetId = 1),
            item(2, dips, supersetId = 1),
            item(3, bench, supersetId = 2),
            item(4, pullUp, supersetId = 2),
        ).toDayGroups()

        assertEquals(listOf(1, 2), groups.map { (it as PlanDayGroup.Superset).id })
    }

    @Test
    fun `an empty day has no groups`() {
        assertTrue(emptyList<PlanItem>().toDayGroups().isEmpty())
    }
}

class PlanDaySummaryTest {

    private val bench = Exercise("Bench Press", MuscleGroups.Chest, id = 1)
    private val curl = Exercise("Curls", MuscleGroups.Biceps, id = 2)

    private fun item(
        exercise: Exercise,
        targetSets: Int = 3,
        restSeconds: Int = 90,
        dropCount: Int = 0,
    ) = PlanItem(
        dayIndex = 1,
        exercise = exercise,
        planId = 1,
        targetSets = targetSets,
        restSeconds = restSeconds,
        dropCount = dropCount,
        id = exercise.id!!.toLong(),
    )

    @Test
    fun `an empty day adds up to nothing`() {
        val summary = emptyList<PlanItem>().daySummary()

        assertEquals(0, summary.exercises)
        assertEquals(0, summary.sets)
        assertEquals(0, summary.minutes)
    }

    @Test
    fun `sets count every exercise of the day`() {
        val summary = listOf(item(bench, targetSets = 4), item(curl, targetSets = 3)).daySummary()

        assertEquals(2, summary.exercises)
        assertEquals(7, summary.sets)
    }

    @Test
    fun `a drop set counts as its whole group`() {
        val summary = listOf(item(curl, targetSets = 3, dropCount = 3)).daySummary()

        assertEquals(12, summary.sets)
    }

    @Test
    fun `time counts the work and the rest`() {
        val summary = listOf(item(bench, targetSets = 4, restSeconds = 120)).daySummary()

        assertEquals(11, summary.minutes)
    }
}

class PlanPreviewTest {

    private val bench = Exercise("Bench Press", MuscleGroups.Chest, id = 1)
    private val pullUp = Exercise("Pull-ups", MuscleGroups.Lats, id = 2)
    private val dips = Exercise("Dips", MuscleGroups.Triceps, id = 3)

    private fun item(
        exercise: Exercise,
        targetSets: Int = 3,
        reps: Int = 10,
        repsMax: Int = 10,
        supersetId: Int? = null,
        dropCount: Int = 0,
        bar: Float = 20F,
        left: Float = 0F,
        right: Float = 0F,
    ) = PlanItem(
        dayIndex = 1,
        exercise = exercise,
        planId = 1,
        supersetId = supersetId,
        targetSets = targetSets,
        targetReps = reps,
        targetRepsMax = repsMax,
        barWeight = bar,
        leftWeight = left,
        rightWeight = right,
        dropCount = dropCount,
        id = exercise.id!!.toLong(),
    )

    @Test
    fun `an exact number of reps reads as one number`() {
        assertEquals("10", item(bench, reps = 10, repsMax = 10).repsLabel)
    }

    @Test
    fun `a range reads with a dash`() {
        assertEquals("6–10", item(bench, reps = 6, repsMax = 10).repsLabel)
    }

    @Test
    fun `weight adds up from the bar and both sides`() {
        assertEquals(45F, item(bench, bar = 20F, left = 12.5F, right = 12.5F).targetWeight, 0.01F)
    }

    @Test
    fun `sides may differ`() {
        assertEquals(32.5F, item(bench, bar = 20F, left = 7.5F, right = 5F).targetWeight, 0.01F)
    }

    @Test
    fun `a planned exercise previews as many rows as it has sets`() {
        val blocks = listOf(item(bench, targetSets = 4)).toPreviewBlocks()

        val block = blocks.single() as SessionBlock.SingleExercise
        assertEquals(4, block.chains.size)
        assertEquals(20F, block.chains.first().set.weight, 0.01F)
    }

    @Test
    fun `a planned superset previews as rounds`() {
        val blocks = listOf(
            item(pullUp, supersetId = 3, targetSets = 3),
            item(dips, supersetId = 3, targetSets = 3),
        ).toPreviewBlocks()

        val superset = blocks.single() as SessionBlock.Superset
        assertEquals(3, superset.rounds.size)
        assertTrue(superset.rounds.all { it.chains.size == 2 })
    }

    @Test
    fun `a planned drop set previews as a group`() {
        val blocks = listOf(item(bench, targetSets = 2, dropCount = 3)).toPreviewBlocks()

        val chain = (blocks.single() as SessionBlock.SingleExercise).chains.first()
        assertTrue(chain.isDropSet)
        assertEquals(4, chain.steps.size)
    }
}
