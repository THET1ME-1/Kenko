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

package com.looker.kenko.ui.stats

import androidx.lifecycle.ViewModel
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Load of one period: the body map, the numbers, the muscles and the exercises behind them.
 *
 * The whole journal is read once and counted in memory — a lifter's log is small, and the
 * period changes far more often than the sessions do.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    sessionRepo: SessionRepo,
) : ViewModel() {

    private val _period: MutableStateFlow<StatsPeriod> =
        MutableStateFlow(StatsPeriod.of(StatsRange.Week, localDate))

    val period: StateFlow<StatsPeriod> = _period

    val state: StateFlow<StatsSummary> = combine(
        sessionRepo.stream,
        _period,
    ) { sessions, period ->
        sessions.summarize(period)
    }.asStateFlow(emptyList<Session>().summarize(_period.value))

    /**
     * A period that ends in the future has nothing to show yet.
     */
    val canGoForward: StateFlow<Boolean> = _period
        .map { it.to < localDate }
        .asStateFlow(false)

    fun setRange(range: StatsRange) {
        if (range == StatsRange.Custom) return
        _period.value = StatsPeriod.of(range, localDate)
    }

    fun setCustom(from: LocalDate, to: LocalDate) {
        _period.value = StatsPeriod.custom(from, to)
    }

    fun shift(by: Int) {
        _period.value = _period.value.shifted(by)
    }
}
