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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.looker.kenko.data.model.ExerciseLoad
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.MuscleLoad
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.setsOfExercise
import com.looker.kenko.data.model.setsOfMuscle
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * One session's worth of a single exercise or muscle: what was lifted on that day.
 */
@Immutable
data class SetRow(
    val date: LocalDate,
    val set: Set,
    val isAssisting: Boolean,
)

@Immutable
data class MuscleStatsUiState(
    val muscle: MuscleGroups,
    val load: MuscleLoad?,
    val exercises: List<ExerciseLoad>,
    val rows: List<SetRow>,
)

/**
 * Everything one muscle did inside the period: its own exercises, and the sets it merely helped in.
 */
@HiltViewModel(assistedFactory = MuscleStatsViewModel.Factory::class)
class MuscleStatsViewModel @AssistedInject constructor(
    sessionRepo: SessionRepo,
    @Assisted private val routeData: Routes.MuscleStats,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.MuscleStats): MuscleStatsViewModel
    }

    private val muscle: MuscleGroups = MuscleGroups.valueOf(routeData.muscle)

    val period: StatsPeriod = routeData.period()

    val state: StateFlow<MuscleStatsUiState> = sessionRepo.stream
        .map { sessions -> sessions.toState() }
        .asStateFlow(
            MuscleStatsUiState(
                muscle = muscle,
                load = null,
                exercises = emptyList(),
                rows = emptyList(),
            ),
        )

    private fun List<Session>.toState(): MuscleStatsUiState {
        val summary = summarize(period)
        return MuscleStatsUiState(
            muscle = muscle,
            load = summary.muscles.firstOrNull { it.muscle == muscle },
            exercises = summary.exerciseLoads.filter {
                it.exercise.target == muscle || muscle in it.exercise.secondaryTargets
            },
            rows = setsOfMuscle(muscle, period).map { (date, set) ->
                SetRow(
                    date = date,
                    set = set,
                    isAssisting = set.exercise.target != muscle,
                )
            },
        )
    }
}

@Immutable
data class ExerciseStatsUiState(
    val name: String,
    val load: ExerciseLoad?,
    val rows: List<SetRow>,
    val bestPerSession: List<Pair<LocalDate, Float>>,
)

/**
 * One exercise inside the period: its numbers, its sets and how the top weight moved.
 */
@HiltViewModel(assistedFactory = ExerciseStatsViewModel.Factory::class)
class ExerciseStatsViewModel @AssistedInject constructor(
    sessionRepo: SessionRepo,
    @Assisted private val routeData: Routes.ExerciseStats,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.ExerciseStats): ExerciseStatsViewModel
    }

    val period: StatsPeriod = routeData.period()

    private val name: String = routeData.name

    val state: StateFlow<ExerciseStatsUiState> = sessionRepo.stream
        .map { sessions -> sessions.toState() }
        .asStateFlow(
            ExerciseStatsUiState(
                name = name,
                load = null,
                rows = emptyList(),
                bestPerSession = emptyList(),
            ),
        )

    private fun List<Session>.toState(): ExerciseStatsUiState {
        val rows = setsOfExercise(name, period).map { (date, set) ->
            SetRow(date = date, set = set, isAssisting = false)
        }
        return ExerciseStatsUiState(
            name = name,
            load = summarize(period).exerciseLoads.firstOrNull { it.exercise.name == name },
            rows = rows,
            bestPerSession = rows
                .groupBy { it.date }
                .map { (date, sets) -> date to (sets.maxOfOrNull { it.set.weight } ?: 0F) }
                .sortedBy { it.first },
        )
    }
}
