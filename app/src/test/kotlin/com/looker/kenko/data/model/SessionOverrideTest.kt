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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val bench = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)
private val incline = Exercise(name = "Incline Press", target = MuscleGroups.Chest, id = 2)
private val row = Exercise(name = "Barbell Row", target = MuscleGroups.UpperBack, id = 3)
private val curl = Exercise(name = "Curls", target = MuscleGroups.Biceps, id = 4)

private fun item(
    exercise: Exercise,
    order: Int,
    sets: Int = 4,
    reps: Int = 8,
    supersetId: Int? = null,
) = PlanItem(
    dayIndex = 1,
    exercise = exercise,
    planId = 1,
    supersetId = supersetId,
    targetSets = sets,
    targetReps = reps,
    targetRepsMax = reps,
    order = order,
    id = order.toLong(),
)

private val day = listOf(item(bench, 0), item(row, 1), item(curl, 2))

class SessionOverrideTest {

    @Test
    fun `a day without overrides stays as it is`() {
        assertEquals(day, day.applyToday(emptyList()))
    }

    @Test
    fun `a skipped exercise leaves the day`() {
        val today = day.applyToday(listOf(SessionOverride(exerciseId = 3, skipped = true)))

        assertEquals(listOf(bench, curl), today.map { it.exercise })
    }

    @Test
    fun `a replaced exercise keeps the goal of the one it stands in for`() {
        val today = day.applyToday(
            listOf(SessionOverride(exerciseId = 1, replacement = incline)),
        )

        assertEquals(incline, today.first().exercise)
        assertEquals(4, today.first().targetSets)
    }

    @Test
    fun `a replacement remembers whose place it took`() {
        val today = day.applyToday(
            listOf(SessionOverride(exerciseId = 1, replacement = incline)),
        )

        assertEquals(1, today.first().replacedExerciseId)
    }

    @Test
    fun `an order of the day beats the order of the plan`() {
        val today = day.applyToday(
            listOf(SessionOverride(exerciseId = 4, orderIndex = -1)),
        )

        assertEquals(listOf(curl, bench, row), today.map { it.exercise })
    }

    @Test
    fun `a goal for today overrides only what it names`() {
        val today = day.applyToday(
            listOf(
                SessionOverride(
                    exerciseId = 1,
                    targetSets = 5,
                    barWeight = 20F,
                    leftWeight = 25F,
                    rightWeight = 25F,
                ),
            ),
        )

        val first = today.first()
        assertEquals(5, first.targetSets)
        assertEquals(70F, first.targetWeight, 0.01F)
        assertEquals(8, first.targetReps)
    }

    @Test
    fun `an override for an exercise outside the day changes nothing`() {
        assertEquals(day, day.applyToday(listOf(SessionOverride(exerciseId = 99, skipped = true))))
    }

    @Test
    fun `a superset survives the reordering of its neighbours`() {
        val paired = listOf(
            item(bench, 0),
            item(row, 1, supersetId = 7),
            item(curl, 2, supersetId = 7),
        )

        val today = paired.applyToday(listOf(SessionOverride(exerciseId = 3, orderIndex = -1)))

        assertTrue(today.filter { it.supersetId == 7 }.size == 2)
    }

    @Test
    fun `two exercises tied together today become a superset`() {
        val today = day.applyToday(
            listOf(
                SessionOverride(exerciseId = 1, supersetId = 1),
                SessionOverride(exerciseId = 3, supersetId = 1, orderIndex = 0),
            ),
        )

        assertEquals(listOf(1, 1), today.filter { it.supersetId != null }.map { it.supersetId })
    }

    @Test
    fun `a superset broken for today falls apart into single exercises`() {
        val paired = listOf(
            item(bench, 0, supersetId = 7),
            item(row, 1, supersetId = 7),
        )

        val today = paired.applyToday(
            paired.map { SessionOverride(exerciseId = it.exercise.id!!, supersetId = NO_SUPERSET) },
        )

        assertTrue(today.all { it.supersetId == null })
    }

    @Test
    fun `skipping every exercise leaves an empty day`() {
        val today = day.applyToday(
            day.map { SessionOverride(exerciseId = it.exercise.id!!, skipped = true) },
        )

        assertTrue(today.isEmpty())
    }
}
