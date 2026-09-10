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
import org.junit.Assert.assertNull
import org.junit.Test

private val bench = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)
private val row = Exercise(name = "Barbell Row", target = MuscleGroups.UpperBack, id = 2)
private val curl = Exercise(name = "Barbell Curls", target = MuscleGroups.Biceps, id = 3)

private fun set(
    weight: Float,
    reps: Int,
    exercise: Exercise = bench,
    type: SetType = SetType.Standard,
) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = type,
    exercise = exercise,
    rir = RepsInReserve(2),
)

private fun session(date: LocalDate, dayIndex: Int?, vararg sets: Set) =
    Session(date = date, sets = sets.toList(), planId = 1, dayIndex = dayIndex)

class LastTimeTest {

    private val today = LocalDate(2026, 9, 10)

    @Test
    fun `last time is the same day of the plan, not merely the last session`() {
        val sessions = listOf(
            session(LocalDate(2026, 9, 3), 3, set(80F, 8)),
            session(LocalDate(2026, 9, 8), 1, set(60F, 10, row)),
        )

        val last = sessions.lastTimeOf(dayIndex = 3, before = today)

        assertEquals(LocalDate(2026, 9, 3), last?.date)
    }

    @Test
    fun `without a plan day the last session with work counts`() {
        val sessions = listOf(
            session(LocalDate(2026, 9, 3), null, set(80F, 8)),
            session(LocalDate(2026, 9, 8), null, set(60F, 10, row)),
        )

        val last = sessions.lastTimeOf(dayIndex = null, before = today)

        assertEquals(LocalDate(2026, 9, 8), last?.date)
    }

    @Test
    fun `warm-ups stay out of the volume`() {
        val sessions = listOf(
            session(
                LocalDate(2026, 9, 3),
                3,
                set(20F, 10, type = SetType.Warmup),
                set(80F, 8),
            ),
        )

        val last = sessions.lastTimeOf(dayIndex = 3, before = today)

        assertEquals(640F, last?.volume)
    }

    @Test
    fun `top sets name every exercise once, heaviest first`() {
        val sessions = listOf(
            session(
                LocalDate(2026, 9, 3),
                3,
                set(80F, 8),
                set(75F, 8),
                set(60F, 10, row),
                set(20F, 12, curl),
            ),
        )

        val top = sessions.lastTimeOf(dayIndex = 3, before = today)?.topSets.orEmpty()

        assertEquals(listOf("Bench Press", "Barbell Row", "Barbell Curls"), top.map { it.exercise.name })
        assertEquals(80F, top.first().weight)
    }

    @Test
    fun `a day trained for the first time has no last time`() {
        val sessions = listOf(session(LocalDate(2026, 9, 3), 1, set(80F, 8)))

        assertNull(sessions.lastTimeOf(dayIndex = 3, before = today))
    }

    @Test
    fun `streak counts weeks in a row`() {
        val sessions = listOf(
            session(LocalDate(2026, 9, 8), 1, set(80F, 8)),
            session(LocalDate(2026, 9, 1), 1, set(80F, 8)),
            session(LocalDate(2026, 8, 26), 1, set(80F, 8)),
        )

        assertEquals(3, sessions.weekStreak(today))
    }

    @Test
    fun `a missed week ends the streak`() {
        val sessions = listOf(
            session(LocalDate(2026, 9, 8), 1, set(80F, 8)),
            session(LocalDate(2026, 8, 18), 1, set(80F, 8)),
        )

        assertEquals(1, sessions.weekStreak(today))
    }

    @Test
    fun `a week without training yet keeps the streak of the week before`() {
        val sessions = listOf(session(LocalDate(2026, 9, 3), 1, set(80F, 8)))

        assertEquals(1, sessions.weekStreak(today))
    }
}
