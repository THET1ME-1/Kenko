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

package com.looker.kenko.ui.addEditExercise

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.DashedAddButton
import com.looker.kenko.ui.components.ErrorSnackbar
import com.looker.kenko.ui.components.KenkoButton
import com.looker.kenko.ui.components.BodyPicker
import com.looker.kenko.ui.components.kenkoTextFieldColor
import com.looker.kenko.ui.components.rememberIllustration
import com.looker.kenko.ui.components.rememberPhoto
import com.looker.kenko.ui.exercises.string
import com.looker.kenko.ui.exercises.localizedExerciseName
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter

@Composable
fun AddEditExercise(
    viewModel: AddEditExerciseViewModel,
    onDone: () -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val grips by viewModel.grips.collectAsStateWithLifecycle()
    // The field shows the name the library lists, not the English key behind it.
    val shown = state.originalName?.let { localizedExerciseName(it, state.originalNameRu) }
    LaunchedEffect(shown) { shown?.let(viewModel::showLocalizedName) }
    AddEditExercise(
        state = state,
        grips = grips,
        onAddGrip = viewModel::addGrip,
        onGripPhoto = viewModel::setGripPhoto,
        onRemoveGrip = viewModel::removeGrip,
        name = viewModel.exerciseName,
        reference = viewModel.reference,
        snackbarState = viewModel.snackbarState,
        onNameChange = viewModel::setName,
        onReferenceChange = viewModel::addReference,
        onMuscleClick = viewModel::toggleMuscle,
        onPhotoChange = viewModel::setPhoto,
        onIsometricChange = viewModel::setIsometric,
        onSaveClick = { viewModel.addNewExercise(onDone) },
        onBackPress = onBackPress,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditExercise(
    state: AddEditExerciseUiState,
    name: String,
    reference: String,
    grips: List<Grip> = emptyList(),
    onAddGrip: (String) -> Unit = {},
    onGripPhoto: (Grip, Uri?) -> Unit = { _, _ -> },
    onRemoveGrip: (Grip) -> Unit = {},
    snackbarState: SnackbarHostState = remember { SnackbarHostState() },
    onNameChange: (String) -> Unit = {},
    onReferenceChange: (String) -> Unit = {},
    onMuscleClick: (MuscleGroups) -> Unit = {},
    onPhotoChange: (Uri?) -> Unit = {},
    onIsometricChange: (Boolean) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onBackPress: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = {
                    Text(
                        text = stringResource(
                            if (state.isExisting) {
                                R.string.label_edit_exercise
                            } else {
                                R.string.label_add_exercise_header
                            },
                        ),
                    )
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) { ErrorSnackbar(data = it) }
        },
        // Saving stays under the thumb: the form is long, and the button used to sit at its end.
        floatingActionButton = {
            KenkoButton(
                onClick = onSaveClick,
                label = { Text(text = stringResource(R.string.label_save)) },
                icon = {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = KenkoIcons.Save,
                        contentDescription = null,
                    )
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PhotoBlock(
                photoUri = state.photoUri,
                illustration = state.illustration,
                frames = state.frames,
                onPick = onPhotoChange,
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = onNameChange,
                isError = state.isError,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = kenkoTextFieldColor(),
                placeholder = { Text(text = stringResource(R.string.label_name)) },
                leadingIcon = { Icon(painter = KenkoIcons.Rename, contentDescription = null) },
                supportingText = if (state.isError) {
                    { Text(text = stringResource(R.string.error_exercise_name_exists)) }
                } else {
                    null
                },
            )

            Spacer(Modifier.height(20.dp))

            MuscleSection(
                primary = state.targetMuscle,
                secondary = state.secondaryMuscles,
                onMuscleClick = onMuscleClick,
            )

            Spacer(Modifier.height(20.dp))

            IsometricSwitch(
                checked = state.isIsometric,
                onCheckedChange = onIsometricChange,
            )

            if (state.isExisting) {
                Spacer(Modifier.height(20.dp))
                GripSection(
                    grips = grips,
                    onAdd = onAddGrip,
                    onPhoto = onGripPhoto,
                    onRemove = onRemoveGrip,
                )
            }

            Spacer(Modifier.height(16.dp))

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = reference,
                onValueChange = onReferenceChange,
                isError = state.isReferenceInvalid,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = kenkoTextFieldColor(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                placeholder = { Text(text = stringResource(R.string.label_reference)) },
                leadingIcon = { Icon(painter = KenkoIcons.Lightbulb, contentDescription = null) },
                supportingText = {
                    Text(
                        text = stringResource(
                            if (state.isReferenceInvalid) {
                                R.string.error_invalid_reference_format
                            } else {
                                R.string.label_reference_optional
                            },
                        ),
                    )
                },
            )

            Spacer(Modifier.height(96.dp))
        }
    }
}

/**
 * The picture of the movement: the lifter's own photo if they took one, otherwise the two shots
 * from the catalogue taking turns, and an empty slot when the exercise has neither.
 */
@Composable
private fun PhotoBlock(
    photoUri: String?,
    onPick: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
    illustration: String? = null,
    frames: Int = 0,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onPick) },
    )
    val request = remember {
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    }
    val photo = rememberPhoto(photoUri)
    val drawing = rememberIllustration(
        illustration = illustration.takeIf { photo == null },
        frames = frames,
        animate = true,
    )
    val shown = photo ?: drawing
    Column(modifier = modifier.fillMaxWidth()) {
        if (shown != null) {
            Image(
                bitmap = shown,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16F / 10F)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { picker.launch(request) }) {
                    Text(
                        text = if (photo == null) {
                            stringResource(R.string.label_add_photo)
                        } else {
                            stringResource(R.string.label_replace_photo)
                        },
                    )
                }
                if (photo != null) {
                    TextButton(onClick = { onPick(null) }) {
                        Text(text = stringResource(R.string.label_remove_photo))
                    }
                }
            }
        } else {
            // No picture, no reserved 16:10 hole: the button stands on its own height.
            DashedAddButton(
                label = stringResource(R.string.label_add_photo),
                onClick = { picker.launch(request) },
                modifier = Modifier.fillMaxWidth(),
                accent = true,
            )
        }
    }
}

/**
 * Muscles are picked on the body, not from a list of chips.
 */
@Composable
private fun MuscleSection(
    primary: MuscleGroups,
    secondary: Set<MuscleGroups>,
    onMuscleClick: (MuscleGroups) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(R.string.label_primary_muscle).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = stringResource(primary.string),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val secondaryNames = secondary.map { stringResource(it.string) }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.label_secondary_muscles).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = secondaryNames.joinToString(" · ").ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        BodyPicker(
            primary = primary,
            secondary = secondary,
            onMuscleClick = onMuscleClick,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.label_pick_muscle_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * Handles of the exercise: wide, close, rope. Each can carry its own photo.
 */
@Composable
private fun GripSection(
    grips: List<Grip>,
    onAdd: (String) -> Unit,
    onPhoto: (Grip, Uri?) -> Unit,
    onRemove: (Grip) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newName by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_grips).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(R.string.label_grip_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
        grips.forEach { grip ->
            GripRow(
                grip = grip,
                onPhoto = { uri -> onPhoto(grip, uri) },
                onRemove = { onRemove(grip) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                modifier = Modifier.weight(1F),
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = kenkoTextFieldColor(),
                label = { Text(text = stringResource(R.string.label_grip_name)) },
            )
            TextButton(
                onClick = {
                    onAdd(newName)
                    newName = ""
                },
            ) {
                Text(text = stringResource(R.string.label_add))
            }
        }
    }
}

@Composable
private fun GripRow(
    grip: Grip,
    onPhoto: (Uri?) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onPhoto) },
    )
    val request = remember {
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    }
    val photo = rememberPhoto(grip.photoUri)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { picker.launch(request) },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    painter = KenkoIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            modifier = Modifier.weight(1F),
            text = grip.name,
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onRemove) {
            Text(text = stringResource(R.string.label_remove))
        }
    }
}

@Composable
private fun IsometricSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(R.string.label_is_isometric),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.label_is_isometric_DESC),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun AddEditExercisePreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    var name by remember { mutableStateOf("Bench Press") }
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        AddEditExercise(
            state = AddEditExerciseUiState(
                targetMuscle = MuscleGroups.Chest,
                secondaryMuscles = setOf(MuscleGroups.Triceps, MuscleGroups.Shoulders),
                photoUri = null,
                illustration = null,
                frames = 0,
                originalName = null,
                originalNameRu = null,
                isIsometric = false,
                isError = false,
                isExisting = false,
                isReferenceInvalid = false,
            ),
            name = name,
            reference = "",
            onNameChange = { name = it },
        )
    }
}
