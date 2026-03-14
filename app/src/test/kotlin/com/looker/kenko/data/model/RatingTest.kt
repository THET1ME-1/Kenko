package com.looker.kenko.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingTest {

    @Test
    fun `rating holds its value`() {
        assertEquals(42.5f, Rating(42.5f).value)
    }

    @Test
    fun `rating zero holds zero`() {
        assertEquals(0f, Rating(0f).value)
    }

    @Test
    fun `plus adds two rating values`() {
        val a = Rating(10.0f)
        val b = Rating(5.5f)
        assertEquals(15.5f, (a + b).value)
    }

    @Test
    fun `plus with zero returns same value`() {
        val rating = Rating(7.3f)
        assertEquals(7.3f, (rating + Rating(0f)).value)
    }

    @Test
    fun `plus of two zeros returns zero`() {
        assertEquals(0f, (Rating(0f) + Rating(0f)).value)
    }

    @Test
    fun `plus is commutative`() {
        val a = Rating(3.1f)
        val b = Rating(2.9f)
        assertEquals((a + b).value, (b + a).value)
    }

    @Test
    fun `plus with negative value decreases result`() {
        val a = Rating(10f)
        val b = Rating(-3f)
        assertEquals(7f, (a + b).value)
    }
}
