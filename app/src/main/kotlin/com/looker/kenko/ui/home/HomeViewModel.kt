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

package com.looker.kenko.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.looker.kenko.data.PlanDayResolver
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepo: PlanRepo,
    sessionRepo: SessionRepo,
    private val dayResolver: PlanDayResolver,
) : ViewModel() {

    private val planStream = planRepo.current

    private val sessionStream = sessionRepo.streamByDate(localDate)

    private val sessionsStream = sessionRepo.stream

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planItemStream = planRepo.current.flatMapLatest { plan ->
        planRepo.planItemsForDay(dayResolver.dayFor(localDate, plan?.id))
    }

    /**
     * Load of the running week for the card on the home screen.
     */
    val weekLoad = sessionsStream.map { sessions ->
        val period = StatsPeriod.of(StatsRange.Week, localDate)
        val summary = sessions.summarize(period)
        WeekLoad(
            volume = summary.volume,
            sets = summary.sets,
            heat = MuscleGroups.entries.associateWith { muscle ->
                val top = summary.muscles.maxOfOrNull { it.volume } ?: 0F
                val own = summary.muscles.firstOrNull { it.muscle == muscle }?.volume ?: 0F
                if (top <= 0F) 0F else (own / top).coerceIn(0F, 1F)
            },
        )
    }.asStateFlow(WeekLoad())

    val state = combine(
        planStream,
        sessionStream,
        sessionsStream,
        planItemStream,
    ) { currentPlan, currentSession, sessions, planItems ->
        val isFirstSession = sessions.size <= 1 && sessions.firstOrNull()?.date == localDate
        HomeUiData(
            isPlanSelected = currentPlan != null,
            isSessionStarted = currentSession != null && currentSession.sets.isNotEmpty(),
            isTodayEmpty = planItems.isEmpty(),
            isFirstSession = isFirstSession,
            currentPlanId = currentPlan?.id,
        )
    }.asStateFlow(
        HomeUiData(
            isPlanSelected = true,
            isSessionStarted = false,
            isTodayEmpty = false,
            isFirstSession = false,
            currentPlanId = null,
        ),
    )
}

/**
 * What the home card says about the running week: how much was moved and where it landed.
 */
@Immutable
data class WeekLoad(
    val volume: Float = 0F,
    val sets: Int = 0,
    val heat: Map<MuscleGroups, Float> = emptyMap(),
) {
    val isEmpty: Boolean get() = sets == 0
}

@Immutable
data class HomeUiData(
    val isPlanSelected: Boolean,
    val isSessionStarted: Boolean,
    val isTodayEmpty: Boolean,
    val isFirstSession: Boolean,
    val currentPlanId: Int?,
)
