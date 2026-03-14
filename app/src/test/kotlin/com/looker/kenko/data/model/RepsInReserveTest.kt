package com.looker.kenko.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RepsInReserveTest {

    @Test
    fun `rir value 0 has modifier 1_20`() {
        assertEquals(1.20f, RepsInReserve(0).modifier)
    }

    @Test
    fun `rir negative value has modifier 1_20`() {
        assertEquals(1.20f, RepsInReserve(-1).modifier)
    }

    @Test
    fun `rir very negative value has modifier 1_20`() {
        assertEquals(1.20f, RepsInReserve(-100).modifier)
    }

    @Test
    fun `rir value 1 has modifier 1_12`() {
        assertEquals(1.12f, RepsInReserve(1).modifier)
    }

    @Test
    fun `rir value 2 has modifier 1_04`() {
        assertEquals(1.04f, RepsInReserve(2).modifier)
    }

    @Test
    fun `rir value 3 has modifier 0_96`() {
        assertEquals(0.96f, RepsInReserve(3).modifier)
    }

    @Test
    fun `rir value 4 has modifier 0_88`() {
        assertEquals(0.88f, RepsInReserve(4).modifier)
    }

    @Test
    fun `rir value 5 has modifier 0_80`() {
        assertEquals(0.80f, RepsInReserve(5).modifier)
    }

    @Test
    fun `rir large value has modifier 0_80`() {
        assertEquals(0.80f, RepsInReserve(100).modifier)
    }

    @Test
    fun `fromRPE 10 gives rir 0 with modifier 1_20`() {
        val rir = RepsInReserve.fromRPE(10)
        assertEquals(0, rir.value)
        assertEquals(1.20f, rir.modifier)
    }

    @Test
    fun `fromRPE 9 gives rir 1 with modifier 1_12`() {
        val rir = RepsInReserve.fromRPE(9)
        assertEquals(1, rir.value)
        assertEquals(1.12f, rir.modifier)
    }

    @Test
    fun `fromRPE 8 gives rir 2 with modifier 1_04`() {
        val rir = RepsInReserve.fromRPE(8)
        assertEquals(2, rir.value)
        assertEquals(1.04f, rir.modifier)
    }

    @Test
    fun `fromRPE 7 gives rir 3 with modifier 0_96`() {
        val rir = RepsInReserve.fromRPE(7)
        assertEquals(3, rir.value)
        assertEquals(0.96f, rir.modifier)
    }

    @Test
    fun `fromRPE 6 gives rir 4 with modifier 0_88`() {
        val rir = RepsInReserve.fromRPE(6)
        assertEquals(4, rir.value)
        assertEquals(0.88f, rir.modifier)
    }

    @Test
    fun `fromRPE 5 gives rir 5 with modifier 0_80`() {
        val rir = RepsInReserve.fromRPE(5)
        assertEquals(5, rir.value)
        assertEquals(0.80f, rir.modifier)
    }

    @Test
    fun `fromRPE 0 gives rir 10 with modifier 0_80`() {
        val rir = RepsInReserve.fromRPE(0)
        assertEquals(10, rir.value)
        assertEquals(0.80f, rir.modifier)
    }

    @Test
    fun `fromRPE above 10 gives negative rir with modifier 1_20`() {
        val rir = RepsInReserve.fromRPE(11)
        assertEquals(-1, rir.value)
        assertEquals(1.20f, rir.modifier)
    }
}
