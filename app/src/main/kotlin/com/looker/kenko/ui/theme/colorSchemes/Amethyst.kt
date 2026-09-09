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

package com.looker.kenko.ui.theme.colorSchemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.looker.kenko.R

// Amethyst — the palette of the design mockup: violet all the way through,
// with the coral of the mockup kept for errors and the second leg of a superset.

private val primaryLight = Color(0xFF714BA8)
private val onPrimaryLight = Color(0xFFFCFBFF)
private val primaryContainerLight = Color(0xFFEBDFFF)
private val onPrimaryContainerLight = Color(0xFF4E2D7A)
private val secondaryLight = Color(0xFF5B338F)
private val onSecondaryLight = Color(0xFFFCFBFF)
private val secondaryContainerLight = Color(0xFFF2EDFD)
private val onSecondaryContainerLight = Color(0xFF4E2D7A)
private val tertiaryLight = Color(0xFF5B338F)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFE4D4F8)
private val onTertiaryContainerLight = Color(0xFF3B1C63)
private val errorLight = Color(0xFFC1514D)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF410002)
private val backgroundLight = Color(0xFFFCFBFF)
private val onBackgroundLight = Color(0xFF211D29)
private val surfaceLight = Color(0xFFFCFBFF)
private val onSurfaceLight = Color(0xFF211D29)
private val surfaceVariantLight = Color(0xFFEDE9F6)
private val onSurfaceVariantLight = Color(0xFF665F73)
private val outlineLight = Color(0xFF898296)
private val outlineVariantLight = Color(0xFFE0DBEC)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF322C38)
private val inverseOnSurfaceLight = Color(0xFFF4F2FB)
private val inversePrimaryLight = Color(0xFFD8BBF6)
private val surfaceDimLight = Color(0xFFDDD8E6)
private val surfaceBrightLight = Color(0xFFFCFBFF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFBF9FF)
private val surfaceContainerLight = Color(0xFFF4F2FB)
private val surfaceContainerHighLight = Color(0xFFF1EEFA)
private val surfaceContainerHighestLight = Color(0xFFEDE9F6)

private val primaryDark = Color(0xFFD3B4F5)
private val onPrimaryDark = Color(0xFF3B1C63)
private val primaryContainerDark = Color(0xFF53308A)
private val onPrimaryContainerDark = Color(0xFFEBDFFF)
private val secondaryDark = Color(0xFFC6B3DC)
private val onSecondaryDark = Color(0xFF352A44)
private val secondaryContainerDark = Color(0xFF3F3350)
private val onSecondaryContainerDark = Color(0xFFE7DBF6)
private val tertiaryDark = Color(0xFFD9BFF7)
private val onTertiaryDark = Color(0xFF3B1C63)
private val tertiaryContainerDark = Color(0xFF4A2C74)
private val onTertiaryContainerDark = Color(0xFFEBDFFF)
private val errorDark = Color(0xFFF2A9A5)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF16121B)
private val onBackgroundDark = Color(0xFFE9E3F2)
private val surfaceDark = Color(0xFF16121B)
private val onSurfaceDark = Color(0xFFE9E3F2)
private val surfaceVariantDark = Color(0xFF443A50)
private val onSurfaceVariantDark = Color(0xFFC9C0D6)
private val outlineDark = Color(0xFF948AA3)
private val outlineVariantDark = Color(0xFF443A50)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE9E3F2)
private val inverseOnSurfaceDark = Color(0xFF322C38)
private val inversePrimaryDark = Color(0xFF714BA8)
private val surfaceDimDark = Color(0xFF16121B)
private val surfaceBrightDark = Color(0xFF3C3544)
private val surfaceContainerLowestDark = Color(0xFF110D15)
private val surfaceContainerLowDark = Color(0xFF1D1823)
private val surfaceContainerDark = Color(0xFF221C29)
private val surfaceContainerHighDark = Color(0xFF2C2534)
private val surfaceContainerHighestDark = Color(0xFF37303F)

private val amethystLightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val amethystDarkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

val amethystColorSchemes = ColorSchemes(
    light = amethystLightScheme,
    dark = amethystDarkScheme,
    nameRes = R.string.label_color_scheme_amethyst,
)
