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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A dashed outline that reads as an empty slot waiting to be filled: `+ exercise`.
 */
@Composable
fun DashedAddButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val color = if (accent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color = color,
                    style = Stroke(
                        width = KenkoBorderWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(DASH_LENGTH, DASH_GAP),
                        ),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

private const val DASH_LENGTH = 10F
private const val DASH_GAP = 8F

private fun DrawScope.drawOutline(
    outline: Outline,
    color: Color,
    style: Stroke,
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color = color, style = style)

        is Outline.Rounded -> drawPath(
            path = Path().apply { addRoundRect(outline.roundRect) },
            color = color,
            style = style,
        )

        is Outline.Generic -> drawPath(path = outline.path, color = color, style = style)
    }
}
