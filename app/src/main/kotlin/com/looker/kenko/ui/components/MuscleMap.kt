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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.looker.kenko.R
import com.looker.kenko.data.model.MuscleGroups

/**
 * Which side of the body a zone lives on.
 */
private enum class Side { Front, Back }

/**
 * A muscle zone as a share of the drawing: x, y, width and height run 0..1.
 */
private data class Zone(
    val muscle: MuscleGroups,
    val side: Side,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rounded: Boolean = true,
) {
    fun rect(size: Size) = Rect(
        offset = Offset(left * size.width, top * size.height),
        size = Size(width * size.width, height * size.height),
    )
}

private val zones = listOf(
    // Front
    Zone(MuscleGroups.Traps, Side.Front, 0.36F, 0.115F, 0.28F, 0.045F),
    Zone(MuscleGroups.Shoulders, Side.Front, 0.20F, 0.16F, 0.15F, 0.09F),
    Zone(MuscleGroups.Shoulders, Side.Front, 0.65F, 0.16F, 0.15F, 0.09F),
    Zone(MuscleGroups.Chest, Side.Front, 0.30F, 0.17F, 0.40F, 0.11F),
    Zone(MuscleGroups.Biceps, Side.Front, 0.16F, 0.26F, 0.13F, 0.13F),
    Zone(MuscleGroups.Biceps, Side.Front, 0.71F, 0.26F, 0.13F, 0.13F),
    Zone(MuscleGroups.Core, Side.Front, 0.34F, 0.29F, 0.32F, 0.16F),
    Zone(MuscleGroups.Quads, Side.Front, 0.31F, 0.47F, 0.17F, 0.24F),
    Zone(MuscleGroups.Quads, Side.Front, 0.52F, 0.47F, 0.17F, 0.24F),
    Zone(MuscleGroups.Calves, Side.Front, 0.33F, 0.74F, 0.14F, 0.18F),
    Zone(MuscleGroups.Calves, Side.Front, 0.53F, 0.74F, 0.14F, 0.18F),
    // Back
    Zone(MuscleGroups.Traps, Side.Back, 0.33F, 0.115F, 0.34F, 0.075F),
    Zone(MuscleGroups.UpperBack, Side.Back, 0.30F, 0.20F, 0.40F, 0.10F),
    Zone(MuscleGroups.Triceps, Side.Back, 0.16F, 0.26F, 0.13F, 0.13F),
    Zone(MuscleGroups.Triceps, Side.Back, 0.71F, 0.26F, 0.13F, 0.13F),
    Zone(MuscleGroups.Lats, Side.Back, 0.28F, 0.30F, 0.44F, 0.12F),
    Zone(MuscleGroups.Glutes, Side.Back, 0.33F, 0.43F, 0.34F, 0.09F),
    Zone(MuscleGroups.Hamstrings, Side.Back, 0.31F, 0.53F, 0.17F, 0.20F),
    Zone(MuscleGroups.Hamstrings, Side.Back, 0.52F, 0.53F, 0.17F, 0.20F),
    Zone(MuscleGroups.Calves, Side.Back, 0.33F, 0.74F, 0.14F, 0.18F),
    Zone(MuscleGroups.Calves, Side.Back, 0.53F, 0.74F, 0.14F, 0.18F),
)

/**
 * Two bodies, front and back, where every muscle is a tappable block.
 *
 * The primary muscle is filled, the ones that help are outlined — no lists of chips.
 */
@Composable
fun MuscleMap(
    primary: MuscleGroups?,
    secondary: Set<MuscleGroups>,
    onZoneClick: (MuscleGroups) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Side.entries.forEach { side ->
            Column(
                modifier = Modifier.weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.5F)
                        .pointerInput(side) {
                            detectTapGestures { offset ->
                                zones
                                    .filter { it.side == side }
                                    .firstOrNull { it.rect(size.toSize()).contains(offset) }
                                    ?.let { onZoneClick(it.muscle) }
                            }
                        },
                ) {
                    drawBody(
                        outline = colors.outlineVariant,
                        fill = colors.surfaceContainerHigh,
                    )
                    zones
                        .filter { it.side == side }
                        .forEach { zone ->
                            val rect = zone.rect(size)
                            when (zone.muscle) {
                                primary -> drawZone(rect, colors.primary, filled = true)
                                in secondary -> drawZone(rect, colors.primary, filled = false)
                                else -> drawZone(rect, colors.outline, filled = false, faint = true)
                            }
                        }
                }
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = stringResource(
                        if (side == Side.Front) R.string.label_body_front else R.string.label_body_back,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.outline,
                )
            }
        }
    }
}

/**
 * The silhouette the zones sit on: head, torso, arms, legs, all built from blocks.
 */
private fun DrawScope.drawBody(outline: Color, fill: Color) {
    val stroke = Stroke(width = 1.4.dp.toPx())
    fun block(left: Float, top: Float, width: Float, height: Float, radius: Float = 0.06F) {
        val rect = Rect(
            offset = Offset(left * size.width, top * size.height),
            size = Size(width * size.width, height * size.height),
        )
        val corner = CornerRadius(radius * size.width)
        drawRoundRect(color = fill, topLeft = rect.topLeft, size = rect.size, cornerRadius = corner)
        drawRoundRect(
            color = outline,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = corner,
            style = stroke,
        )
    }
    // head
    drawCircle(
        color = fill,
        radius = 0.09F * size.width,
        center = Offset(0.5F * size.width, 0.07F * size.height),
    )
    drawCircle(
        color = outline,
        radius = 0.09F * size.width,
        center = Offset(0.5F * size.width, 0.07F * size.height),
        style = stroke,
    )
    block(0.28F, 0.11F, 0.44F, 0.35F, radius = 0.10F) // torso
    block(0.14F, 0.155F, 0.14F, 0.30F, radius = 0.07F) // left arm
    block(0.72F, 0.155F, 0.14F, 0.30F, radius = 0.07F) // right arm
    block(0.30F, 0.46F, 0.19F, 0.47F, radius = 0.09F) // left leg
    block(0.51F, 0.46F, 0.19F, 0.47F, radius = 0.09F) // right leg
}

private fun DrawScope.drawZone(
    rect: Rect,
    color: Color,
    filled: Boolean,
    faint: Boolean = false,
) {
    val corner = CornerRadius(0.05F * size.width)
    if (filled) {
        drawRoundRect(color = color, topLeft = rect.topLeft, size = rect.size, cornerRadius = corner)
    } else {
        drawRoundRect(
            color = if (faint) color.copy(alpha = 0.35F) else color,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = corner,
            style = Stroke(width = 1.4.dp.toPx()),
        )
    }
}
