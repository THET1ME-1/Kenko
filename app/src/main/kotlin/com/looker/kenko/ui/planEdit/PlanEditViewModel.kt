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

package com.looker.kenko.ui.planEdit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.GripRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.ui.planEdit.PlanTargets
import com.looker.kenko.utils.asStateFlow
import com.looker.kenko.utils.nextLocalDateTime
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

@HiltViewModel(assistedFactory = PlanEditViewModel.Factory::class)
class PlanEditViewModel @AssistedInject constructor(
    private val repo: PlanRepo,
    private val stringHandler: StringHandler,
    private val sessionRepo: com.looker.kenko.data.repository.SessionRepo,
    private val settingsRepo: SettingsRepo,
    private val gripRepo: GripRepo,
    @Assisted private val routeData: Routes.PlanEdit,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.PlanEdit): PlanEditViewModel
    }

    private val _planId: Int = routeData.id

    // if null show name edit else plan edit
    private val planIdStream = MutableStateFlow(_planId)

    val planNameState: TextFieldState = TextFieldState("")

    val snackbarState = SnackbarHostState()

    private val _isBackAlreadyPressedOnce = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _planItemsStream = planIdStream.flatMapLatest { repo.planItems(it) }

    private val _dayIndex: MutableStateFlow<Int> = MutableStateFlow(1)

    private val _isSheetVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)


    private val _editedItem: MutableStateFlow<PlanItem?> = MutableStateFlow(null)
    val editedItem: StateFlow<PlanItem?> = _editedItem

    private val _replacedItem: MutableStateFlow<PlanItem?> = MutableStateFlow(null)
    val replacedItem: StateFlow<PlanItem?> = _replacedItem

    /**
     * Handles of the exercise being set up, so the plan can ask for a particular one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val gripsOfEdited: StateFlow<List<Grip>> = _editedItem
        .flatMapLatest { item ->
            val exerciseId = item?.exercise?.id
            if (exerciseId == null) flowOf(emptyList()) else gripRepo.grips(exerciseId)
        }
        .asStateFlow(emptyList())

    @OptIn(FlowPreview::class)
    val isNameAlreadyUsed = snapshotFlow { planNameState.text.trim().toString() }
        .debounce(200.milliseconds)
        .map { repo.planNameExists(it) }
        .asStateFlow(false)

    val pageState: StateFlow<PlanEditStage> = planIdStream.map { id ->
        if (id == -1) PlanEditStage.NameEdit else PlanEditStage.PlanEdit
    }.asStateFlow(PlanEditStage.NameEdit)

    val state: StateFlow<PlanEditState> = combine(
        _planItemsStream,
        _dayIndex,
        _isSheetVisible,
        settingsRepo.get { isWeekMode },
    ) { items, day, sheetVisible, weekMode ->
        PlanEditState(
            currentDay = day,
            dayCount = items.maxOfOrNull { it.dayIndex } ?: 0,
            isWeekMode = weekMode,
            exerciseSheetVisible = sheetVisible,
            items = items.filter { it.dayIndex == day },
        )
    }.asStateFlow(
        PlanEditState(
            currentDay = 1,
            dayCount = 0,
            isWeekMode = false,
            exerciseSheetVisible = false,
            items = emptyList(),
        ),
    )

    /**
     * Ties an exercise with the one standing right after it. Two taps instead of a mode:
     * the superset is built where the lifter is already looking.
     */
    fun tieWithNext(item: PlanItem) {
        viewModelScope.launch {
            val dayItems = repo.getPlanItems(planIdStream.value, _dayIndex.value)
                .sortedBy { it.order }
            val index = dayItems.indexOfFirst { it.id == item.id }
            val next = dayItems.getOrNull(index + 1)
            val ids = listOfNotNull(item.id, next?.id)
            if (ids.size < 2) {
                snackbarState.showSnackbar(
                    stringHandler.getString(R.string.error_superset_needs_two),
                )
                return@launch
            }
            repo.setSuperset(ids, repo.nextSupersetId(planIdStream.value))
        }
    }

    fun breakSuperset(supersetId: Int) {
        viewModelScope.launch {
            val ids = repo.getPlanItems(planIdStream.value, _dayIndex.value)
                .filter { it.supersetId == supersetId }
                .mapNotNull { it.id }
            repo.setSuperset(ids, null)
        }
    }

    /**
     * Puts another exercise in the same slot, keeping sets, reps and rest.
     */
    fun replaceExercise(item: PlanItem, exercise: Exercise) {
        viewModelScope.launch {
            repo.updateItem(item.copy(exercise = exercise))
            _replacedItem.emit(null)
        }
    }

    fun startReplacing(item: PlanItem?) {
        viewModelScope.launch {
            _replacedItem.emit(item)
        }
    }

    /**
     * Moves an exercise up or down the day by swapping its place with the neighbour.
     */
    fun moveItem(item: PlanItem, delta: Int) {
        viewModelScope.launch {
            val dayItems = repo.getPlanItems(planIdStream.value, _dayIndex.value)
                .sortedBy { it.order }
            val index = dayItems.indexOfFirst { it.id == item.id }
            val target = index + delta
            if (index < 0 || target !in dayItems.indices) return@launch
            val neighbour = dayItems[target]
            repo.updateItem(item.copy(order = neighbour.order))
            repo.updateItem(neighbour.copy(order = item.order))
        }
    }

    /**
     * Writes down the order the day was dragged into: the list already shows it, the plan
     * catches up once the finger is off.
     */
    fun reorderDay(itemIds: List<Long>) {
        viewModelScope.launch {
            val dayItems = repo.getPlanItems(planIdStream.value, _dayIndex.value)
                .associateBy { it.id }
            itemIds.forEachIndexed { index, id ->
                val item = dayItems[id] ?: return@forEachIndexed
                if (item.order != index) {
                    repo.updateItem(item.copy(order = index))
                }
            }
        }
    }

    fun editTargets(item: PlanItem?) {
        viewModelScope.launch {
            _editedItem.emit(item)
        }
    }

    fun saveTargets(item: PlanItem, targets: PlanTargets) {
        viewModelScope.launch {
            repo.updateItem(
                item.copy(
                    targetSets = targets.sets,
                    targetReps = targets.repsMin,
                    targetRepsMax = targets.repsMax,
                    restSeconds = targets.restSeconds,
                    dropCount = targets.dropCount,
                    dropPercent = targets.dropPercent,
                    barWeight = targets.barWeight,
                    leftWeight = targets.leftWeight,
                    rightWeight = targets.rightWeight,
                    gripId = targets.gripId,
                ),
            )
            _editedItem.emit(null)
        }
    }

    fun saveName() {
        viewModelScope.launch {
            if (planNameState.text.isBlank()) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_plan_name_empty))
                return@launch
            }
            if (isNameAlreadyUsed.value) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_plan_name_exists))
                return@launch
            }
            val createId = repo.createPlan(planNameState.text.toString())
            // Только что собранная программа сразу становится текущей: иначе она лежит мёртвым грузом.
            repo.setCurrent(createId)
            planIdStream.emit(createId)
        }
    }

    private val _folds: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())

    /**
     * What the lifter folded by hand in this day. Kept in the model, so a trip to the goals sheet
     * or to the exercise picker does not unfold the whole day again.
     */
    val folds: StateFlow<Map<String, Boolean>> = _folds

    fun setFolded(key: String, folded: Boolean) {
        viewModelScope.launch {
            _folds.emit(_folds.value + (key to folded))
        }
    }

    fun setCurrentDay(day: Int) {
        viewModelScope.launch {
            _dayIndex.emit(day.coerceAtLeast(1))
        }
    }

    /**
     * Writes today's exercises into another day, targets and supersets included.
     */
    fun copyCurrentDayTo(day: Int) {
        val planId = planIdStream.value ?: return
        viewModelScope.launch {
            repo.copyDay(planId = planId, fromDay = _dayIndex.value, toDay = day)
            setCurrentDay(day)
        }
    }

    fun openSheet() {
        viewModelScope.launch {
            _isSheetVisible.emit(true)
        }
    }

    fun closeSheet() {
        viewModelScope.launch {
            _isSheetVisible.emit(false)
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            repo.addItem(
                PlanItem(
                    dayIndex = _dayIndex.value,
                    exercise = exercise,
                    planId = planIdStream.value,
                ),
            )
        }
    }

    fun removeItem(item: PlanItem) {
        val id = item.id ?: return
        viewModelScope.launch {
            repo.removeItem(id)
        }
    }

    fun onBackPress(stage: PlanEditStage, onBackPress: () -> Unit) {
        viewModelScope.launch {
            if (stage == PlanEditStage.NameEdit) {
                onBackPress()
                return@launch
            }
            // Пустая программа не держит человека на экране: уходим и убираем её за собой.
            if (repo.getPlanItems(planIdStream.value).isEmpty()) {
                repo.deletePlan(planIdStream.value)
            }
            onBackPress()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun debugFillMockData(sessions: Int = 9) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val rand = Random.Default
            val added = AtomicInt(0)
            val planId = planIdStream.value
            var sessionsAdded = 0
            while (sessionsAdded < sessions) {
                val date = rand.nextLocalDateTime(now - (sessions * 2).days, now).date

                val items = repo.getPlanItems(planId, date.dayOfWeek.isoDayNumber).ifEmpty { continue }
                sessionsAdded++

                val sessionId = sessionRepo.getSessionIdOrCreate(date)
                for (item in items) {
                    val exerciseId = item.exercise.id ?: continue
                    val setsCount = rand.nextInt(1, 4)
                    repeat(setsCount) {
                        val weight = rand.nextInt(10, 80).toFloat()
                        val reps = rand.nextInt(5, 15)
                        sessionRepo.addSet(
                            sessionId = sessionId,
                            exerciseId = exerciseId,
                            weight = weight,
                            reps = reps,
                            setType = SetType.entries.random(),
                            rir = RepsInReserve(2),
                        )
                        added.incrementAndFetch()
                    }
                }
            }
            snackbarState.showSnackbar("Mock data added: ${added.load()} sets")
        }
    }
}

@Stable
enum class PlanEditStage {
    NameEdit,
    PlanEdit,
}

@Stable
data class PlanEditState(
    val currentDay: Int,
    val dayCount: Int,
    val isWeekMode: Boolean,
    val exerciseSheetVisible: Boolean,
    val items: List<PlanItem>,
) {
    val exercises: List<Exercise> get() = items.map(PlanItem::exercise)
}
