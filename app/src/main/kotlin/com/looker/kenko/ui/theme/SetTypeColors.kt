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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.local.model.WeightNote

/**
 * Colour of every kind of set, so a glance down the list tells warm-ups from work.
 *
 * The app keeps one accent for everything else; these are labels, not decoration, and each one
 * has a pair — darker for a light background, lighter for a dark one.
 */
private val WarmupLight = Color(0xFF2F7D46)
private val WarmupDark = Color(0xFF7BD69B)

private val RestPauseLight = Color(0xFF2E6BA8)
private val RestPauseDark = Color(0xFF8CC0F0)

private val ClusterLight = Color(0xFF9A6216)
private val ClusterDark = Color(0xFFE7B267)

private val AmrapLight = Color(0xFFB03A36)
private val AmrapDark = Color(0xFFF09E9A)

private val NoteLight = Color(0xFF6C5A8E)
private val NoteDark = Color(0xFFC6B4E8)

/**
 * A dark scheme paints on a dark surface — the light halves of the pairs are for it.
 */
@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5F

@Composable
fun setTypeColor(type: SetType): Color {
    val dark = isDarkSurface()
    return when (type) {
        SetType.Warmup -> if (dark) WarmupDark else WarmupLight
        SetType.Standard -> MaterialTheme.colorScheme.onSurfaceVariant
        SetType.Drop -> MaterialTheme.colorScheme.primary
        SetType.RestPause -> if (dark) RestPauseDark else RestPauseLight
        SetType.Cluster -> if (dark) ClusterDark else ClusterLight
        SetType.Amrap -> if (dark) AmrapDark else AmrapLight
    }
}

/**
 * A weight with a note gets its own quiet colour: it is not a kind of work, it is a caveat.
 */
@Composable
fun weightNoteColor(note: WeightNote): Color {
    val dark = isDarkSurface()
    return when (note) {
        WeightNote.Partials, WeightNote.Negatives, WeightNote.Assisted, WeightNote.Paused ->
            if (dark) NoteDark else NoteLight
    }
}
