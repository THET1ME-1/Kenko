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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DropChainTest {

    @Test
    fun `the chain of the mockup comes out to the gram`() {
        val chain = buildDropChain(base = 40F, drops = 3, reps = 12, percent = 20)

        assertEquals(listOf(40F, 32.5F, 25F, 20F), chain.map { it.weight })
    }

    @Test
    fun `every weight lands on a loadable step`() {
        val chain = buildDropChain(base = 47.5F, drops = 4, reps = 10, percent = 15)

        assertTrue(chain.all { (it.weight / WEIGHT_STEP) % 1F == 0F })
    }

    @Test
    fun `reps fall by three per cut but never below four`() {
        val chain = buildDropChain(base = 40F, drops = 4, reps = 10, percent = 20)

        assertEquals(listOf(10, 7, 4, 4, 4), chain.map { it.reps })
    }

    @Test
    fun `the working set keeps its own weight and reps`() {
        val chain = buildDropChain(base = 22.5F, drops = 2, reps = 15, percent = 25)

        assertEquals(22.5F, chain.first().weight)
        assertEquals(15, chain.first().reps)
        assertTrue(!chain.first().isAutoWeight)
        assertTrue(chain.drop(1).all { it.isAutoWeight })
    }

    @Test
    fun `a group without drops is just the working set`() {
        assertEquals(1, buildDropChain(base = 60F, drops = 0, reps = 8).size)
    }

    @Test
    fun `the cut stays inside what the steppers allow`() {
        val tooSmall = buildDropChain(base = 100F, drops = 1, reps = 10, percent = 1)
        val tooLarge = buildDropChain(base = 100F, drops = 1, reps = 10, percent = 90)

        assertEquals(95F, tooSmall[1].weight)
        assertEquals(65F, tooLarge[1].weight)
    }

    @Test
    fun `volume adds up over the whole group`() {
        val chain = buildDropChain(base = 40F, drops = 3, reps = 12, percent = 20)

        val expected = chain.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
        assertEquals(expected, chain.volume(), 0.01F)
    }

    @Test
    fun `the chain reads as one line`() {
        val chain = buildDropChain(base = 40F, drops = 2, reps = 12, percent = 20)

        assertEquals("40.0 → 32.5 → 25.0", chain.chainLabel())
    }

    @Test
    fun `one rep max follows Epley`() {
        assertEquals(56F, oneRepMax(40F, 12), 0.01F)
        assertEquals(100F, oneRepMax(100F, 0), 0.01F)
    }
}

class SetChainStepsTest {

    private val bench = Exercise("Bench Press", MuscleGroups.Chest, id = 1)

    private fun set(
        id: Int,
        weight: Float,
        reps: Int,
        dropCount: Int = 0,
        dropPercent: Int = DEFAULT_DROP_PERCENT,
        parentSetId: Int? = null,
        dropIndex: Int = 0,
    ) = Set(
        repsOrDuration = reps,
        weight = weight,
        type = com.looker.kenko.data.local.model.SetType.Drop,
        exercise = bench,
        rir = RepsInReserve(2),
        parentSetId = parentSetId,
        dropIndex = dropIndex,
        dropCount = dropCount,
        dropPercent = dropPercent,
        id = id,
    )

    @Test
    fun `a group shows the cuts nobody did yet`() {
        val chain = SetChain(set(1, 40F, 12, dropCount = 3))

        val steps = chain.steps
        assertEquals(4, steps.size)
        assertEquals(listOf(true, false, false, false), steps.map { it.isPerformed })
        assertEquals(listOf(40F, 32.5F, 25F, 20F), steps.map { it.weight })
    }

    @Test
    fun `a performed cut replaces the computed one`() {
        val chain = SetChain(
            set = set(1, 40F, 12, dropCount = 3),
            drops = listOf(set(2, 30F, 8, parentSetId = 1, dropIndex = 1)),
        )

        val steps = chain.steps
        assertEquals(listOf(true, true, false, false), steps.map { it.isPerformed })
        assertEquals(30F, steps[1].weight)
        assertEquals(8, steps[1].reps)
    }

    @Test
    fun `more cuts than planned still all show up`() {
        val chain = SetChain(
            set = set(1, 40F, 12, dropCount = 1),
            drops = listOf(
                set(2, 30F, 8, parentSetId = 1, dropIndex = 1),
                set(3, 20F, 6, parentSetId = 1, dropIndex = 2),
            ),
        )

        assertEquals(3, chain.steps.size)
        assertTrue(chain.steps.all { it.isPerformed })
    }

    @Test
    fun `volume counts the working set and every cut`() {
        val chain = SetChain(
            set = set(1, 40F, 12, dropCount = 1),
            drops = listOf(set(2, 30F, 8, parentSetId = 1, dropIndex = 1)),
        )

        assertEquals(720F, chain.volume, 0.01F)
    }

    @Test
    fun `a plain set is not a group`() {
        assertTrue(!SetChain(set(1, 40F, 12)).isDropSet)
    }
}
