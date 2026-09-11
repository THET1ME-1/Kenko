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

package com.looker.kenko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.looker.kenko.R

/**
 * Width of the monospace: the widest instance of Martian Mono, the one the type was chosen by.
 */
private const val MONO_WIDTH = 112.5F

private fun monoFont(weight: FontWeight, axis: Float) = Font(
    resId = R.font.martianmono_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(axis.toInt()),
        FontVariation.width(MONO_WIDTH),
    ),
)

private fun displayFont(weight: FontWeight, axis: Float) = Font(
    resId = R.font.jura_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis.toInt())),
)

val FontFamily.Companion.Numbers
    get() = FontFamily(monoFont(FontWeight.Bold, 700F))

val displayFont = FontFamily(
    displayFont(FontWeight.Bold, 700F),
    displayFont(FontWeight.SemiBold, 600F),
)

val bodyFont = FontFamily(
    monoFont(FontWeight.Bold, 700F),
    monoFont(FontWeight.Normal, 400F),
)

/**
 * The size comes from a resource: Russian words are longer, and at 78sp they break mid-word.
 */
@Composable
fun Typography.header(): TextStyle {
    val size = integerResource(R.integer.header_font_size)
    return displayLarge.copy(
        fontSize = size.sp,
        lineHeight = (size * 0.9F).sp,
    )
}

fun TextStyle.numbers() = copy(fontFamily = FontFamily.Numbers)

val baseline = Typography()

val Typography = Typography().copy(
    displayLarge = baseline.displayLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
    ),
    displayMedium = baseline.displayMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        lineHeight = 45.sp,
    ),
    displaySmall = baseline.displaySmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = baseline.headlineMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = baseline.headlineSmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = baseline.titleLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = baseline.titleMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    ),
    titleSmall = baseline.titleSmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = baseline.bodyLarge.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = baseline.bodyMedium.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = baseline.bodySmall.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = baseline.labelLarge.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
    labelMedium = baseline.labelMedium.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
    labelSmall = baseline.labelSmall.copy(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
    ),
)
