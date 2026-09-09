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

package com.looker.kenko.ui.sessionDetail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.PlanDayResolver
import com.looker.kenko.data.local.model.DEFAULT_REST_SECONDS
import com.looker.kenko.data.model.DEFAULT_DROP_PERCENT
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.RestTimer
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Ghost
import com.looker.kenko.data.model.Record
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.beatsRecord
import com.looker.kenko.data.model.SessionBlock
import com.looker.kenko.data.model.ghostOf
import com.looker.kenko.data.model.SetChain
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.toSessionBlocks
import com.looker.kenko.data.model.week
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.addSet.AddSetTarget
import com.looker.kenko.ui.addSet.dropTargetOf
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import com.looker.kenko.utils.isToday
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@HiltViewModel(assistedFactory = SessionDetailViewModel.Factory::class)
class SessionDetailViewModel @AssistedInject constructor(
    private val repo: SessionRepo,
    private val planRepo: PlanRepo,
    @Assisted private val routeData: Routes.SessionDetail,
    private val uriHandler: UriHandler,
    private val dayResolver: PlanDayResolver,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.SessionDetail): SessionDetailViewModel
    }

    private val epochDays: Int? = routeData.epochDays.takeIf { it != -1 }

    private val sessionDate: LocalDate = epochDays?.let {
        LocalDate.fromEpochDays(it)
    } ?: localDate

    val previousSessionDate = sessionDate - week

    private val previousSessionExists: Flow<Boolean> = repo.streamByDate(previousSessionDate)
        .map { it != null }

    private val sessionStream: Flow<Session?> = repo.streamByDate(sessionDate)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val plannedToday: Flow<List<PlanItem>> = combine(
        sessionStream,
        planRepo.current,
    ) { session, activePlan -> session to activePlan }
        .flatMapLatest { (session, activePlan) ->
            val planId = session?.planId ?: activePlan?.id
            val day = session?.dayIndex ?: dayResolver.dayFor(sessionDate, planId)
            currentDayIndex = day
            when {
                planId != null -> planRepo.planItems(planId, day)
                sessionDate.isToday -> planRepo.planItemsForDay(day)
                else -> flowOf(emptyList())
            }
        }

    /**
     * Day of the plan this screen is writing into.
     */
    private var currentDayIndex: Int = 1

    private val _sheetTarget: MutableStateFlow<SetSheetTarget?> = MutableStateFlow(null)
    val sheetTarget: StateFlow<SetSheetTarget?> = _sheetTarget

    private val _exercisePickerVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val exercisePickerVisible: StateFlow<Boolean> = _exercisePickerVisible

    private val _restTimer: MutableStateFlow<RestTimer?> = MutableStateFlow(null)

    /**
     * Ticks while the lifter rests, so the screen can count down without owning a clock.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val rest: StateFlow<RestUiState?> = _restTimer.flatMapLatest { timer ->
        if (timer == null) {
            flowOf<RestUiState?>(null)
        } else {
            flow<RestUiState?> {
                while (true) {
                    val now = Clock.System.now()
                    emit(
                        RestUiState(
                            exerciseName = timer.exerciseName,
                            secondsLeft = timer.secondsLeft(now),
                            totalSeconds = timer.totalSeconds,
                        ),
                    )
                    if (timer.isDone(now)) break
                    delay(TICK)
                }
            }
        }
    }.asStateFlow(null)

    private var plannedItems: List<PlanItem> = emptyList()

    private fun restSecondsFor(exercise: Exercise?): Int =
        plannedItems.firstOrNull { it.exercise.id == exercise?.id }?.restSeconds
            ?: DEFAULT_REST_SECONDS

    private suspend fun startRest(exerciseName: String, seconds: Int) {
        if (seconds <= 0) return
        _restTimer.emit(
            RestTimer(
                exerciseName = exerciseName,
                totalSeconds = seconds,
                endsAt = Clock.System.now() + seconds.seconds,
            ),
        )
    }

    fun shiftRest(bySeconds: Int) {
        val timer = _restTimer.value ?: return
        viewModelScope.launch {
            _restTimer.emit(timer.shifted(bySeconds, Clock.System.now()))
        }
    }

    fun stopRest() {
        viewModelScope.launch {
            _restTimer.emit(null)
        }
    }

    /**
     * Starts the rest as soon as a new set lands in today's session, for as long as the plan says.
     */
    private fun watchSetsForRest() {
        viewModelScope.launch {
            var knownCount: Int? = null
            combine(sessionStream, plannedToday) { session, planned -> session to planned }
                .collect { (session, planned) ->
                    plannedItems = planned
                    val sets = session?.sets.orEmpty()
                    val previous = knownCount
                    knownCount = sets.size
                    if (previous == null || sets.size <= previous || !sessionDate.isToday) {
                        return@collect
                    }
                    val last = sets.maxByOrNull { it.id ?: 0 } ?: return@collect
                    announceRecord(last)
                    // Внутри группы и круга отдыха нет: таймер стартует, когда группа закрыта.
                    if (last.parentSetId != null || last.supersetId != null) return@collect
                    if (last.dropCount > 0) return@collect
                    startRest(last.exercise.name, restSecondsFor(last.exercise))
                }
        }
    }

    private val _record: MutableSharedFlow<Record> = MutableSharedFlow()

    /**
     * Fires the moment a set beats everything that exercise had before.
     */
    val record: SharedFlow<Record> = _record

    /**
     * Compares the set against the journal as it stood without it.
     */
    private suspend fun announceRecord(set: Set) {
        if (set.parentSetId != null) return
        val history = repo.stream.first().map { session ->
            session.copy(sets = session.sets.filter { it.id != set.id })
        }
        history.beatsRecord(set, sessionDate)?.let { _record.emit(it) }
    }

    init {
        watchSetsForRest()
    }

    val state: StateFlow<SessionDetailState> =
        combine(
            combine(sessionStream, repo.stream) { session, all -> session to all },
            plannedToday,
            previousSessionExists,
            planRepo.current,
        ) { (session, allSessions), planned, previousSession, activePlan ->
            if (session == null && epochDays != null) {
                return@combine SessionDetailState.Error.InvalidSession
            }

            val currentSession = session ?: Session(-1, emptyList())

            val blocks = currentSession.sets.toSessionBlocks(plannedItems = planned)

            val ghosts = blocks
                .flatMap { it.exercises }
                .distinctBy { it.name }
                .mapNotNull { exercise ->
                    allSessions.ghostOf(exercise.name, before = currentSession.date)
                        ?.let { exercise.name to it }
                }
                .toMap()

            val planId = session?.planId ?: if (sessionDate.isToday) activePlan?.id else null

            SessionDetailState.Success(
                SessionUiData(
                    date = currentSession.date,
                    dayIndex = session?.dayIndex ?: currentDayIndex,
                    blocks = blocks,
                    isToday = currentSession.date.isToday,
                    planId = planId,
                    hasPreviousSession = previousSession,
                    ghosts = ghosts,
                ),
            )
        }.onStart { emit(SessionDetailState.Loading) }
            .asStateFlow(SessionDetailState.Loading)

    fun removeSet(setId: Int?) {
        if (setId == null) return
        viewModelScope.launch {
            repo.removeSet(setId)
        }
    }

    fun showAddSetSheet(exercise: Exercise, supersetId: Int? = null) {
        val exerciseId = exercise.id ?: return
        val plan = plannedItems.firstOrNull { it.exercise.id == exerciseId }
        viewModelScope.launch {
            _sheetTarget.emit(
                SetSheetTarget(
                    exerciseName = exercise.name,
                    setNumber = performedSets(exerciseId) + 1,
                    target = AddSetTarget(
                        exerciseId = exerciseId,
                        supersetId = supersetId,
                        dropCount = plan?.dropCount ?: 0,
                        dropPercent = plan?.dropPercent ?: DEFAULT_DROP_PERCENT,
                        planWeight = plan?.targetWeight ?: 0F,
                        planReps = plan?.targetReps ?: 0,
                        gripId = plan?.gripId,
                    ),
                ),
            )
        }
    }

    /**
     * How many sets of this exercise are already written down — the sheet shows the next number.
     */
    private fun performedSets(exerciseId: Int): Int {
        val blocks = (state.value as? SessionDetailState.Success)?.data?.blocks.orEmpty()
        return blocks.filterIsInstance<SessionBlock.SingleExercise>()
            .firstOrNull { it.exercise.id == exerciseId }
            ?.chains
            ?.size
            ?: blocks.filterIsInstance<SessionBlock.Superset>()
                .sumOf { block ->
                    block.rounds.count { round ->
                        round.chains.any { it.set.exercise.id == exerciseId }
                    }
                }
    }

    fun showAddDropSheet(chain: SetChain) {
        showDropStepSheet(chain, chain.performedSteps)
    }

    /**
     * Opens the sheet on one cut of the group: the computed weight is only a suggestion,
     * the lifter can put in whatever the rack actually has.
     */
    fun showDropStepSheet(chain: SetChain, stepIndex: Int) {
        val parentId = chain.set.id ?: return
        val exerciseId = chain.set.exercise.id ?: return
        val step = chain.steps.getOrNull(stepIndex) ?: return
        viewModelScope.launch {
            _sheetTarget.emit(
                SetSheetTarget(
                    exerciseName = chain.set.exercise.name,
                    target = AddSetTarget(
                        exerciseId = exerciseId,
                        parentSetId = parentId,
                        dropIndex = stepIndex,
                        suggestion = AddSetTarget.Suggestion(
                            reps = step.reps,
                            weight = step.weight,
                        ),
                    ),
                ),
            )
        }
    }

    fun setDropCount(chain: SetChain, count: Int) {
        val id = chain.set.id ?: return
        viewModelScope.launch {
            repo.setDropSettings(id, count, chain.set.dropPercent)
        }
    }

    fun setDropPercent(chain: SetChain, percent: Int) {
        val id = chain.set.id ?: return
        viewModelScope.launch {
            repo.setDropSettings(id, chain.set.dropCount, percent)
        }
    }

    fun markDropStep(chain: SetChain, stepIndex: Int) {
        val id = chain.set.id ?: return
        if (stepIndex == 0) return
        viewModelScope.launch {
            repo.markDropStep(id, stepIndex)
        }
    }

    fun markDropGroup(chain: SetChain) {
        val id = chain.set.id ?: return
        viewModelScope.launch {
            repo.markWholeDropGroup(id)
            startRest(chain.set.exercise.name, restSecondsFor(chain.set.exercise))
        }
    }

    fun undoDrops(chain: SetChain) {
        val id = chain.set.id ?: return
        viewModelScope.launch {
            repo.clearDrops(id)
        }
    }

    fun closeSupersetRound(block: SessionBlock.Superset) {
        viewModelScope.launch {
            val sessionId = repo.getSessionIdOrCreate(sessionDate)
            repo.closeSupersetRound(sessionId, block.id, block.plan)
            startRest(
                exerciseName = block.exercises.firstOrNull()?.name.orEmpty(),
                seconds = block.plan.maxOfOrNull { it.restSeconds } ?: DEFAULT_REST_SECONDS,
            )
        }
    }

    fun undoSupersetRound(block: SessionBlock.Superset) {
        viewModelScope.launch {
            val sessionId = repo.getSessionIdOrCreate(sessionDate)
            repo.clearLastSupersetRound(sessionId, block.id)
        }
    }

    fun showExercisePicker() {
        viewModelScope.launch {
            _exercisePickerVisible.emit(true)
        }
    }

    fun hideExercisePicker() {
        viewModelScope.launch {
            _exercisePickerVisible.emit(false)
        }
    }

    fun hideSheet() {
        viewModelScope.launch {
            _sheetTarget.emit(null)
        }
    }

    fun openReference(reference: String) {
        viewModelScope.launch {
            try {
                uriHandler.openUri(reference)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}

private const val TICK = 250L

@Immutable
data class RestUiState(
    val exerciseName: String,
    val secondsLeft: Int,
    val totalSeconds: Int,
) {
    val isDone: Boolean get() = secondsLeft == 0

    val progress: Float
        get() = if (totalSeconds == 0) 0F else secondsLeft.toFloat() / totalSeconds
}

@Immutable
data class SetSheetTarget(
    val exerciseName: String,
    val target: AddSetTarget,
    val setNumber: Int = 1,
)

@Stable
data class SessionUiData(
    val date: LocalDate,
    val dayIndex: Int?,
    val blocks: List<SessionBlock>,
    val isToday: Boolean = false,
    val planId: Int? = null,
    val hasPreviousSession: Boolean = false,
    /**
     * The same exercises as they went last time, keyed by exercise name.
     */
    val ghosts: Map<String, Ghost> = emptyMap(),
) {
    /**
     * Kilograms this session is ahead of the last time the same exercises were done.
     *
     * Exercises done for the first time have nothing to compare with and stay out of the count.
     */
    val ghostDelta: Float
        get() = blocks.sumOf { block ->
            block.exercises.sumOf { exercise ->
                val ghost = ghosts[exercise.name] ?: return@sumOf 0.0
                (todayVolume(exercise.name) - ghost.volume).toDouble()
            }
        }.toFloat()

    val hasGhost: Boolean get() = blocks.any { block ->
        block.exercises.any { ghosts.containsKey(it.name) }
    }

    private fun todayVolume(exerciseName: String): Float = blocks
        .flatMap { block ->
            when (block) {
                is SessionBlock.SingleExercise -> block.chains
                is SessionBlock.Superset -> block.rounds.flatMap { it.chains }
            }
        }
        .filter { it.set.exercise.name == exerciseName }
        .sumOf { it.volume.toDouble() }
        .toFloat()
}

sealed interface SessionDetailState {

    data object Loading : SessionDetailState

    data class Success(val data: SessionUiData) : SessionDetailState

    sealed class Error(
        @param:StringRes val title: Int,
        @param:StringRes val errorMessage: Int,
    ) : SessionDetailState {
        data object InvalidSession : Error(
            title = R.string.label_missed_day,
            errorMessage = R.string.error_cant_find_session,
        )

        data object EmptyPlan : Error(
            title = R.string.label_nothing_today,
            errorMessage = R.string.label_no_exercise_today,
        )
    }
}
