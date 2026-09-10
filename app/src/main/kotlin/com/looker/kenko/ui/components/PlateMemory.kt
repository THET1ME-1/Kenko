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

package com.looker.kenko.ui.components

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Навеска, которую человек собрал руками, помнится до конца тренировки.
 *
 * Раньше калькулятор каждый раз пересчитывал блины из веса и терял ручную сборку: вышел из
 * шторки — и набирай заново. Штанга между подходами меняется на пару блинов, а не целиком.
 */
@Singleton
class PlateMemory @Inject constructor() {

    private val byExercise = mutableMapOf<Int, Loadout>()

    fun of(exerciseId: Int): Loadout? = byExercise[exerciseId]

    fun remember(exerciseId: Int, loadout: Loadout) {
        byExercise[exerciseId] = loadout
    }
}
