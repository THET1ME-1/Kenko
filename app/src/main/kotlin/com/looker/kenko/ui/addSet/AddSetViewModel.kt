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

package com.looker.kenko.ui.addSet

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.GripRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AddSetViewModel.AddSetViewModelFactory::class)
class AddSetViewModel @AssistedInject constructor(
    private val sessionRepo: SessionRepo,
    gripRepo: GripRepo,
    @Assisted private val target: AddSetTarget,
) : ViewModel() {

    private val id: Int = target.exerciseId

    var reps by mutableIntStateOf(12)
    val weights: TextFieldState = TextFieldState("20.0")

    var selectedSetType by mutableStateOf(SetType.Standard)
        private set

    /**
     * Handles this exercise can be done with.
     */
    val grips: StateFlow<List<Grip>> = gripRepo.grips(target.exerciseId).asStateFlow(emptyList())

    var selectedGripId: Int? by mutableStateOf(target.gripId)
        private set

    /**
     * What this exercise looked like last time, shown under the title.
     */
    var lastSet: Set? by mutableStateOf(null)
        private set

    /**
     * Reps left in the tank. Kenko counts them into the rating of a set.
     */
    var repsInReserve: Int by mutableIntStateOf(2)
        private set

    fun cycleReserve() {
        repsInReserve = (repsInReserve + 1) % 5
    }

    fun setReserve(value: Int) {
        repsInReserve = value.coerceIn(0, 4)
    }

    fun cycleSetType() {
        selectedSetType = when (selectedSetType) {
            SetType.Standard -> SetType.Drop
            SetType.Drop -> SetType.RestPause
            SetType.RestPause -> SetType.Standard
        }
    }

    fun setWeight(value: Float) {
        weights.setTextAndPlaceCursorAtEnd(formatWeight(value))
    }

    fun selectGrip(gripId: Int?) {
        selectedGripId = gripId
    }

    fun setSetType(type: SetType) {
        selectedSetType = type
    }

    fun addRep(value: Int) {
        reps += value
    }

    fun addWeight(value: Float) {
        weights.setTextAndPlaceCursorAtEnd((weightFloat + value).toString())
    }

    fun addSet() {
        viewModelScope.launch {
            val parentSetId = target.parentSetId
            if (parentSetId != null) {
                sessionRepo.addDrop(
                    parentSetId = parentSetId,
                    weight = weightFloat,
                    reps = reps,
                    rir = RepsInReserve(repsInReserve),
                    dropIndex = target.dropIndex,
                )
                return@launch
            }
            val sessionId = sessionRepo.getSessionIdOrCreate(localDate)
            sessionRepo.addSet(
                sessionId = sessionId,
                exerciseId = id,
                weight = weightFloat,
                reps = reps,
                setType = selectedSetType,
                rir = RepsInReserve(repsInReserve),
                supersetId = target.supersetId,
                dropCount = when {
                    selectedSetType == SetType.Drop -> maxOf(1, target.dropCount)
                    else -> target.dropCount
                },
                dropPercent = target.dropPercent,
                gripId = selectedGripId,
            )
        }
    }

    private inline val weightFloat: Float
        get() = weights.text.toString().toFloatOrNull() ?: 0F

    init {
        if (target.dropCount > 0) {
            setSetType(SetType.Drop)
        }
        val suggestion = target.suggestion
        if (suggestion != null) {
            reps = suggestion.reps
            addWeight(suggestion.weight - weightFloat)
            setSetType(SetType.Drop)
        } else {
            viewModelScope.launch {
                val last = sessionRepo.getLastSetByExerciseId(id)
                lastSet = last
                if (last != null) {
                    reps = last.repsOrDuration
                    addWeight(last.weight - weightFloat)
                    setSetType(last.type)
                } else {
                    if (target.planReps > 0) reps = target.planReps
                    if (target.planWeight > 0F) addWeight(target.planWeight - weightFloat)
                }
            }
        }
    }

    @AssistedFactory
    interface AddSetViewModelFactory {
        fun create(target: AddSetTarget): AddSetViewModel
    }

    object IntTransformation : InputTransformation {
        override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        override fun TextFieldBuffer.transformInput() {
            if (!asCharSequence().isDigitsOnly()) {
                revertAllChanges()
            }
        }
    }

    object FloatTransformation : InputTransformation {
        override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        override fun TextFieldBuffer.transformInput() {
            toString().toFloatOrNull() ?: revertAllChanges()
        }
    }
}
