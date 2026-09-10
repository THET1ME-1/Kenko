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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun exercise(id: Int) = Exercise(name = "Exercise $id", target = MuscleGroups.Chest, id = id)

private fun item(
    exerciseId: Int,
    day: Int = 1,
    supersetId: Int? = null,
    id: Long? = 100L + exerciseId,
) = PlanItem(
    dayIndex = day,
    exercise = exercise(exerciseId),
    planId = 1,
    supersetId = supersetId,
    targetSets = 3,
    targetReps = 8,
    targetRepsMax = 12,
    id = id,
)

class PlanCopyTest {

    @Test
    fun `a copy lands in the day and plan it was asked for`() {
        val copied = listOf(item(1), item(2)).copiedTo(planId = 7, dayIndex = 4, takenSupersetIds = emptySet())

        assertEquals(listOf(7, 7), copied.map { it.planId })
        assertEquals(listOf(4, 4), copied.map { it.dayIndex })
    }

    @Test
    fun `copies are new rows, not the same ones moved`() {
        val copied = listOf(item(1)).copiedTo(planId = 1, dayIndex = 2, takenSupersetIds = emptySet())

        assertNull(copied.single().id)
    }

    @Test
    fun `a superset stays one group, under an id the target day does not use`() {
        val source = listOf(item(1, supersetId = 5), item(2, supersetId = 5), item(3))

        val copied = source.copiedTo(planId = 1, dayIndex = 2, takenSupersetIds = setOf(5, 6))

        val ids = copied.mapNotNull { it.supersetId }.distinct()
        assertEquals(1, ids.size)
        assertNotEquals(5, ids.single())
        assertEquals(listOf(true, true, false), copied.map { it.supersetId != null })
    }

    @Test
    fun `two supersets of one day stay two after the copy`() {
        val source = listOf(item(1, supersetId = 1), item(2, supersetId = 2))

        val copied = source.copiedTo(planId = 1, dayIndex = 3, takenSupersetIds = setOf(1, 2))

        assertEquals(2, copied.mapNotNull { it.supersetId }.distinct().size)
    }

    @Test
    fun `targets travel with the copy`() {
        val copied = listOf(item(1)).copiedTo(planId = 1, dayIndex = 2, takenSupersetIds = emptySet())

        assertEquals(3, copied.single().targetSets)
        assertEquals(8, copied.single().targetReps)
        assertEquals(12, copied.single().targetRepsMax)
    }
}
