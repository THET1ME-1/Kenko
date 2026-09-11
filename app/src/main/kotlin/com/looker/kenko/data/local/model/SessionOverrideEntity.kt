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

package com.looker.kenko.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * A change the lifter made to a planned exercise for one session only.
 *
 * The program stays as it was written; the day gets its own version of it.
 */
@Entity(
    tableName = "session_overrides",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionOverrideEntity(
    @ColumnInfo(index = true)
    val sessionId: Int,
    @ColumnInfo(index = true)
    val exerciseId: Int,
    @ColumnInfo(defaultValue = "0")
    val skipped: Boolean = false,
    /**
     * Exercise performed instead of the planned one today.
     */
    @ColumnInfo(index = true, defaultValue = "NULL")
    val replacementId: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val orderIndex: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val targetSets: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val targetReps: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val targetRepsMax: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val barWeight: Float? = null,
    @ColumnInfo(defaultValue = "NULL")
    val leftWeight: Float? = null,
    @ColumnInfo(defaultValue = "NULL")
    val rightWeight: Float? = null,
    @ColumnInfo(defaultValue = "NULL")
    val restSeconds: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val dropCount: Int? = null,
    /**
     * Superset for today; `-1` breaks the one the plan gave.
     */
    @ColumnInfo(defaultValue = "NULL")
    val supersetId: Int? = null,
)
