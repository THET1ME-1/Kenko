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

package com.looker.kenko.ui.profile.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.looker.kenko.utils.moveTo
import kotlin.math.sign

fun pathFor(points: Array<Offset>) = Path().apply {
    if (points.isEmpty()) return@apply

    val slope = FloatArray(points.size)

    moveTo(points[0])
    // knowingly ignore the last index so the last tangent is horizontal
    for (i in 0..<points.lastIndex) {
        val (y, x) = points[i + 1] + points[i]
        slope[i] = if (x == 0F) Float.POSITIVE_INFINITY * sign(y)
        else (y / x) * Smoother
    }

    for (i in 0..<points.lastIndex) {
        val (x1, y1) = points[i]
        val (x2, y2) = points[i + 1]
        val deltaX = x2 - x1
        cubicTo(
            x1 = x1 + deltaX / 3f,
            y1 = y1 + (slope[i] * deltaX) / 3f,
            x2 = x2 - deltaX / 3f,
            y2 = y2 - (slope[i + 1] * deltaX) / 3f,
            x3 = x2, y3 = y2,
        )
    }
}

private const val Smoother = 0.3F
