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
import org.junit.Test

class WeightUnitTest {

    @Test
    fun `kilograms are stored and shown as they are`() {
        assertEquals(100F, WeightUnit.Kg.fromKilograms(100F), 0.001F)
        assertEquals(100F, WeightUnit.Kg.toKilograms(100F), 0.001F)
    }

    @Test
    fun `a hundred kilograms read as two hundred and twenty pounds`() {
        assertEquals(220.5F, WeightUnit.Lb.fromKilograms(100F), 0.05F)
    }

    @Test
    fun `pounds typed in come back as the same kilograms`() {
        val kilograms = WeightUnit.Lb.toKilograms(WeightUnit.Lb.fromKilograms(82.5F))

        assertEquals(82.5F, kilograms, 0.01F)
    }

    @Test
    fun `the step of the ruler follows the unit`() {
        assertEquals(2.5F, WeightUnit.Kg.step, 0.001F)
        assertEquals(5F, WeightUnit.Lb.step, 0.001F)
    }

    @Test
    fun `weight reads without a trailing zero, half a kilo survives`() {
        assertEquals("70", WeightUnit.Kg.format(70F))
        assertEquals("72.5", WeightUnit.Kg.format(72.5F))
    }
}
