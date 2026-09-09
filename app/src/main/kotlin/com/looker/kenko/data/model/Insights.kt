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

package com.looker.kenko.data.model

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * The same exercise as it went last time: every set of it, and the weight they added up to.
 *
 * The ghost of the previous session is what a lifter races against — the number to beat is
 * already in the journal.
 */
@Immutable
data class Ghost(
    val date: LocalDate,
    val sets: List<Set>,
) {
    val volume: Float get() = sets.sumOf { it.volume.toDouble() }.toFloat()

    /**
     * Set that stood in the same place last time, so rows line up one to one.
     */
    fun setAt(index: Int): Set? = sets.getOrNull(index)
}

/**
 * The last session before [before] where this exercise was performed.
 *
 * Drops are kept: they were part of that set and count towards the volume it moved.
 */
fun List<Session>.ghostOf(exerciseName: String, before: LocalDate): Ghost? = this
    .filter { it.date < before }
    .filter { session -> session.sets.any { it.exercise.name == exerciseName } }
    .maxByOrNull { it.date }
    ?.let { session ->
        Ghost(
            date = session.date,
            sets = session.sets.filter { it.exercise.name == exerciseName },
        )
    }

/**
 * Best set an exercise ever had, by the one-rep max it is worth.
 *
 * Weight alone would call `12 × 40` weaker than `1 × 45`, which is not how a lifter reads it.
 */
@Immutable
data class Record(
    val exercise: Exercise,
    val weight: Float,
    val reps: Int,
    val date: LocalDate,
    /**
     * The record this one replaced, if the journal holds one.
     */
    val previous: Float = 0F,
) {
    val estimatedMax: Float get() = oneRepMax(weight, reps)

    /**
     * How long the record has been standing, in days.
     */
    fun standingFor(today: LocalDate): Int = date.daysUntil(today).coerceAtLeast(0)

    val gain: Float get() = estimatedMax - previous
}

/**
 * Best set of every exercise in the journal, strongest first.
 */
fun List<Session>.records(): List<Record> {
    val byExercise = mutableMapOf<String, MutableList<Pair<LocalDate, Set>>>()
    forEach { session ->
        session.sets.forEach { set ->
            byExercise.getOrPut(set.exercise.name) { mutableListOf() }.add(session.date to set)
        }
    }
    return byExercise.values.mapNotNull { entries ->
        val ordered = entries.sortedWith(compareBy({ it.first }, { it.second.id ?: 0 }))
        var best: Pair<LocalDate, Set>? = null
        var previous = 0F
        ordered.forEach { candidate ->
            val current = best
            val worth = oneRepMax(candidate.second.weight, candidate.second.repsOrDuration)
            if (current == null) {
                best = candidate
            } else if (worth > oneRepMax(current.second.weight, current.second.repsOrDuration)) {
                previous = oneRepMax(current.second.weight, current.second.repsOrDuration)
                best = candidate
            }
        }
        best?.let { (date, set) ->
            if (set.weight <= 0F) {
                null
            } else {
                Record(
                    exercise = set.exercise,
                    weight = set.weight,
                    reps = set.repsOrDuration,
                    date = date,
                    previous = previous,
                )
            }
        }
    }.sortedByDescending { it.estimatedMax }
}

/**
 * Whether a set just written beats everything this exercise had before it.
 *
 * The receiver is the journal as it stood before the set landed.
 */
fun List<Session>.beatsRecord(set: Set, date: LocalDate): Record? {
    if (set.weight <= 0F) return null
    val standing = records().firstOrNull { it.exercise.name == set.exercise.name }
    val worth = oneRepMax(set.weight, set.repsOrDuration)
    if (standing != null && worth <= standing.estimatedMax) return null
    return Record(
        exercise = set.exercise,
        weight = set.weight,
        reps = set.repsOrDuration,
        date = date,
        previous = standing?.estimatedMax ?: 0F,
    )
}

/**
 * How much heavier the same exercise goes in one gym than in another.
 *
 * Plates and machines lie differently from place to place: a factor of `0.9` means this gym's
 * numbers run a tenth lower, so a working weight brought from the other gym should be cut.
 */
@Immutable
data class GymShift(
    val exerciseName: String,
    val factor: Float,
    val fromWeight: Float,
    val toWeight: Float,
) {
    /**
     * Below this the difference is noise, not a different machine.
     */
    val isMeaningful: Boolean get() = abs(1F - factor) >= MIN_SHIFT

    fun applyTo(weight: Float): Float = roundToStep(weight * factor)
}

private const val MIN_SHIFT = 0.04F

/**
 * At least this many sets in each gym before the numbers mean anything.
 */
private const val MIN_SETS_PER_GYM = 2

/**
 * Compares the top working weights of one exercise between two gyms.
 *
 * Returns null while either gym has too little history to judge by.
 */
fun List<Session>.gymShift(
    exerciseName: String,
    from: Int?,
    to: Int?,
): GymShift? = gymShift(from, to, exerciseName) { it.exercise.name == exerciseName }

/**
 * The same comparison for an exercise known by id — the sheet has the id, not the name.
 */
fun List<Session>.gymShift(
    exerciseId: Int,
    from: Int?,
    to: Int?,
): GymShift? = gymShift(from, to, exerciseId.toString()) { it.exercise.id == exerciseId }

private fun List<Session>.gymShift(
    from: Int?,
    to: Int?,
    label: String,
    isTheExercise: (Set) -> Boolean,
): GymShift? {
    if (from == to) return null
    val fromWeights = topWeights(from, isTheExercise)
    val toWeights = topWeights(to, isTheExercise)
    if (fromWeights.size < MIN_SETS_PER_GYM || toWeights.size < MIN_SETS_PER_GYM) return null
    val fromTop = fromWeights.average().toFloat()
    val toTop = toWeights.average().toFloat()
    if (fromTop <= 0F || toTop <= 0F) return null
    return GymShift(
        exerciseName = label,
        factor = toTop / fromTop,
        fromWeight = fromTop,
        toWeight = toTop,
    )
}

/**
 * Heaviest set of each session of this exercise in one gym, newest sessions first.
 */
private fun List<Session>.topWeights(
    gymId: Int?,
    isTheExercise: (Set) -> Boolean,
): List<Float> = this
    .filter { it.gymId == gymId }
    .sortedByDescending { it.date }
    .mapNotNull { session ->
        session.sets
            .filter { isTheExercise(it) && it.parentSetId == null }
            .maxOfOrNull { it.weight }
            ?.takeIf { it > 0F }
    }
    .take(MAX_SESSIONS_PER_GYM)

/**
 * Gym of the last session this exercise was done in.
 */
fun List<Session>.lastGymOf(exerciseId: Int): Int? = this
    .filter { session -> session.sets.any { it.exercise.id == exerciseId } }
    .maxByOrNull { it.date }
    ?.gymId

/**
 * Older sessions say little about today's machine, so only the recent ones count.
 */
private const val MAX_SESSIONS_PER_GYM = 6
