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

package com.looker.kenko.ui.theme

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.looker.kenko.data.model.settings.Theme
import com.looker.kenko.ui.theme.colorSchemes.ColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.defaultColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.sereneColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.twilightColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.amethystColorSchemes

data class KenkoThemeConfig(
    val colorSchemes: ColorSchemes,
    val theme: Theme,
)

class KenkoThemePreviewParameter : PreviewParameterProvider<KenkoThemeConfig> {
    override val values = sequenceOf(
        KenkoThemeConfig(amethystColorSchemes, Theme.Light),
        KenkoThemeConfig(amethystColorSchemes, Theme.Dark),
        KenkoThemeConfig(defaultColorSchemes, Theme.Light),
        KenkoThemeConfig(defaultColorSchemes, Theme.Dark),
        KenkoThemeConfig(sereneColorSchemes, Theme.Light),
        KenkoThemeConfig(sereneColorSchemes, Theme.Dark),
        KenkoThemeConfig(twilightColorSchemes, Theme.Light),
        KenkoThemeConfig(twilightColorSchemes, Theme.Dark),
    )

    override fun getDisplayName(index: Int): String? = when (index) {
        0 -> "Zestful Light"
        1 -> "Zestful Dark"
        2 -> "Default Light"
        3 -> "Default Dark"
        4 -> "Serene Light"
        5 -> "Serene Dark"
        6 -> "Twilight Light"
        7 -> "Twilight Dark"
        else -> null
    }
}
