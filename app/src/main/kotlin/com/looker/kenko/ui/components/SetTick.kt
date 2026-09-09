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

package com.looker.kenko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.theme.KenkoIcons

enum class TickState {
    /** Written down already. */
    Done,

    /** Next in line. */
    Active,

    /** Still ahead, drawn hollow. */
    Ahead,
}

/**
 * The round mark at the end of a set row: filled when done, outlined when it is the next one,
 * empty while it waits.
 */
@Composable
fun SetTick(
    state: TickState,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                when (state) {
                    TickState.Done -> Modifier.background(colors.primary)
                    TickState.Active -> Modifier.border(KenkoBorderWidth, colors.primary, CircleShape)
                    TickState.Ahead -> Modifier.border(KenkoBorderWidth, colors.outlineVariant, CircleShape)
                },
            )
            .then(clickable),
        contentAlignment = Alignment.Center,
    ) {
        if (state != TickState.Ahead) {
            Icon(
                painter = KenkoIcons.Done,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6F),
                tint = if (state == TickState.Done) colors.onPrimary else colors.primary,
            )
        }
    }
}
