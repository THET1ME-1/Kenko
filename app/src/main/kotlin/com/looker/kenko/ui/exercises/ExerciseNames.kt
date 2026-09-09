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

package com.looker.kenko.ui.exercises

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise

/**
 * Names of the exercises the app ships with, so they read in the language of the phone.
 *
 * Exercises a lifter adds keep the name they were given.
 */
private val builtInNames: Map<String, Int> = mapOf(
    "curls" to R.string.exercise_curls,
    "barbell curls" to R.string.exercise_barbell_curls,
    "preacher curls" to R.string.exercise_preacher_curls,
    "incline bicep curls" to R.string.exercise_incline_bicep_curls,
    "tricep push-down" to R.string.exercise_tricep_push_down,
    "skull-crushers" to R.string.exercise_skull_crushers,
    "overhead extensions" to R.string.exercise_overhead_extensions,
    "lateral raises" to R.string.exercise_lateral_raises,
    "shoulder press" to R.string.exercise_shoulder_press,
    "face pulls" to R.string.exercise_face_pulls,
    "squats" to R.string.exercise_squats,
    "leg press" to R.string.exercise_leg_press,
    "hack squats" to R.string.exercise_hack_squats,
    "stiff legged deadlift" to R.string.exercise_stiff_legged_deadlift,
    "lying leg curls" to R.string.exercise_lying_leg_curls,
    "calf raises" to R.string.exercise_calf_raises,
    "hip thrusts" to R.string.exercise_hip_thrusts,
    "lunges" to R.string.exercise_lunges,
    "sit-ups" to R.string.exercise_sit_ups,
    "leg raises" to R.string.exercise_leg_raises,
    "bench press" to R.string.exercise_bench_press,
    "incline bench" to R.string.exercise_incline_bench,
    "pec dec" to R.string.exercise_pec_dec,
    "chest fly" to R.string.exercise_chest_fly,
    "shrugs" to R.string.exercise_shrugs,
    "lat pull-down" to R.string.exercise_lat_pull_down,
    "pull-ups" to R.string.exercise_pull_ups,
    "lat prayers" to R.string.exercise_lat_prayers,
    "bent-over rows" to R.string.exercise_bent_over_rows,
    "chest-supported rows" to R.string.exercise_chest_supported_rows,
    "leg extensions" to R.string.exercise_leg_extensions,
    "behind-the-back bicep curls" to R.string.exercise_behind_the_back_curls,
    "smith-squats" to R.string.exercise_smith_squats,
    "cable lateral raises" to R.string.exercise_cable_lateral_raises,
    "upright rows" to R.string.exercise_upright_rows,
    "tircep push down" to R.string.exercise_tricep_push_down,
    "calve raises" to R.string.exercise_calf_raises,
)

@Composable
fun Exercise.displayName(): String = localizedExerciseName(name)

/**
 * The string resource of a built-in name, for places that have a context but no composition —
 * the report writer among them.
 */
fun exerciseNameRes(name: String): Int? = builtInNames[name.trim().lowercase()]

/**
 * The same translation for a name that travels alone, without its exercise.
 */
@Composable
fun localizedExerciseName(name: String): String {
    val resource = builtInNames[name.trim().lowercase()] ?: return name
    return stringResource(resource)
}
