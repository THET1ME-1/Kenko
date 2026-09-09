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

package com.looker.kenko.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.looker.kenko.data.local.model.DEFAULT_BAR_WEIGHT
import com.looker.kenko.data.local.model.DEFAULT_REST_SECONDS
import com.looker.kenko.data.local.model.DEFAULT_TARGET_REPS
import com.looker.kenko.data.local.model.DEFAULT_TARGET_SETS
import com.looker.kenko.data.model.Labels.Difficulty
import com.looker.kenko.data.model.Labels.Equipment
import com.looker.kenko.data.model.Labels.Focus
import com.looker.kenko.data.model.Labels.Time
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Immutable
data class Plan(
    val name: String,
    val description: String?,
    val difficulty: Difficulty?,
    val focus: Focus?,
    val equipment: Equipment?,
    val time: Time?,
    val isActive: Boolean,
    val stat: PlanStat = PlanStat(0, 0),
    val id: Int? = null,
)

@Immutable
data class PlanItem(
    /**
     * Day of the plan, counted from one. In week mode it doubles as the ISO day of week.
     */
    val dayIndex: Int,
    val exercise: Exercise,
    val planId: Int,
    /**
     * Items of one day sharing a [supersetId] are performed as a superset.
     */
    val supersetId: Int? = null,
    val targetSets: Int = DEFAULT_TARGET_SETS,
    /**
     * Lower end of the rep range the plan asks for.
     */
    val targetReps: Int = DEFAULT_TARGET_REPS,
    /**
     * Upper end of the range. Equal to [targetReps] when the plan asks for one exact number.
     */
    val targetRepsMax: Int = DEFAULT_TARGET_REPS,
    /**
     * Bar the exercise is loaded on. Zero for movements without one.
     */
    val barWeight: Float = DEFAULT_BAR_WEIGHT,
    /**
     * Weight hanging on each side. They can differ, which dumbbells and machines do all the time.
     */
    val leftWeight: Float = 0F,
    val rightWeight: Float = 0F,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val order: Int = 0,
    /**
     * Drops the plan asks for on every set of this exercise. Zero means plain sets.
     */
    val dropCount: Int = 0,
    val dropPercent: Int = DEFAULT_DROP_PERCENT,
    /**
     * Handle the plan expects for this exercise.
     */
    val gripId: Int? = null,
    val id: Long? = null,
) {
    /**
     * Sets this item adds to the day, drops counted in.
     */
    val setCount: Int get() = targetSets * (dropCount + 1)

    /**
     * What the bar weighs once loaded.
     */
    val targetWeight: Float get() = barWeight + leftWeight + rightWeight

    /**
     * `10` for an exact number, `6–10` for a range.
     */
    val repsLabel: String
        get() = if (targetRepsMax > targetReps) "$targetReps–$targetRepsMax" else "$targetReps"
}

val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

val week = DatePeriod(days = 7)

class PlanPreviewParameters : PreviewParameterProvider<List<Plan>> {
    override val values: Sequence<List<Plan>> = sequenceOf(
        listOf(
            Plan(
                name = "Push Pull Leg",
                description = null,
                difficulty = Difficulty.ADAPTABLE,
                focus = null,
                equipment = Equipment.FULL_GYM,
                time = Time.NORMAL,
                isActive = true,
                stat = PlanStat(21, 5),
            ),
            Plan(
                name = "Upper Lower",
                description = "Alternative upper lower split",
                difficulty = Difficulty.BEGINNER,
                focus = Focus.POWER_BUILDING,
                equipment = Equipment.FULL_GYM,
                time = Time.QUICK,
                isActive = false,
                stat = PlanStat(21, 4),
            ),
            Plan(
                name = "Upper Lower 2",
                description = "Lower Upper split at home",
                difficulty = Difficulty.ADAPTABLE,
                focus = Focus.POWER_BUILDING,
                equipment = Equipment.DUMBBELLS,
                time = Time.QUICK,
                isActive = false,
                stat = PlanStat(21, 5),
            ),
        ),
    )
}
