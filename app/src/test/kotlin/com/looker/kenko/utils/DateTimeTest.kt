package com.looker.kenko.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class DateTimeTest {

    @Test
    fun `EpochDays plus combines values`() {
        val a = EpochDays(100)
        val b = EpochDays(50)
        assertEquals(150, (a + b).value)
    }

    @Test
    fun `EpochDays plus with zero returns same value`() {
        val a = EpochDays(42)
        assertEquals(42, (a + EpochDays(0)).value)
    }

    @Test
    fun `EpochDays plus two zeros`() {
        assertEquals(0, (EpochDays(0) + EpochDays(0)).value)
    }

    @Test
    fun `toLocalEpochDays round trip preserves date`() {
        val date = LocalDate(2025, 3, 14)
        val epochDays = date.toLocalEpochDays()
        assertEquals(date, LocalDate.fromEpochDays(epochDays.value))
    }

    @Test
    fun `toLocalEpochDays round trip for epoch start`() {
        val date = LocalDate(1970, 1, 1)
        val epochDays = date.toLocalEpochDays()
        assertEquals(date, LocalDate.fromEpochDays(epochDays.value))
    }

    @Test
    fun `DayOfWeek plus 1 from Monday is Tuesday`() {
        assertEquals(DayOfWeek.TUESDAY, DayOfWeek.MONDAY + 1)
    }

    @Test
    fun `DayOfWeek plus 7 wraps to same day`() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.MONDAY + 7)
    }

    @Test
    fun `DayOfWeek plus 6 from Monday is Sunday`() {
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.MONDAY + 6)
    }

    @Test
    fun `DayOfWeek plus 1 from Sunday wraps to Monday`() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.SUNDAY + 1)
    }

    @Test
    fun `DayOfWeek plus more than 7 wraps correctly`() {
        // Monday + 9 = Monday + 7 + 2 = Wednesday
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY + 9)
    }

    @Test
    fun `DayOfWeek plus 0 returns same day`() {
        assertEquals(DayOfWeek.THURSDAY, DayOfWeek.THURSDAY + 0)
    }

    @Test
    fun `DayOfWeek minus 1 from Monday is Sunday`() {
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.MONDAY - 1)
    }

    @Test
    fun `DayOfWeek minus 7 wraps to same day`() {
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.FRIDAY - 7)
    }

    @Test
    fun `DayOfWeek minus 0 returns same day`() {
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.WEDNESDAY - 0)
    }

    @Test
    fun `DayOfWeek minus 1 from Tuesday is Monday`() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.TUESDAY - 1)
    }

    @Test
    fun `DayOfWeek minus delegates to plus negative`() {
        val dayMinusTwo = DayOfWeek.FRIDAY - 2
        val dayPlusNegativeTwo = DayOfWeek.FRIDAY + (-2)
        assertEquals(dayPlusNegativeTwo, dayMinusTwo)
    }

    @Test
    fun `DayOfWeek plus full week cycle returns original`() {
        for (day in DayOfWeek.entries) {
            assertEquals(day, day + 7)
        }
    }

    @Test
    fun `DayOfWeek minus full week cycle returns original`() {
        for (day in DayOfWeek.entries) {
            assertEquals(day, day - 7)
        }
    }
}
