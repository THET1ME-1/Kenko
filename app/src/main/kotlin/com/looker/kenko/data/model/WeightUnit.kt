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

import kotlin.math.round

/**
 * What the numbers mean on screen. The database always speaks kilograms — otherwise a switch
 * of units would rewrite years of history.
 */
enum class WeightUnit {
    Kg,
    Lb,
    ;

    /**
     * The smallest plate worth counting: two and a half kilos, five pounds.
     */
    val step: Float get() = if (this == Kg) 2.5F else 5F

    fun fromKilograms(kilograms: Float): Float =
        if (this == Kg) kilograms else kilograms * POUNDS_IN_KILOGRAM

    fun toKilograms(shown: Float): Float =
        if (this == Kg) shown else shown / POUNDS_IN_KILOGRAM

    /**
     * `70`, not `70.0`, and the half kilo stays when there is one.
     */
    fun format(shown: Float): String {
        val rounded = round(shown * 10F) / 10F
        return if (rounded == rounded.toInt().toFloat()) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    /**
     * A weight held in kilograms, written the way this unit reads it.
     */
    fun formatKilograms(kilograms: Float): String = format(fromKilograms(kilograms))
}

private const val POUNDS_IN_KILOGRAM = 2.2046226F
