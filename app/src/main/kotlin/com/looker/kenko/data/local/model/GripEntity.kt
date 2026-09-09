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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.kenko.data.model.Grip

/**
 * A handle an exercise can be done with: wide bar, close grip, rope.
 *
 * Keeping them here saves a copy of the whole exercise per handle, and each one can carry
 * its own picture.
 */
@Entity(
    tableName = "exercise_grips",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class GripEntity(
    val exerciseId: Int,
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val photoUri: String? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun GripEntity.toExternal(): Grip = Grip(
    id = id,
    exerciseId = exerciseId,
    name = name,
    photoUri = photoUri,
)

fun Grip.toEntity(): GripEntity = GripEntity(
    id = id ?: 0,
    exerciseId = exerciseId,
    name = name,
    photoUri = photoUri,
)
