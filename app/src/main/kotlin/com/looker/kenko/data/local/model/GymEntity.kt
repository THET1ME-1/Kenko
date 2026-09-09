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
import com.looker.kenko.data.model.Gym

/**
 * A place to train. What it holds decides which exercises the app offers.
 */
@Entity("gyms")
data class GymEntity(
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val note: String? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

/**
 * Equipment of a gym: one row per exercise the place can actually do.
 */
@Entity(
    tableName = "gym_exercises",
    primaryKeys = ["gymId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = GymEntity::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class GymExerciseEntity(
    val gymId: Int,
    val exerciseId: Int,
)

fun GymEntity.toExternal(exerciseCount: Int = 0): Gym = Gym(
    id = id,
    name = name,
    note = note,
    exerciseCount = exerciseCount,
)

fun Gym.toEntity(): GymEntity = GymEntity(
    id = id ?: 0,
    name = name,
    note = note,
)
