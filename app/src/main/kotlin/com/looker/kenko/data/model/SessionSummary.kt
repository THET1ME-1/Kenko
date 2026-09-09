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

/**
 * Part of the day a session started in. The name of a session comes from it, until the lifter
 * types their own.
 */
enum class DayPart {
    Morning,
    Afternoon,
    Evening,
    Night,
}

/**
 * Hours the parts of the day cover, the way a gym-goer thinks of them.
 */
fun dayPartOf(hour: Int): DayPart = when (hour) {
    in 5..11 -> DayPart.Morning
    in 12..16 -> DayPart.Afternoon
    in 17..22 -> DayPart.Evening
    else -> DayPart.Night
}

/**
 * What the session added up to. Warm-ups are left out of every number here — they were not
 * the work.
 */
@Immutable
data class SessionResult(
    val sets: Int,
    val reps: Int,
    val volume: Float,
    val exercises: Int,
    val muscles: Map<MuscleGroups, Float>,
    val records: List<Record>,
    val minutes: Int,
) {
    val isEmpty: Boolean get() = sets == 0

    val volumePerSet: Float get() = if (sets == 0) 0F else volume / sets
}

/**
 * Counts one session for the screen that closes it.
 *
 * [history] is the journal without this session — records are judged against it.
 */
fun Session.result(history: List<Session>, minutes: Int): SessionResult {
    val working = sets.filter { it.countsAsWork }
    val volumeByMuscle = mutableMapOf<MuscleGroups, Float>()
    working.forEach { set ->
        val volume = set.volume
        val target = set.exercise.target
        volumeByMuscle[target] = (volumeByMuscle[target] ?: 0F) + volume
        set.exercise.secondaryTargets.distinct().forEach { assisted ->
            if (assisted == target) return@forEach
            volumeByMuscle[assisted] = (volumeByMuscle[assisted] ?: 0F) + volume * ASSIST_SHARE
        }
    }
    val top = volumeByMuscle.values.maxOrNull() ?: 0F
    return SessionResult(
        sets = working.size,
        reps = working.sumOf { it.repsOrDuration },
        volume = working.sumOf { it.volume.toDouble() }.toFloat(),
        exercises = working.map { it.exercise.name }.distinct().size,
        muscles = if (top <= 0F) {
            emptyMap()
        } else {
            volumeByMuscle.mapValues { (_, value) -> (value / top).coerceIn(0F, 1F) }
        },
        records = recordsOf(history),
        minutes = minutes,
    )
}

/**
 * Records this session set: every set compared with the journal as it was before the session.
 */
private fun Session.recordsOf(history: List<Session>): List<Record> {
    val before = history.filter { it.id != id }
    val beaten = mutableMapOf<String, Record>()
    sets.sortedBy { it.id ?: 0 }.forEach { set ->
        val standing = beaten[set.exercise.name]
        val journal = if (standing == null) {
            before
        } else {
            before + Session(
                date = date,
                sets = listOf(set.copy(weight = standing.weight, repsOrDuration = standing.reps)),
                planId = planId,
            )
        }
        journal.beatsRecord(set, date)?.let { beaten[set.exercise.name] = it }
    }
    return beaten.values.sortedByDescending { it.estimatedMax }
}
