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

package com.looker.kenko.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("exercise")
@Entity("exercises")
data class ExerciseEntity(
    val name: String,
    val target: MuscleGroups,
    val reference: String? = null,
    val isIsometric: Boolean = false,
    /**
     * File the app copied into its own folder, so the picture survives the gallery.
     */
    @ColumnInfo(defaultValue = "NULL")
    val photoUri: String? = null,
    /**
     * Muscles that also work, written as names separated by commas.
     */
    @ColumnInfo(defaultValue = "''")
    val secondaryTargets: String = "",
    /**
     * Name in the lifter's language, when the app ships one.
     */
    @ColumnInfo(defaultValue = "NULL")
    val nameRu: String? = null,
    /**
     * Folder of the illustration in the free-exercise-db set. Pictures are fetched on demand,
     * so the app itself stays small.
     */
    @ColumnInfo(defaultValue = "NULL")
    val illustration: String? = null,
    @ColumnInfo(defaultValue = "0")
    val frames: Int = 0,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
fun ExerciseEntity.toExternal(): Exercise = Exercise(
    id = id,
    name = name,
    nameRu = nameRu,
    illustration = illustration,
    frames = frames,
    target = target,
    reference = reference,
    isIsometric = isIsometric,
    photoUri = photoUri,
    secondaryTargets = secondaryTargets.toMuscleGroups(),
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id ?: 0,
    name = name,
    nameRu = nameRu,
    illustration = illustration,
    frames = frames,
    target = target,
    reference = reference,
    isIsometric = isIsometric,
    photoUri = photoUri,
    secondaryTargets = secondaryTargets.asStored(),
)

private fun String.toMuscleGroups(): List<MuscleGroups> =
    split(',')
        .mapNotNull { name ->
            MuscleGroups.entries.firstOrNull { it.name == name.trim() }
        }

private fun List<MuscleGroups>.asStored(): String = joinToString(",") { it.name }
