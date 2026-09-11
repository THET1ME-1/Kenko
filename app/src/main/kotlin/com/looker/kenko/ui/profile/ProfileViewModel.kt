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

package com.looker.kenko.ui.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.looker.kenko.data.model.Gym
import com.looker.kenko.data.model.Plan
import com.looker.kenko.data.model.PlanStat
import com.looker.kenko.data.model.ProfileSummary
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsRange
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.profileSummary
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.GymRepo
import com.looker.kenko.data.repository.PerformanceRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@HiltViewModel
class ProfileViewModel @Inject constructor(
    planRepo: PlanRepo,
    sessionRepo: SessionRepo,
    exerciseRepo: ExerciseRepo,
    performanceRepo: PerformanceRepo,
    gymRepo: GymRepo,
) : ViewModel() {

    private val today: LocalDate = localDate

    /**
     * The whole journal is read once and turned into two answers: the life-long numbers and the
     * load of the month behind the stats row.
     */
    private val journal: Flow<Journal> = sessionRepo.stream
        .map { sessions: List<Session> ->
            Journal(
                summary = sessions.profileSummary(today),
                month = sessions.summarize(StatsPeriod.of(StatsRange.Month, today)),
            )
        }
        .flowOn(Dispatchers.Default)

    private val library: Flow<Library> = combine(
        planRepo.current,
        exerciseRepo.numberOfExercise,
        performanceRepo.activity,
        gymRepo.current,
    ) { plan: Plan?, exercises: Int, activity: Map<Int, Int>, gym: Gym? ->
        Library(plan = plan, exercises = exercises, activity = activity, gym = gym)
    }

    val state: StateFlow<ProfileUiState> = combine(
        journal,
        library,
    ) { journal, library ->
        ProfileUiState(
            numberOfExercises = library.exercises,
            isPlanAvailable = library.plan != null,
            planName = library.plan?.name ?: "",
            planId = library.plan?.id ?: -1,
            planStat = library.plan?.stat,
            activity = library.activity,
            gym = library.gym,
            summary = journal.summary,
            month = journal.month,
            today = today,
        )
    }.asStateFlow(ProfileUiState(today = today))

    private data class Journal(
        val summary: ProfileSummary,
        val month: StatsSummary,
    )

    private data class Library(
        val plan: Plan?,
        val exercises: Int,
        val activity: Map<Int, Int>,
        val gym: Gym?,
    )
}

@Stable
data class ProfileUiState(
    val numberOfExercises: Int = 0,
    val isPlanAvailable: Boolean = false,
    val planId: Int = -1,
    val planName: String = "",
    val planStat: PlanStat? = null,
    val activity: Map<Int, Int> = emptyMap(),
    val gym: Gym? = null,
    val summary: ProfileSummary = ProfileSummary(),
    val month: StatsSummary? = null,
    val today: LocalDate = localDate,
)
