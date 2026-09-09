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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Rest between sets, counted from the moment the set was written down.
 *
 * The state keeps the end time instead of a countdown, so the timer survives the screen going
 * away and comes back with the right number.
 */
@Immutable
data class RestTimer(
    val exerciseName: String,
    val exerciseNameRu: String? = null,
    val totalSeconds: Int,
    val endsAt: Instant,
) {
    fun secondsLeft(now: Instant): Int =
        ((endsAt - now).inWholeSeconds).coerceAtLeast(0L).toInt()

    fun isDone(now: Instant): Boolean = now >= endsAt

    /**
     * Moves the finish line, never below the current second.
     */
    fun shifted(by: Int, now: Instant): RestTimer {
        val left = secondsLeft(now)
        val shifted = (left + by).coerceIn(0, MAX_REST_SECONDS)
        return copy(
            totalSeconds = (totalSeconds + by).coerceIn(shifted, MAX_REST_SECONDS),
            endsAt = now + shifted.seconds,
        )
    }
}

const val MAX_REST_SECONDS = 3600

/**
 * `mm:ss`, the way a gym clock reads.
 */
fun formatSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val rest = safe % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}
