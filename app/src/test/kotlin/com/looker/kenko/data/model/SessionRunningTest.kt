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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val press = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)

private fun set() = Set(
    repsOrDuration = 8,
    weight = 80F,
    type = SetType.Standard,
    exercise = press,
    rir = RepsInReserve(2),
)

private fun session(sets: List<Set>, finishedAt: Long? = null) = Session(
    date = LocalDate(2026, 9, 10),
    sets = sets,
    planId = 1,
    finishedAt = finishedAt,
)

class SessionRunningTest {

    @Test
    fun `a session with a set written into it is running`() {
        assertTrue(session(listOf(set())).isRunning)
    }

    @Test
    fun `an empty record left after the sets were deleted is not running`() {
        assertFalse(session(emptyList()).isRunning)
    }

    @Test
    fun `a finished session is not running`() {
        assertFalse(session(listOf(set()), finishedAt = 1_757_000_000).isRunning)
    }

    @Test
    fun `no session at all is not running`() {
        val nothing: Session? = null

        assertFalse(nothing.isRunning)
    }
}
