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

import kotlinx.datetime.DayOfWeek
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
        dayOfWeek = DayOfWeek.MONDAY,
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
        dayOfWeek = DayOfWeek.MONDAY,
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
