package com.looker.kenko.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PlanStatTest {

    @Test
    fun `exercises count is preserved`() {
        val stat = PlanStat(exercises = 5, workDays = 3)
        assertEquals(5, stat.exercises)
    }

    @Test
    fun `work days count is preserved`() {
        val stat = PlanStat(exercises = 5, workDays = 3)
        assertEquals(3, stat.workDays)
    }

    @Test
    fun `rest days is 7 minus work days`() {
        val stat = PlanStat(exercises = 5, workDays = 3)
        assertEquals(4, stat.restDays)
    }

    @Test
    fun `zero work days gives 7 rest days`() {
        val stat = PlanStat(exercises = 0, workDays = 0)
        assertEquals(7, stat.restDays)
    }

    @Test
    fun `7 work days gives 0 rest days`() {
        val stat = PlanStat(exercises = 21, workDays = 7)
        assertEquals(0, stat.restDays)
    }

    @Test
    fun `zero exercises and zero work days are preserved`() {
        val stat = PlanStat(exercises = 0, workDays = 0)
        assertEquals(0, stat.exercises)
        assertEquals(0, stat.workDays)
    }

    @Test
    fun `large exercise count is preserved`() {
        val stat = PlanStat(exercises = 1000, workDays = 5)
        assertEquals(1000, stat.exercises)
        assertEquals(5, stat.workDays)
        assertEquals(2, stat.restDays)
    }

    @Test
    fun `work days 1 gives 6 rest days`() {
        val stat = PlanStat(exercises = 3, workDays = 1)
        assertEquals(6, stat.restDays)
    }
}
