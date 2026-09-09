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

package com.looker.kenko.ui.addEditExercise

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.PhotoStore
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.GripRepo
import com.looker.kenko.ui.navigation.Routes
import com.looker.kenko.utils.asStateFlow
import com.looker.kenko.utils.isValidUrl
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AddEditExerciseViewModel.Factory::class)
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddEditExerciseViewModel @AssistedInject constructor(
    private val repo: ExerciseRepo,
    private val gripRepo: GripRepo,
    private val stringHandler: StringHandler,
    private val photoStore: PhotoStore,
    @Assisted private val routeData: Routes.AddEditExercise,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(routeData: Routes.AddEditExercise): AddEditExerciseViewModel
    }

    private val exerciseId: Int? = routeData.id

    private val defaultTarget: MuscleGroups? = routeData.target?.let { MuscleGroups.valueOf(it) }

    private val targetMuscle = MutableStateFlow(MuscleGroups.Chest)

    private val isIsometric = MutableStateFlow(false)

    private val secondaryMuscles = MutableStateFlow(emptySet<MuscleGroups>())

    private val photoUri = MutableStateFlow<String?>(null)

    /**
     * The exercise as it lies in the library. An edit must not lose what the screen never shows —
     * the Russian name and the illustration come from the catalogue and belong to the row.
     */
    private val original = MutableStateFlow<Exercise?>(null)

    private val isReadOnly: Boolean = exerciseId != null

    val snackbarState = SnackbarHostState()

    var exerciseName: String by mutableStateOf("")
        private set

    var reference: String by mutableStateOf("")
        private set

    private val isReferenceInvalid = snapshotFlow { reference }
        .debounce(200.milliseconds)
        .mapLatest { it.isValidUrl() && it.isNotBlank() }

    private val exerciseAlreadyExistError = snapshotFlow { exerciseName }
        .debounce(200.milliseconds)
        .mapLatest { repo.isExerciseAvailable(it) && !isReadOnly }

    val state = combine(
        combine(targetMuscle, secondaryMuscles, photoUri, original, ::Basics),
        isIsometric,
        flowOf(isReadOnly),
        exerciseAlreadyExistError,
        isReferenceInvalid,
    ) { basics, isometric, readOnly, alreadyExist, referenceInvalid ->
        AddEditExerciseUiState(
            targetMuscle = basics.target,
            secondaryMuscles = basics.secondary,
            photoUri = basics.photo,
            illustration = basics.exercise?.illustration,
            frames = basics.exercise?.frames ?: 0,
            isIsometric = isometric,
            isReadOnly = readOnly,
            isError = alreadyExist,
            isReferenceInvalid = referenceInvalid,
        )
    }.asStateFlow(
        AddEditExerciseUiState(
            targetMuscle = MuscleGroups.Chest,
            secondaryMuscles = emptySet(),
            photoUri = null,
            illustration = null,
            frames = 0,
            isIsometric = false,
            isError = false,
            isReadOnly = false,
            isReferenceInvalid = false,
        ),
    )

    /**
     * One tap on the body: an untouched muscle becomes the main one, the main one steps aside
     * into the helpers, and a helper tapped again drops out.
     */
    fun toggleMuscle(muscle: MuscleGroups) {
        viewModelScope.launch {
            when {
                targetMuscle.value == muscle -> {
                    secondaryMuscles.emit(secondaryMuscles.value - muscle)
                }

                muscle in secondaryMuscles.value -> {
                    secondaryMuscles.emit(secondaryMuscles.value - muscle)
                }

                else -> {
                    secondaryMuscles.emit(secondaryMuscles.value + targetMuscle.value - muscle)
                    targetMuscle.emit(muscle)
                }
            }
        }
    }

    /**
     * Handles of this exercise. Empty until the lifter adds one.
     */
    val grips: StateFlow<List<Grip>> =
        if (exerciseId == null) {
            MutableStateFlow(emptyList())
        } else {
            gripRepo.grips(exerciseId).asStateFlow(emptyList())
        }

    fun addGrip(name: String) {
        val id = exerciseId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            gripRepo.upsert(Grip(exerciseId = id, name = name.trim()))
        }
    }

    fun setGripPhoto(grip: Grip, uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                photoStore.delete(grip.photoUri)
                gripRepo.upsert(grip.copy(photoUri = null))
                return@launch
            }
            photoStore.save(uri)?.let { path ->
                photoStore.delete(grip.photoUri)
                gripRepo.upsert(grip.copy(photoUri = path))
            }
        }
    }

    fun removeGrip(grip: Grip) {
        val id = grip.id ?: return
        viewModelScope.launch {
            photoStore.delete(grip.photoUri)
            gripRepo.delete(id)
        }
    }

    fun setPhoto(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                photoStore.delete(photoUri.value)
                photoUri.emit(null)
                return@launch
            }
            photoStore.save(uri)?.let { path ->
                photoStore.delete(photoUri.value)
                photoUri.emit(path)
            }
        }
    }

    fun setName(value: String) {
        exerciseName = value
    }

    fun addReference(value: String) {
        reference = value
    }

    fun setTargetMuscle(value: MuscleGroups) {
        viewModelScope.launch {
            targetMuscle.emit(value)
        }
    }

    fun setIsometric(value: Boolean) {
        viewModelScope.launch {
            isIsometric.emit(value)
        }
    }

    fun addNewExercise(onDone: () -> Unit) {
        viewModelScope.launch {
            if (exerciseName.isBlank()) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_exercise_name_empty))
                return@launch
            }
            if (state.value.isReferenceInvalid) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_invalid_reference_format))
                return@launch
            }
            repo.upsert(
                Exercise(
                    name = exerciseName,
                    target = targetMuscle.value,
                    reference = reference.ifBlank { null },
                    isIsometric = isIsometric.value,
                    photoUri = photoUri.value,
                    secondaryTargets = secondaryMuscles.value.toList(),
                    nameRu = original.value?.nameRu,
                    illustration = original.value?.illustration,
                    frames = original.value?.frames ?: 0,
                    id = exerciseId,
                ),
            )
            onDone()
        }
    }

    init {
        viewModelScope.launch {
            if (exerciseId != null) {
                val exercise = repo.get(exerciseId)
                exercise?.let {
                    original.emit(it)
                    setName(it.name)
                    addReference(it.reference ?: "")
                    setIsometric(it.isIsometric)
                    setTargetMuscle(it.target)
                    secondaryMuscles.emit(it.secondaryTargets.toSet())
                    photoUri.emit(it.photoUri)
                }
            } else {
                if (routeData.name != null) setName(routeData.name)
                if (defaultTarget != null) setTargetMuscle(defaultTarget)
            }
        }
    }
}

/**
 * What the four independent pieces of the form add up to before the rest of the state joins them.
 */
private data class Basics(
    val target: MuscleGroups,
    val secondary: Set<MuscleGroups>,
    val photo: String?,
    val exercise: Exercise?,
)

@Stable
data class AddEditExerciseUiState(
    val targetMuscle: MuscleGroups,
    val secondaryMuscles: Set<MuscleGroups>,
    val photoUri: String?,
    val illustration: String?,
    val frames: Int,
    val isIsometric: Boolean,
    val isError: Boolean,
    val isReadOnly: Boolean,
    val isReferenceInvalid: Boolean,
)
