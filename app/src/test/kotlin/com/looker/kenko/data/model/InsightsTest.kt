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
import com.looker.kenko.data.local.model.WeightNote
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val press = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)
private val curl = Exercise(name = "Barbell Curls", target = MuscleGroups.Biceps, id = 2)

private fun set(
    weight: Float,
    reps: Int,
    exercise: Exercise = press,
    parentSetId: Int? = null,
    id: Int? = null,
    type: SetType = SetType.Standard,
    weightNote: WeightNote? = null,
) = Set(
    repsOrDuration = reps,
    weight = weight,
    type = type,
    exercise = exercise,
    rir = RepsInReserve(2),
    weightNote = weightNote,
    parentSetId = parentSetId,
    id = id,
)

private fun session(
    date: LocalDate,
    vararg sets: Set,
    gymId: Int? = null,
) = Session(date = date, sets = sets.toList(), planId = null, gymId = gymId)

class GhostTest {

    private val monday = LocalDate(2026, 9, 7)
    private val wednesday = LocalDate(2026, 9, 9)
    private val friday = LocalDate(2026, 9, 11)

    @Test
    fun `ghost is the latest session before today that had the exercise`() {
        val sessions = listOf(
            session(monday, set(50F, 10)),
            session(wednesday, set(55F, 10)),
            session(friday, set(60F, 10)),
        )

        val ghost = sessions.ghostOf(press.name, before = friday)

        assertEquals(wednesday, ghost?.date)
        assertEquals(550F, ghost?.volume ?: 0F, 0.01F)
    }

    @Test
    fun `sessions without the exercise are skipped`() {
        val sessions = listOf(
            session(monday, set(50F, 10)),
            session(wednesday, set(40F, 12, exercise = curl)),
        )

        val ghost = sessions.ghostOf(press.name, before = friday)

        assertEquals(monday, ghost?.date)
    }

    @Test
    fun `a session of nothing but warm-ups is not a ghost`() {
        val sessions = listOf(
            session(monday, set(50F, 10)),
            session(wednesday, set(20F, 10, type = SetType.Warmup)),
        )

        assertEquals(monday, sessions.ghostOf(press.name, before = friday)?.date)
    }

    @Test
    fun `first time an exercise is done there is no ghost`() {
        val ghost = listOf(session(monday, set(50F, 10))).ghostOf(curl.name, before = friday)

        assertNull(ghost)
    }

    @Test
    fun `sets line up by their place in the session`() {
        val ghost = listOf(session(monday, set(50F, 10), set(50F, 8)))
            .ghostOf(press.name, before = wednesday)

        assertEquals(8, ghost?.setAt(1)?.repsOrDuration)
        assertNull(ghost?.setAt(2))
    }
}

class RecordTest {

    private val monday = LocalDate(2026, 9, 7)
    private val wednesday = LocalDate(2026, 9, 9)
    private val friday = LocalDate(2026, 9, 11)

    @Test
    fun `record is the set worth the most, not the heaviest`() {
        val sessions = listOf(
            session(monday, set(45F, 1, id = 1)),
            session(wednesday, set(40F, 12, id = 2)),
        )

        val record = sessions.records().single { it.exercise.name == press.name }

        assertEquals(40F, record.weight, 0.01F)
        assertEquals(12, record.reps)
        assertEquals(wednesday, record.date)
    }

    @Test
    fun `record keeps the one it replaced`() {
        val sessions = listOf(
            session(monday, set(50F, 10, id = 1)),
            session(wednesday, set(60F, 10, id = 2)),
        )

        val record = sessions.records().single()

        assertEquals(oneRepMax(50F, 10), record.previous, 0.01F)
        assertTrue(record.gain > 0F)
    }

    @Test
    fun `standing time is counted in days`() {
        val record = listOf(session(monday, set(50F, 10))).records().single()

        assertEquals(4, record.standingFor(friday))
        assertEquals(0, record.standingFor(monday))
    }

    @Test
    fun `a heavier set beats the standing record`() {
        val history = listOf(session(monday, set(50F, 10)))

        val beaten = history.beatsRecord(set(55F, 10), date = wednesday)

        assertNotNull(beaten)
        assertEquals(oneRepMax(50F, 10), beaten?.previous ?: 0F, 0.01F)
    }

    @Test
    fun `an equal set does not beat the record`() {
        val history = listOf(session(monday, set(50F, 10)))

        assertNull(history.beatsRecord(set(50F, 10), date = wednesday))
    }

    @Test
    fun `the very first set of an exercise is a record`() {
        val beaten = emptyList<Session>().beatsRecord(set(40F, 8), date = monday)

        assertEquals(0F, beaten?.previous ?: -1F, 0.01F)
    }

    @Test
    fun `a warm-up never claims a record`() {
        val sessions = listOf(session(monday, set(80F, 5, type = SetType.Warmup)))

        assertTrue(sessions.records().isEmpty())
        assertNull(
            sessions.beatsRecord(set(80F, 5, type = SetType.Warmup), date = wednesday),
        )
    }

    @Test
    fun `a weight with a note never claims a record`() {
        val history = listOf(session(monday, set(50F, 10)))
        val helped = set(70F, 10, weightNote = WeightNote.Assisted)

        assertNull(history.beatsRecord(helped, date = wednesday))
        assertEquals(
            50F,
            listOf(session(monday, set(50F, 10), helped)).records().single().weight,
            0.01F,
        )
    }

    @Test
    fun `bodyweight sets stay out of the records`() {
        val sessions = listOf(session(monday, set(0F, 20)))

        assertTrue(sessions.records().isEmpty())
        assertNull(sessions.beatsRecord(set(0F, 25), date = wednesday))
    }
}

class GymShiftTest {

    private val day1 = LocalDate(2026, 9, 1)
    private val day2 = LocalDate(2026, 9, 3)
    private val day3 = LocalDate(2026, 9, 5)
    private val day4 = LocalDate(2026, 9, 7)

    @Test
    fun `shift compares top weights of the two gyms`() {
        val sessions = listOf(
            session(day1, set(60F, 8), gymId = 1),
            session(day2, set(62F, 8), gymId = 1),
            session(day3, set(55F, 8), gymId = 2),
            session(day4, set(55F, 8), gymId = 2),
        )

        val shift = sessions.gymShift(press.name, from = 1, to = 2)

        assertEquals(55F / 61F, shift?.factor ?: 0F, 0.01F)
        assertTrue(shift?.isMeaningful == true)
        assertEquals(54F, shift?.applyTo(60F) ?: 0F, 1.5F)
    }

    @Test
    fun `one session in a gym is not enough to judge`() {
        val sessions = listOf(
            session(day1, set(60F, 8), gymId = 1),
            session(day2, set(62F, 8), gymId = 1),
            session(day3, set(55F, 8), gymId = 2),
        )

        assertNull(sessions.gymShift(press.name, from = 1, to = 2))
    }

    @Test
    fun `a small difference is noise, not a different machine`() {
        val sessions = listOf(
            session(day1, set(60F, 8), gymId = 1),
            session(day2, set(60F, 8), gymId = 1),
            session(day3, set(59F, 8), gymId = 2),
            session(day4, set(59F, 8), gymId = 2),
        )

        assertTrue(sessions.gymShift(press.name, from = 1, to = 2)?.isMeaningful == false)
    }

    @Test
    fun `warm-ups do not set the correction between gyms`() {
        val sessions = listOf(
            session(day1, set(60F, 8), gymId = 1),
            session(day2, set(60F, 8), gymId = 1),
            session(day3, set(20F, 10, type = SetType.Warmup), set(50F, 8), gymId = 2),
            session(day4, set(20F, 10, type = SetType.Warmup), set(50F, 8), gymId = 2),
        )

        val shift = sessions.gymShift(press.name, from = 1, to = 2)

        assertEquals(50F / 60F, shift?.factor ?: 0F, 0.01F)
    }

    @Test
    fun `the same gym has nothing to compare`() {
        val sessions = listOf(
            session(day1, set(60F, 8), gymId = 1),
            session(day2, set(60F, 8), gymId = 1),
        )

        assertNull(sessions.gymShift(press.name, from = 1, to = 1))
    }

    @Test
    fun `drops do not stand in for a working weight`() {
        val sessions = listOf(
            session(day1, set(60F, 8), set(40F, 6, parentSetId = 1), gymId = 1),
            session(day2, set(60F, 8), gymId = 1),
            session(day3, set(50F, 8), gymId = 2),
            session(day4, set(50F, 8), gymId = 2),
        )

        val shift = sessions.gymShift(press.name, from = 1, to = 2)

        assertEquals(50F / 60F, shift?.factor ?: 0F, 0.01F)
    }
}
