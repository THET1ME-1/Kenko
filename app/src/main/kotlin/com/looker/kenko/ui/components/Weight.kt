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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.looker.kenko.R
import com.looker.kenko.data.model.WeightUnit

/**
 * The unit the screen speaks. Everything below the screen keeps kilograms.
 */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.Kg }

/**
 * A weight held in kilograms, written for the eye that reads this app.
 */
@Composable
fun weightText(kilograms: Float): String = LocalWeightUnit.current.formatKilograms(kilograms)

/**
 * `КГ` or `LB`, whichever the settings say.
 */
@Composable
fun unitLabel(): String = when (LocalWeightUnit.current) {
    WeightUnit.Kg -> stringResource(R.string.label_kg)
    WeightUnit.Lb -> stringResource(R.string.label_lb)
}
