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
import org.junit.Assert.assertTrue
import org.junit.Test

private val bench = Exercise(
    name = "Bench Press",
    target = MuscleGroups.Chest,
    secondaryTargets = listOf(MuscleGroups.Triceps, MuscleGroups.Shoulders),
    id = 1,
)

private val curl = Exercise(
    name = "Barbell Curls",
    target = MuscleGroups.Biceps,
    id = 2,
)

private fun set(weight: Float, reps: Int, exercise: Exercise) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = SetType.Standard,
    exercise = exercise,
    rir = RepsInReserve(2),
)

private fun session(date: LocalDate, vararg sets: Set) =
    Session(date = date, sets = sets.toList(), planId = null)

class StatsTest {

    private val monday = LocalDate(2026, 9, 7)
    private val wednesday = LocalDate(2026, 9, 9)
    private val nextMonday = LocalDate(2026, 9, 14)

    @Test
    fun `week starts on monday and holds seven days`() {
        val period = StatsPeriod.of(StatsRange.Week, wednesday)

        assertEquals(monday, period.from)
        assertEquals(LocalDate(2026, 9, 13), period.to)
        assertEquals(7, period.dayCount)
    }

    @Test
    fun `month period covers the whole month`() {
        val period = StatsPeriod.of(StatsRange.Month, wednesday)

        assertEquals(LocalDate(2026, 9, 1), period.from)
        assertEquals(LocalDate(2026, 9, 30), period.to)
    }

    @Test
    fun `shifting a week steps back exactly seven days`() {
        val previous = StatsPeriod.of(StatsRange.Week, wednesday).shifted(-1)

        assertEquals(LocalDate(2026, 8, 31), previous.from)
        assertEquals(LocalDate(2026, 9, 6), previous.to)
    }

    @Test
    fun `custom period slides by its own length`() {
        val period = StatsPeriod.custom(monday, wednesday)

        assertEquals(3, period.dayCount)
        assertEquals(LocalDate(2026, 9, 4), period.shifted(-1).from)
    }

    @Test
    fun `custom period puts the ends in order`() {
        val period = StatsPeriod.custom(wednesday, monday)

        assertEquals(monday, period.from)
        assertEquals(wednesday, period.to)
    }

    @Test
    fun `sessions outside the period are left out`() {
        val sessions = listOf(
            session(wednesday, set(50F, 10, bench)),
            session(nextMonday, set(60F, 10, bench)),
        )

        val summary = sessions.summarize(StatsPeriod.of(StatsRange.Week, wednesday))

        assertEquals(1, summary.sessions)
        assertEquals(500F, summary.volume, 0.01F)
    }

    @Test
    fun `helping muscle takes half the volume`() {
        val summary = listOf(session(wednesday, set(50F, 10, bench)))
            .summarize(StatsPeriod.of(StatsRange.Week, wednesday))

        val chest = summary.muscles.first { it.muscle == MuscleGroups.Chest }
        val triceps = summary.muscles.first { it.muscle == MuscleGroups.Triceps }

        assertEquals(500F, chest.volume, 0.01F)
        assertEquals(250F, triceps.volume, 0.01F)
        assertEquals(1, chest.directSets)
        assertEquals(0, chest.assistSets)
        assertEquals(1, triceps.assistSets)
    }

    @Test
    fun `untouched muscles stay in the list with zeroes`() {
        val summary = listOf(session(wednesday, set(50F, 10, curl)))
            .summarize(StatsPeriod.of(StatsRange.Week, wednesday))

        val calves = summary.muscles.first { it.muscle == MuscleGroups.Calves }

        assertEquals(0F, calves.volume, 0.01F)
        assertTrue(!calves.isTouched)
        assertEquals(MuscleGroups.entries.size, summary.muscles.size)
    }

    @Test
    fun `exercise keeps the heaviest set of the period`() {
        val sessions = listOf(
            session(monday, set(40F, 12, curl), set(45F, 8, curl)),
            session(wednesday, set(42.5F, 10, curl)),
        )

        val summary = sessions.summarize(StatsPeriod.of(StatsRange.Week, wednesday))
        val load = summary.exerciseLoads.single()

        assertEquals(45F, load.topWeight, 0.01F)
        assertEquals(8, load.topReps)
        assertEquals(3, load.sets)
        assertEquals(2, load.sessions)
        assertEquals(30, load.reps)
    }

    @Test
    fun `every day of the period gets a bar, empty ones included`() {
        val summary = listOf(session(wednesday, set(50F, 10, bench)))
            .summarize(StatsPeriod.of(StatsRange.Week, wednesday))

        assertEquals(7, summary.days.size)
        assertEquals(500F, summary.days.first { it.date == wednesday }.volume, 0.01F)
        assertEquals(0F, summary.days.first { it.date == monday }.volume, 0.01F)
    }

    @Test
    fun `empty period reports nothing instead of dividing by zero`() {
        val summary = emptyList<Session>().summarize(StatsPeriod.of(StatsRange.Week, wednesday))

        assertTrue(summary.isEmpty)
        assertEquals(0F, summary.volumePerSession, 0.01F)
        assertEquals(0, summary.repsPerSet)
        assertNull(summary.heaviestMuscle)
    }

    @Test
    fun `muscle rows collect both its own and helping sets`() {
        val sessions = listOf(session(wednesday, set(50F, 10, bench), set(40F, 10, curl)))

        val rows = sessions.setsOfMuscle(
            MuscleGroups.Triceps,
            StatsPeriod.of(StatsRange.Week, wednesday),
        )

        assertEquals(1, rows.size)
        assertEquals(bench.name, rows.single().second.exercise.name)
    }
}
