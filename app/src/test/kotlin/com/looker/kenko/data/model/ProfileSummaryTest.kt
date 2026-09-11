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

private val press = Exercise(
    name = "Bench Press",
    target = MuscleGroups.Chest,
    id = 1,
)

private val squat = Exercise(
    name = "Squat",
    target = MuscleGroups.Quads,
    id = 2,
)

private fun workSet(
    weight: Float,
    reps: Int,
    exercise: Exercise = press,
    type: SetType = SetType.Standard,
) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = type,
    exercise = exercise,
    rir = RepsInReserve(2),
)

private fun session(
    date: LocalDate,
    vararg sets: Set,
    startedAt: Long? = null,
    finishedAt: Long? = null,
) = Session(
    date = date,
    sets = sets.toList(),
    planId = null,
    startedAt = startedAt,
    finishedAt = finishedAt,
)

class ProfileSummaryTest {

    private val today = LocalDate(2026, 9, 11)

    @Test
    fun `first session is the earliest date of the journal`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 9), workSet(60F, 10)),
            session(LocalDate(2025, 7, 14), workSet(50F, 10)),
            session(LocalDate(2026, 1, 3), workSet(55F, 10)),
        ).profileSummary(today)

        assertEquals(LocalDate(2025, 7, 14), summary.first)
    }

    @Test
    fun `empty journal has no dates and no numbers`() {
        val summary = emptyList<Session>().profileSummary(today)

        assertNull(summary.first)
        assertNull(summary.last)
        assertNull(summary.freshRecord)
        assertEquals(0, summary.sessions)
        assertEquals(0, summary.sets)
        assertEquals(0F, summary.volume, 0.01F)
        assertEquals(0, summary.streakWeeks)
    }

    @Test
    fun `warmup stays out of tonnage and out of the set count`() {
        val summary = listOf(
            session(
                LocalDate(2026, 9, 9),
                workSet(40F, 10, type = SetType.Warmup),
                workSet(60F, 10),
            ),
        ).profileSummary(today)

        assertEquals(600F, summary.volume, 0.01F)
        assertEquals(1, summary.sets)
    }

    @Test
    fun `only closed sessions bring minutes`() {
        val summary = listOf(
            session(
                LocalDate(2026, 9, 9),
                workSet(60F, 10),
                startedAt = 1_000_000L,
                finishedAt = 1_003_600L,
            ),
            session(LocalDate(2026, 9, 11), workSet(60F, 10), startedAt = 1_100_000L),
        ).profileSummary(today)

        assertEquals(60, summary.minutes)
    }

    @Test
    fun `a session counts even when it holds no sets`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 9), workSet(60F, 10)),
            session(LocalDate(2026, 9, 10)),
        ).profileSummary(today)

        assertEquals(2, summary.sessions)
    }

    @Test
    fun `streak counts weeks in a row up to this one`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 8), workSet(60F, 10)),
            session(LocalDate(2026, 9, 2), workSet(60F, 10)),
            session(LocalDate(2026, 8, 26), workSet(60F, 10)),
        ).profileSummary(today)

        assertEquals(3, summary.streakWeeks)
    }

    @Test
    fun `a quiet start of this week does not break the streak`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 3), workSet(60F, 10)),
            session(LocalDate(2026, 8, 27), workSet(60F, 10)),
        ).profileSummary(today)

        assertEquals(2, summary.streakWeeks)
    }

    @Test
    fun `a skipped week breaks the streak`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 8), workSet(60F, 10)),
            session(LocalDate(2026, 8, 25), workSet(60F, 10)),
        ).profileSummary(today)

        assertEquals(1, summary.streakWeeks)
    }

    @Test
    fun `an old journal with nothing recent has no streak`() {
        val summary = listOf(
            session(LocalDate(2026, 6, 1), workSet(60F, 10)),
        ).profileSummary(today)

        assertEquals(0, summary.streakWeeks)
    }

    @Test
    fun `fresh record is the last one set, not the heaviest`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 1), workSet(140F, 5, squat)),
            session(LocalDate(2026, 9, 8), workSet(92.5F, 3, press)),
        ).profileSummary(today)

        assertEquals(press.name, summary.freshRecord?.exercise?.name)
        assertEquals(LocalDate(2026, 9, 8), summary.freshRecord?.date)
    }

    @Test
    fun `last session carries its date and its length`() {
        val summary = listOf(
            session(LocalDate(2026, 9, 3), workSet(60F, 10)),
            session(
                LocalDate(2026, 9, 10),
                workSet(60F, 10),
                startedAt = 1_000_000L,
                finishedAt = 1_004_320L,
            ),
        ).profileSummary(today)

        assertEquals(LocalDate(2026, 9, 10), summary.last)
        assertEquals(72, summary.lastMinutes)
    }

    @Test
    fun `training months count whole months only`() {
        assertEquals(13, trainingMonths(LocalDate(2025, 7, 14), LocalDate(2026, 9, 11)))
        assertEquals(0, trainingMonths(LocalDate(2026, 9, 1), LocalDate(2026, 9, 11)))
    }
}
