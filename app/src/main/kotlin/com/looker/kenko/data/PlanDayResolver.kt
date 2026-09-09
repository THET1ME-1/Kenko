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

package com.looker.kenko.data

import com.looker.kenko.data.local.dao.SessionDao
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.toLocalEpochDays
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * Answers the one question the whole app asks: which day of the plan is it today.
 *
 * In week mode the answer is the day of the week. Otherwise days follow one another —
 * the lifter shows up whenever they can, and the plan simply moves on to the next day.
 */
@Singleton
class PlanDayResolver @Inject constructor(
    private val sessionDao: SessionDao,
    private val planRepo: PlanRepo,
    private val settingsRepo: SettingsRepo,
) {

    suspend fun dayFor(date: LocalDate, planId: Int?): Int {
        sessionDao.getDayIndex(date.toLocalEpochDays())?.let { return it }
        if (settingsRepo.stream.first().isWeekMode) {
            return date.dayOfWeek.isoDayNumber
        }
        val days = planId?.let { planRepo.dayCount(it) } ?: 0
        if (days <= 0) return FIRST_DAY
        val last = sessionDao.getLastDayIndexBefore(date.toLocalEpochDays()) ?: return FIRST_DAY
        return if (last >= days) FIRST_DAY else last + 1
    }

    private companion object {
        const val FIRST_DAY = 1
    }
}
