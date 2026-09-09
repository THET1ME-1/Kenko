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
import kotlin.math.roundToInt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Stretch of days the numbers are counted over.
 */
enum class StatsRange {
    Day,
    Week,
    Month,
    Year,
    Custom,
}

/**
 * A period with both ends included: `from` and `to` are days a session can land on.
 */
@Immutable
data class StatsPeriod(
    val from: LocalDate,
    val to: LocalDate,
    val range: StatsRange,
) {
    operator fun contains(date: LocalDate): Boolean = date in from..to

    val dayCount: Int get() = from.daysUntil(to) + 1

    /**
     * The same length of time, moved back or forward: last week, next month.
     *
     * A custom period keeps its length and slides by exactly that many days.
     */
    fun shifted(by: Int): StatsPeriod = when (range) {
        StatsRange.Day -> of(StatsRange.Day, from.plus(DatePeriod(days = by)))
        StatsRange.Week -> of(StatsRange.Week, from.plus(DatePeriod(days = 7 * by)))
        StatsRange.Month -> of(StatsRange.Month, from.plus(DatePeriod(months = by)))
        StatsRange.Year -> of(StatsRange.Year, from.plus(DatePeriod(years = by)))
        StatsRange.Custom -> {
            val length = dayCount
            copy(
                from = from.plus(DatePeriod(days = length * by)),
                to = to.plus(DatePeriod(days = length * by)),
            )
        }
    }

    companion object {
        /**
         * The period the day belongs to: its week starts on Monday, its month on the 1st.
         */
        fun of(range: StatsRange, anchor: LocalDate): StatsPeriod = when (range) {
            StatsRange.Day -> StatsPeriod(anchor, anchor, range)
            StatsRange.Week -> {
                val start = anchor.minus(
                    DatePeriod(days = anchor.dayOfWeek.ordinal),
                )
                StatsPeriod(start, start.plus(DatePeriod(days = 6)), range)
            }

            StatsRange.Month -> {
                val start = LocalDate(anchor.year, anchor.month, 1)
                StatsPeriod(start, start.plus(DatePeriod(months = 1, days = -1)), range)
            }

            StatsRange.Year -> {
                val start = LocalDate(anchor.year, 1, 1)
                StatsPeriod(start, LocalDate(anchor.year, 12, 31), range)
            }

            StatsRange.Custom -> StatsPeriod(anchor, anchor, range)
        }

        fun custom(from: LocalDate, to: LocalDate): StatsPeriod =
            if (from <= to) {
                StatsPeriod(from, to, StatsRange.Custom)
            } else {
                StatsPeriod(to, from, StatsRange.Custom)
            }
    }
}

/**
 * A helping muscle takes half the load: it works in the movement, but it is not the one being
 * trained. Without a split like this a bench press would count as a full triceps session.
 */
const val ASSIST_SHARE = 0.5F

/**
 * Kilograms moved in a set. Isometrics hold the weight for seconds, so the number is the same
 * arithmetic with a different unit behind it.
 */
val Set.volume: Float get() = weight * repsOrDuration

/**
 * What one muscle got out of the period.
 *
 * [volume] is weighted by [ASSIST_SHARE], [directSets] and [assistSets] are honest counts.
 */
@Immutable
data class MuscleLoad(
    val muscle: MuscleGroups,
    val volume: Float,
    val directSets: Int,
    val assistSets: Int,
    val reps: Int,
    val exercises: Int,
) {
    val sets: Int get() = directSets + assistSets

    val isTouched: Boolean get() = sets > 0
}

/**
 * What one exercise got out of the period, with the heaviest set kept for the record line.
 */
@Immutable
data class ExerciseLoad(
    val exercise: Exercise,
    val volume: Float,
    val sets: Int,
    val reps: Int,
    val topWeight: Float,
    val topReps: Int,
    val sessions: Int,
) {
    /**
     * Epley on the heaviest set of the period.
     */
    val estimatedMax: Float get() = oneRepMax(topWeight, topReps)
}

@Immutable
data class DayVolume(
    val date: LocalDate,
    val volume: Float,
    val sets: Int,
)

/**
 * Everything the stats screen shows for one period, counted in one pass over the sessions.
 */
@Immutable
data class StatsSummary(
    val period: StatsPeriod,
    val sessions: Int,
    val exercises: Int,
    val sets: Int,
    val reps: Int,
    val volume: Float,
    val muscles: List<MuscleLoad>,
    val exerciseLoads: List<ExerciseLoad>,
    val days: List<DayVolume>,
) {
    val isEmpty: Boolean get() = sets == 0

    val volumePerSession: Float get() = if (sessions == 0) 0F else volume / sessions

    val setsPerSession: Int get() = if (sessions == 0) 0 else (sets.toFloat() / sessions).roundToInt()

    val repsPerSet: Int get() = if (sets == 0) 0 else (reps.toFloat() / sets).roundToInt()

    val heaviestMuscle: MuscleLoad? get() = muscles.maxByOrNull { it.volume }?.takeIf { it.isTouched }

    val busiestDay: DayVolume? get() = days.maxByOrNull { it.volume }?.takeIf { it.sets > 0 }

    /**
     * Share of the period's volume that landed on this muscle, `0..1`.
     */
    fun share(load: MuscleLoad): Float {
        val total = muscles.sumOf { it.volume.toDouble() }.toFloat()
        return if (total <= 0F) 0F else load.volume / total
    }
}

/**
 * Counts a period out of the whole journal: what each muscle got, what each exercise got,
 * and how the volume sat across the days.
 */
fun List<Session>.summarize(period: StatsPeriod): StatsSummary {
    val inPeriod = filter { it.date in period }
    val muscleVolume = mutableMapOf<MuscleGroups, Float>()
    val muscleDirect = mutableMapOf<MuscleGroups, Int>()
    val muscleAssist = mutableMapOf<MuscleGroups, Int>()
    val muscleReps = mutableMapOf<MuscleGroups, Int>()
    val muscleExercises = mutableMapOf<MuscleGroups, MutableSet<String>>()

    val exerciseVolume = mutableMapOf<String, Float>()
    val exerciseSets = mutableMapOf<String, Int>()
    val exerciseReps = mutableMapOf<String, Int>()
    val exerciseTop = mutableMapOf<String, Pair<Float, Int>>()
    val exerciseSessions = mutableMapOf<String, MutableSet<LocalDate>>()
    val exerciseByName = mutableMapOf<String, Exercise>()

    var totalSets = 0
    var totalReps = 0
    var totalVolume = 0F

    val dayVolume = mutableMapOf<LocalDate, Float>()
    val daySets = mutableMapOf<LocalDate, Int>()

    inPeriod.forEach { session ->
        session.sets.forEach { set ->
            // Разминка двигает суставы, а не числа: в тоннаж и подходы она не идёт.
            if (!set.countsAsWork) return@forEach
            val exercise = set.exercise
            val volume = set.volume
            totalSets++
            totalReps += set.repsOrDuration
            totalVolume += volume

            dayVolume[session.date] = (dayVolume[session.date] ?: 0F) + volume
            daySets[session.date] = (daySets[session.date] ?: 0) + 1

            val key = exercise.name
            exerciseByName[key] = exercise
            exerciseVolume[key] = (exerciseVolume[key] ?: 0F) + volume
            exerciseSets[key] = (exerciseSets[key] ?: 0) + 1
            exerciseReps[key] = (exerciseReps[key] ?: 0) + set.repsOrDuration
            exerciseSessions.getOrPut(key) { mutableSetOf() }.add(session.date)
            val top = exerciseTop[key]
            if (top == null || set.weight > top.first) {
                exerciseTop[key] = set.weight to set.repsOrDuration
            }

            muscleVolume[exercise.target] = (muscleVolume[exercise.target] ?: 0F) + volume
            muscleDirect[exercise.target] = (muscleDirect[exercise.target] ?: 0) + 1
            muscleReps[exercise.target] =
                (muscleReps[exercise.target] ?: 0) + set.repsOrDuration
            muscleExercises.getOrPut(exercise.target) { mutableSetOf() }.add(key)

            exercise.secondaryTargets.distinct().forEach { assisted ->
                if (assisted == exercise.target) return@forEach
                muscleVolume[assisted] = (muscleVolume[assisted] ?: 0F) + volume * ASSIST_SHARE
                muscleAssist[assisted] = (muscleAssist[assisted] ?: 0) + 1
                muscleReps[assisted] = (muscleReps[assisted] ?: 0) + set.repsOrDuration
                muscleExercises.getOrPut(assisted) { mutableSetOf() }.add(key)
            }
        }
    }

    val muscles = MuscleGroups.entries.map { muscle ->
        MuscleLoad(
            muscle = muscle,
            volume = muscleVolume[muscle] ?: 0F,
            directSets = muscleDirect[muscle] ?: 0,
            assistSets = muscleAssist[muscle] ?: 0,
            reps = muscleReps[muscle] ?: 0,
            exercises = muscleExercises[muscle]?.size ?: 0,
        )
    }.sortedByDescending { it.volume }

    val exerciseLoads = exerciseByName.values.map { exercise ->
        val key = exercise.name
        val top = exerciseTop[key] ?: (0F to 0)
        ExerciseLoad(
            exercise = exercise,
            volume = exerciseVolume[key] ?: 0F,
            sets = exerciseSets[key] ?: 0,
            reps = exerciseReps[key] ?: 0,
            topWeight = top.first,
            topReps = top.second,
            sessions = exerciseSessions[key]?.size ?: 0,
        )
    }.sortedByDescending { it.volume }

    val days = buildList {
        var day = period.from
        while (day <= period.to) {
            add(
                DayVolume(
                    date = day,
                    volume = dayVolume[day] ?: 0F,
                    sets = daySets[day] ?: 0,
                ),
            )
            day = day.plus(DatePeriod(days = 1))
        }
    }

    return StatsSummary(
        period = period,
        sessions = inPeriod.count { session -> session.sets.any { it.countsAsWork } },
        exercises = exerciseByName.size,
        sets = totalSets,
        reps = totalReps,
        volume = totalVolume,
        muscles = muscles,
        exerciseLoads = exerciseLoads,
        days = days,
    )
}

/**
 * Every set of one muscle inside the period, newest first — the report rows and the muscle screen
 * read from here.
 */
fun List<Session>.setsOfMuscle(
    muscle: MuscleGroups,
    period: StatsPeriod,
): List<Pair<LocalDate, Set>> = filter { it.date in period }
    .sortedByDescending { it.date }
    .flatMap { session ->
        session.sets
            .filter { it.countsAsWork }
            .filter { it.exercise.target == muscle || muscle in it.exercise.secondaryTargets }
            .map { session.date to it }
    }

/**
 * Every set of one exercise inside the period, newest first.
 */
fun List<Session>.setsOfExercise(
    exerciseName: String,
    period: StatsPeriod,
): List<Pair<LocalDate, Set>> = filter { it.date in period }
    .sortedByDescending { it.date }
    .flatMap { session ->
        session.sets
            .filter { it.exercise.name == exerciseName }
            .map { session.date to it }
    }
