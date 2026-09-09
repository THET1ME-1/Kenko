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
import com.looker.kenko.data.model.DEFAULT_DROP_PERCENT
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set

@Entity(
    "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SetEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sessionId", "exerciseId"),
        Index("parentSetId"),
    ],
)
data class SetEntity(
    @ColumnInfo("reps")
    val repsOrDuration: Int,
    val weight: Float,
    val type: SetType,
    val order: Int,
    val sessionId: Int,
    val exerciseId: Int,
    val rir: Int = 2,
    /**
     * Set apart from a clean one: partial reps, negatives, help, a pause.
     */
    @ColumnInfo(defaultValue = "NULL")
    val weightNote: WeightNote? = null,
    @ColumnInfo(defaultValue = "NULL")
    val parentSetId: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val dropIndex: Int = 0,
    @ColumnInfo(defaultValue = "NULL")
    val supersetId: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val roundIndex: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val dropCount: Int = 0,
    @ColumnInfo(defaultValue = "20")
    val dropPercent: Int = DEFAULT_DROP_PERCENT,
    @ColumnInfo(defaultValue = "NULL")
    val gripId: Int? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun SetEntity.toExternal(exercise: Exercise): Set = Set(
    repsOrDuration = repsOrDuration,
    weight = weight,
    type = type,
    exercise = exercise,
    rir = RepsInReserve(rir),
    weightNote = weightNote,
    parentSetId = parentSetId,
    dropIndex = dropIndex,
    supersetId = supersetId,
    roundIndex = roundIndex,
    dropCount = dropCount,
    dropPercent = dropPercent,
    gripId = gripId,
    id = id,
)

fun Set.toEntity(sessionId: Int, order: Int): SetEntity = SetEntity(
    id = id ?: 0,
    repsOrDuration = repsOrDuration,
    weight = weight,
    type = type,
    order = order,
    sessionId = sessionId,
    exerciseId = requireNotNull(exercise.id),
    rir = rir.value,
    weightNote = weightNote,
    parentSetId = parentSetId,
    dropIndex = dropIndex,
    supersetId = supersetId,
    roundIndex = roundIndex,
    dropCount = dropCount,
    dropPercent = dropPercent,
    gripId = gripId,
)
