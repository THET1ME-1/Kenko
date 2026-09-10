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
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.LastTime
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.Plan
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.data.model.lastTimeOf
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.model.weekStreak
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val planRepo: PlanRepo,
    sessionRepo: SessionRepo,
    private val dayResolver: PlanDayResolver,
) : ViewModel() {

    private val sessionStream = sessionRepo.streamByDate(localDate)

    private val sessionsStream = sessionRepo.stream

    /**
     * The plan of the day and which of its days is due, resolved once for the whole screen.
     */
    private val todayStream = planRepo.current.map { plan ->
        plan to dayResolver.dayFor(localDate, plan?.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planItemStream = todayStream.flatMapLatest { (_, day) ->
        planRepo.planItemsForDay(day)
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
            streak = sessions.weekStreak(localDate),
            heat = MuscleGroups.entries.associateWith { muscle ->
                val top = summary.muscles.maxOfOrNull { it.volume } ?: 0F
                val own = summary.muscles.firstOrNull { it.muscle == muscle }?.volume ?: 0F
                if (top <= 0F) 0F else (own / top).coerceIn(0F, 1F)
            },
        )
    }.asStateFlow(WeekLoad())

    val state = combine(
        todayStream,
        sessionStream,
        sessionsStream,
        planItemStream,
    ) { (currentPlan, day), currentSession, sessions, planItems ->
        HomeUiData(
            isPlanSelected = currentPlan != null,
            isSessionStarted = currentSession != null && currentSession.sets.isNotEmpty(),
            isTodayEmpty = planItems.isEmpty(),
            currentPlanId = currentPlan?.id,
            planName = currentPlan?.name,
            dayIndex = day,
            todayExercises = planItems.map { it.exercise },
            todaySets = planItems.sumOf { it.targetSets },
            lastTime = sessions.lastTimeOf(
                dayIndex = day.takeIf { currentPlan != null },
                before = localDate,
            ),
        )
    }.asStateFlow(HomeUiData())

    private val _pickerPlanId: MutableStateFlow<Int?> = MutableStateFlow(null)

    private val _isPickerVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPickerVisible: StateFlow<Boolean> = _isPickerVisible

    /**
     * Plans and their days for the sheet that opens before a session starts.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val picker: StateFlow<PlanPicker> = combine(
        planRepo.plans,
        planRepo.current,
        _pickerPlanId,
    ) { plans, current, picked ->
        plans to (picked ?: current?.id ?: plans.firstOrNull()?.id)
    }.flatMapLatest { (plans, shownId) ->
        if (shownId == null) {
            flowOf(PlanPicker(plans = plans))
        } else {
            planRepo.planItems(shownId).map { items ->
                PlanPicker(
                    plans = plans,
                    shownPlanId = shownId,
                    days = items
                        .groupBy { it.dayIndex }
                        .toSortedMap()
                        .map { (index, dayItems) ->
                            PlanDayOption(index = index, exercises = dayItems.size)
                        },
                )
            }
        }
    }.asStateFlow(PlanPicker())

    fun showPicker() {
        _pickerPlanId.value = null
        _isPickerVisible.value = true
    }

    fun hidePicker() {
        _isPickerVisible.value = false
    }

    fun showDaysOf(planId: Int) {
        _pickerPlanId.value = planId
    }
}

/**
 * What the home card says about the running week: how much was moved and where it landed.
 */
@Immutable
data class WeekLoad(
    val volume: Float = 0F,
    val sets: Int = 0,
    val streak: Int = 0,
    val heat: Map<MuscleGroups, Float> = emptyMap(),
) {
    val isEmpty: Boolean get() = sets == 0
}

/**
 * One day of a plan as the picker shows it: its number and how much is written into it.
 */
@Immutable
data class PlanDayOption(
    val index: Int,
    val exercises: Int,
)

@Immutable
data class PlanPicker(
    val plans: List<Plan> = emptyList(),
    val shownPlanId: Int? = null,
    val days: List<PlanDayOption> = emptyList(),
)

@Immutable
data class HomeUiData(
    val isPlanSelected: Boolean = true,
    val isSessionStarted: Boolean = false,
    val isTodayEmpty: Boolean = false,
    val currentPlanId: Int? = null,
    val planName: String? = null,
    val dayIndex: Int = 1,
    val todayExercises: List<Exercise> = emptyList(),
    val todaySets: Int = 0,
    val lastTime: LastTime? = null,
)
