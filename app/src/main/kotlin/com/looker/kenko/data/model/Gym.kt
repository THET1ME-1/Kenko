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

/**
 * A gym the lifter trains at, with the exercises it can actually do.
 *
 * Nothing is selected at first: until a gym is picked the app offers every exercise.
 */
@Immutable
data class Gym(
    val name: String,
    val note: String? = null,
    val exerciseCount: Int = 0,
    val id: Int? = null,
)

/**
 * A handle for an exercise — wide, close, rope — with its own photo.
 */
@Immutable
data class Grip(
    val exerciseId: Int,
    val name: String,
    val photoUri: String? = null,
    val id: Int? = null,
)
