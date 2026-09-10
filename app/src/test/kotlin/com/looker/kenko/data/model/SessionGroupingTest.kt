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
import org.junit.Assert.assertEquals
import org.junit.Test

private val press = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)

private fun workSet() = Set(
    repsOrDuration = 8,
    weight = 80F,
    type = SetType.Standard,
    exercise = press,
    rir = RepsInReserve(2),
)

private fun session(date: LocalDate, dayIndex: Int? = null) =
    Session(date = date, sets = listOf(workSet()), planId = 1, dayIndex = dayIndex)

class SessionGroupingTest {

    private val sessions = listOf(
        session(LocalDate(2026, 9, 8), dayIndex = 1),
        session(LocalDate(2026, 9, 1), dayIndex = 2),
        session(LocalDate(2026, 8, 27), dayIndex = 1),
        session(LocalDate(2025, 12, 30)),
    )

    @Test
    fun `months run from the newest, each holding its own sessions`() {
        val groups = sessions.groupSessions(SessionGrouping.Month)

        assertEquals(
            listOf(
                SessionGroupKey.Month(2026, 9),
                SessionGroupKey.Month(2026, 8),
                SessionGroupKey.Month(2025, 12),
            ),
            groups.map { it.key },
        )
        assertEquals(2, groups.first().sessions.size)
    }

    @Test
    fun `years collapse the same sessions into two groups`() {
        val groups = sessions.groupSessions(SessionGrouping.Year)

        assertEquals(listOf(SessionGroupKey.Year(2026), SessionGroupKey.Year(2025)), groups.map { it.key })
        assertEquals(3, groups.first().sessions.size)
    }

    @Test
    fun `days of the plan run by number, sessions without a day last`() {
        val groups = sessions.groupSessions(SessionGrouping.PlanDay)

        assertEquals(
            listOf(
                SessionGroupKey.PlanDay(1),
                SessionGroupKey.PlanDay(2),
                SessionGroupKey.PlanDay(null),
            ),
            groups.map { it.key },
        )
        assertEquals(2, groups.first().sessions.size)
    }

    @Test
    fun `plain list keeps one group with everything in it`() {
        val groups = sessions.groupSessions(SessionGrouping.None)

        assertEquals(listOf(SessionGroupKey.All), groups.map { it.key })
        assertEquals(4, groups.single().sessions.size)
    }

    @Test
    fun `sessions inside a group run from the newest`() {
        val groups = sessions.groupSessions(SessionGrouping.Month)

        assertEquals(
            listOf(LocalDate(2026, 9, 8), LocalDate(2026, 9, 1)),
            groups.first().sessions.map { it.date },
        )
    }
}
