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
import com.looker.kenko.data.model.Labels.Difficulty
import com.looker.kenko.data.model.Labels.Equipment
import com.looker.kenko.data.model.Labels.Focus
import com.looker.kenko.data.model.Labels.Time
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.Plan
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.PlanStat

@Entity(tableName = "plans")
data class PlanEntity(
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val description: String?,
    @ColumnInfo(defaultValue = "NULL")
    val difficulty: Difficulty?,
    @ColumnInfo(defaultValue = "NULL")
    val focus: Focus?,
    @ColumnInfo(defaultValue = "NULL")
    val equipment: Equipment?,
    @ColumnInfo(defaultValue = "NULL")
    val time: Time?,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

@Entity(
    "plan_day",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("planId", "exerciseId"),
    ],
)
data class PlanDayEntity(
    val planId: Int,
    val exerciseId: Int,
    val dayOfWeek: Int,
    @ColumnInfo(defaultValue = "NULL")
    val supersetId: Int? = null,
    @ColumnInfo(defaultValue = "3")
    val targetSets: Int = DEFAULT_TARGET_SETS,
    @ColumnInfo(defaultValue = "10")
    val targetReps: Int = DEFAULT_TARGET_REPS,
    @ColumnInfo(defaultValue = "0")
    val targetRepsMax: Int = 0,
    @ColumnInfo(defaultValue = "20")
    val barWeight: Float = DEFAULT_BAR_WEIGHT,
    @ColumnInfo(defaultValue = "0")
    val leftWeight: Float = 0F,
    @ColumnInfo(defaultValue = "0")
    val rightWeight: Float = 0F,
    @ColumnInfo(defaultValue = "90")
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    @ColumnInfo(defaultValue = "0")
    val order: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val dropCount: Int = 0,
    @ColumnInfo(defaultValue = "20")
    val dropPercent: Int = DEFAULT_DROP_PERCENT,
    @ColumnInfo(defaultValue = "NULL")
    val gripId: Int? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

const val DEFAULT_TARGET_SETS = 3
const val DEFAULT_TARGET_REPS = 10
const val DEFAULT_REST_SECONDS = 90
const val DEFAULT_BAR_WEIGHT = 20F

fun PlanEntity.toExternal(isActive: Boolean, stat: PlanStat) = Plan(
    id = id,
    name = name,
    description = description,
    difficulty = difficulty,
    focus = focus,
    equipment = equipment,
    time = time,
    stat = stat,
    isActive = isActive,
)

fun Plan.toEntity(): PlanEntity = PlanEntity(
    id = id ?: 0,
    name = name,
    description = description,
    difficulty = difficulty,
    focus = focus,
    equipment = equipment,
    time = time,
)

fun PlanItem.toEntity() = PlanDayEntity(
    id = id ?: 0,
    planId = planId,
    exerciseId = requireNotNull(exercise.id) { "Exercise id cannot be null" },
    dayOfWeek = dayIndex,
    supersetId = supersetId,
    targetSets = targetSets,
    targetReps = targetReps,
    targetRepsMax = targetRepsMax,
    barWeight = barWeight,
    leftWeight = leftWeight,
    rightWeight = rightWeight,
    restSeconds = restSeconds,
    order = order,
    dropCount = dropCount,
    dropPercent = dropPercent,
    gripId = gripId,
)

inline fun PlanDayEntity.toExternal(block: (exerciseId: Int) -> Exercise?) = PlanItem(
    planId = planId,
    dayIndex = dayOfWeek,
    exercise = block(exerciseId) ?: DefaultExercise,
    supersetId = supersetId,
    targetSets = targetSets,
    targetReps = targetReps,
    targetRepsMax = targetRepsMax,
    barWeight = barWeight,
    leftWeight = leftWeight,
    rightWeight = rightWeight,
    restSeconds = restSeconds,
    order = order,
    dropCount = dropCount,
    dropPercent = dropPercent,
    gripId = gripId,
    id = id,
)

val DefaultExercise = Exercise(
    name = "Exercise Deleted",
    target = MuscleGroups.Core,
)
