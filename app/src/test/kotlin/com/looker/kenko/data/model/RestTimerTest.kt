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

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {

    private val start = Instant.fromEpochSeconds(1_000_000)

    private fun timer(seconds: Int = 90) = RestTimer(
        exerciseName = "Bench Press",
        totalSeconds = seconds,
        endsAt = start + seconds.seconds,
    )

    @Test
    fun `seconds left shrink as time passes`() {
        val timer = timer(90)

        assertEquals(90, timer.secondsLeft(start))
        assertEquals(60, timer.secondsLeft(start + 30.seconds))
        assertEquals(0, timer.secondsLeft(start + 90.seconds))
    }

    @Test
    fun `a timer never counts below zero`() {
        assertEquals(0, timer(90).secondsLeft(start + 500.seconds))
    }

    @Test
    fun `a timer is done only after the last second`() {
        val timer = timer(90)

        assertFalse(timer.isDone(start + 89.seconds))
        assertTrue(timer.isDone(start + 90.seconds))
    }

    @Test
    fun `adding thirty seconds pushes the finish line`() {
        val timer = timer(90).shifted(by = 30, now = start + 30.seconds)

        assertEquals(90, timer.secondsLeft(start + 30.seconds))
    }

    @Test
    fun `cutting more than what is left stops at zero`() {
        val timer = timer(90).shifted(by = -300, now = start + 30.seconds)

        assertEquals(0, timer.secondsLeft(start + 30.seconds))
        assertTrue(timer.isDone(start + 30.seconds))
    }

    @Test
    fun `rest is written the way a gym clock reads`() {
        assertEquals("1:30", formatSeconds(90))
        assertEquals("0:05", formatSeconds(5))
        assertEquals("10:00", formatSeconds(600))
        assertEquals("0:00", formatSeconds(-5))
    }
}
