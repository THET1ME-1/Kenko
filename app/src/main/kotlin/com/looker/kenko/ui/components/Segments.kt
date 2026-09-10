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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.start

/**
 * Corner of one segment in a switch, the way the theme picker already draws them: round on the
 * outside, cut to four points where two segments meet.
 */
@Composable
fun segmentShape(index: Int, count: Int): CornerBasedShape = when {
    count == 1 -> CircleShape
    index == 0 -> CircleShape.end(SEGMENT_CUT)
    index == count - 1 -> CircleShape.start(SEGMENT_CUT)
    else -> RoundedCornerShape(SEGMENT_CUT)
}

private val SEGMENT_CUT = 4.dp

/**
 * Segments without borders: depth here comes from the step of the surface, as everywhere else.
 */
@OptIn(ExperimentalMaterial3Api::class)
val kenkoSegmentColors: SegmentedButtonColors
    @Composable
    get() = SegmentedButtonDefaults.colors(
        activeBorderColor = Color.Transparent,
        inactiveBorderColor = Color.Transparent,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
