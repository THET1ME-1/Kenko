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

import com.looker.kenko.data.local.model.SetType
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val press = Exercise(
    name = "Bench Press",
    target = MuscleGroups.Chest,
    secondaryTargets = listOf(MuscleGroups.Triceps),
    id = 1,
)

private fun set(
    weight: Float,
    reps: Int,
    type: SetType = SetType.Standard,
    id: Int? = null,
) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = type,
    exercise = press,
    rir = RepsInReserve(2),
    id = id,
)

class SessionSummaryTest {

    private val monday = LocalDate(2026, 9, 7)
    private val wednesday = LocalDate(2026, 9, 9)

    @Test
    fun `the name of a session comes from the hour it started`() {
        assertEquals(DayPart.Morning, dayPartOf(7))
        assertEquals(DayPart.Afternoon, dayPartOf(13))
        assertEquals(DayPart.Evening, dayPartOf(22))
        assertEquals(DayPart.Night, dayPartOf(3))
        assertEquals(DayPart.Night, dayPartOf(4))
        assertEquals(DayPart.Morning, dayPartOf(5))
    }

    @Test
    fun `the result counts the work, not the warm-up`() {
        val session = Session(
            date = wednesday,
            sets = listOf(set(20F, 10, type = SetType.Warmup), set(60F, 10), set(60F, 8)),
            planId = null,
        )

        val result = session.result(history = emptyList(), minutes = 55)

        assertEquals(2, result.sets)
        assertEquals(18, result.reps)
        assertEquals(1080F, result.volume, 0.01F)
        assertEquals(1, result.exercises)
        assertEquals(55, result.minutes)
    }

    @Test
    fun `muscles come out weighted against the busiest one`() {
        val session = Session(date = wednesday, sets = listOf(set(60F, 10)), planId = null)

        val result = session.result(history = emptyList(), minutes = 40)

        assertEquals(1F, result.muscles[MuscleGroups.Chest] ?: 0F, 0.01F)
        assertEquals(0.5F, result.muscles[MuscleGroups.Triceps] ?: 0F, 0.01F)
    }

    @Test
    fun `a record of the session is judged against the journal before it`() {
        val history = listOf(
            Session(date = monday, sets = listOf(set(50F, 10, id = 1)), planId = null, id = 1),
        )
        val session = Session(
            date = wednesday,
            sets = listOf(set(55F, 10, id = 2)),
            planId = null,
            id = 2,
        )

        val result = session.result(history = history, minutes = 30)

        assertEquals(1, result.records.size)
        assertEquals(55F, result.records.single().weight, 0.01F)
    }

    @Test
    fun `only the best set of an exercise is reported as its record`() {
        val session = Session(
            date = wednesday,
            sets = listOf(set(50F, 10, id = 1), set(55F, 10, id = 2), set(60F, 10, id = 3)),
            planId = null,
            id = 2,
        )

        val result = session.result(history = emptyList(), minutes = 30)

        assertEquals(1, result.records.size)
        assertEquals(60F, result.records.single().weight, 0.01F)
    }

    @Test
    fun `a session of warm-ups alone has nothing to show`() {
        val session = Session(
            date = wednesday,
            sets = listOf(set(20F, 10, type = SetType.Warmup)),
            planId = null,
        )

        assertTrue(session.result(history = emptyList(), minutes = 10).isEmpty)
    }

    @Test
    fun `minutes are counted between the first set and the closing`() {
        val session = Session(
            date = wednesday,
            sets = emptyList(),
            planId = null,
            startedAt = 1_000_000,
            finishedAt = 1_003_600,
        )

        assertEquals(60, session.minutes)
        assertTrue(session.isFinished)
    }
}
