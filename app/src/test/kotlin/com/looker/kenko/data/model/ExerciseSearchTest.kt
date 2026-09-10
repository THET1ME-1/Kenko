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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val press = Exercise(
    name = "Bench Press",
    target = MuscleGroups.Chest,
    nameRu = "Жим лёжа",
    id = 1,
)

class ExerciseSearchTest {

    @Test
    fun `an empty query keeps every exercise`() {
        assertTrue(press.matchesSearch(""))
        assertTrue(press.matchesSearch("   "))
    }

    @Test
    fun `the russian name is searchable, not only the english one`() {
        assertTrue(press.matchesSearch("жим"))
    }

    @Test
    fun `case and stray spaces do not matter`() {
        assertTrue(press.matchesSearch("  BENCH "))
        assertTrue(press.matchesSearch("ЛЁЖА"))
    }

    @Test
    fun `an exercise that has nothing to do with the query drops out`() {
        assertFalse(press.matchesSearch("присед"))
    }
}
