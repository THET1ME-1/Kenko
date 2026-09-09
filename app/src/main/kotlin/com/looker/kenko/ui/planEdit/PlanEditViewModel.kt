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
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.localDate
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

    private val _fullDaySelection: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _selectedItems: MutableStateFlow<Set<Long>> = MutableStateFlow(emptySet())

    private val _supersetMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _editedItem: MutableStateFlow<PlanItem?> = MutableStateFlow(null)
    val editedItem: StateFlow<PlanItem?> = _editedItem

    private val _replacedItem: MutableStateFlow<PlanItem?> = MutableStateFlow(null)
    val replacedItem: StateFlow<PlanItem?> = _replacedItem

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
        _fullDaySelection,
        _isSheetVisible,
        combine(
            _selectedItems,
            _supersetMode,
            settingsRepo.get { isWeekMode },
        ) { selected, mode, weekMode -> Triple(selected, mode, weekMode) },
    ) { items, day, daySelection, sheetVisible, selection ->
        PlanEditState(
            currentDay = day,
            dayCount = items.maxOfOrNull { it.dayIndex } ?: 0,
            isWeekMode = selection.third,
            selectionMode = daySelection,
            exerciseSheetVisible = sheetVisible,
            items = items.filter { it.dayIndex == day },
            selectedItems = selection.first,
            supersetMode = selection.second,
        )
    }.asStateFlow(
        PlanEditState(
            currentDay = 1,
            dayCount = 0,
            isWeekMode = false,
            selectionMode = false,
            exerciseSheetVisible = false,
            items = emptyList(),
            selectedItems = emptySet(),
        ),
    )

    fun toggleSelection(item: PlanItem) {
        val id = item.id ?: return
        viewModelScope.launch {
            _selectedItems.emit(
                if (id in _selectedItems.value) _selectedItems.value - id else _selectedItems.value + id,
            )
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            _selectedItems.emit(emptySet())
            _supersetMode.emit(false)
        }
    }

    /**
     * Turns the day list into a picker: tapping rows now gathers them into a superset.
     */
    fun startSupersetMode() {
        viewModelScope.launch {
            _supersetMode.emit(true)
        }
    }

    /**
     * Ties the picked exercises of the day into one superset.
     */
    fun makeSuperset() {
        viewModelScope.launch {
            val picked = _selectedItems.value.toList()
            if (picked.size < 2) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_superset_needs_two))
                return@launch
            }
            repo.setSuperset(picked, repo.nextSupersetId(planIdStream.value))
            _selectedItems.emit(emptySet())
            _supersetMode.emit(false)
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
            planIdStream.emit(createId)
        }
    }

    fun setCurrentDay(day: Int) {
        viewModelScope.launch {
            _dayIndex.emit(day.coerceAtLeast(1))
            if (_fullDaySelection.value) {
                _fullDaySelection.emit(false)
            }
        }
    }

    fun openFullDaySelection() {
        viewModelScope.launch {
            _fullDaySelection.emit(true)
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
            _selectedItems.emit(_selectedItems.value - id)
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
    val selectionMode: Boolean,
    val exerciseSheetVisible: Boolean,
    val items: List<PlanItem>,
    val selectedItems: Set<Long> = emptySet(),
    val supersetMode: Boolean = false,
) {
    val exercises: List<Exercise> get() = items.map(PlanItem::exercise)
}
