package com.looker.kenko.data.local.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetTypeTest {

    @Test
    fun `Standard ratingModifier is 1_0`() {
        assertEquals(1.0f, SetType.Standard.ratingModifier)
    }

    @Test
    fun `Drop ratingModifier is 1_35`() {
        assertEquals(1.35f, SetType.Drop.ratingModifier)
    }

    @Test
    fun `RestPause ratingModifier is 1_2`() {
        assertEquals(1.2f, SetType.RestPause.ratingModifier)
    }

    @Test
    fun `Drop modifier is higher than Standard`() {
        assertTrue(SetType.Drop.ratingModifier > SetType.Standard.ratingModifier)
    }

    @Test
    fun `RestPause modifier is higher than Standard`() {
        assertTrue(SetType.RestPause.ratingModifier > SetType.Standard.ratingModifier)
    }

    @Test
    fun `defaultSetTypes returns three entries`() {
        assertEquals(3, defaultSetTypes().size)
    }

    @Test
    fun `defaultSetTypes includes Standard`() {
        assertTrue(defaultSetTypes().any { it.type == SetType.Standard })
    }

    @Test
    fun `defaultSetTypes includes Drop`() {
        assertTrue(defaultSetTypes().any { it.type == SetType.Drop })
    }

    @Test
    fun `defaultSetTypes includes RestPause`() {
        assertTrue(defaultSetTypes().any { it.type == SetType.RestPause })
    }

    @Test
    fun `defaultSetTypes Standard entity modifier matches enum`() {
        val entity = defaultSetTypes().first { it.type == SetType.Standard }
        assertEquals(SetType.Standard.ratingModifier, entity.modifier)
    }

    @Test
    fun `defaultSetTypes Drop entity modifier matches enum`() {
        val entity = defaultSetTypes().first { it.type == SetType.Drop }
        assertEquals(SetType.Drop.ratingModifier, entity.modifier)
    }

    @Test
    fun `defaultSetTypes RestPause entity modifier matches enum`() {
        val entity = defaultSetTypes().first { it.type == SetType.RestPause }
        assertEquals(SetType.RestPause.ratingModifier, entity.modifier)
    }
}
