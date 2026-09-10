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

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.ui.components.body.BODY_BACK_OFFSET
import com.looker.kenko.ui.components.body.BODY_VIEW_HEIGHT
import com.looker.kenko.ui.components.body.BODY_VIEW_WIDTH
import com.looker.kenko.ui.components.body.BodyRegion
import com.looker.kenko.ui.components.body.bodyBack
import com.looker.kenko.ui.components.body.bodyFront
import com.looker.kenko.ui.components.body.bodySilhouetteBack
import com.looker.kenko.ui.components.body.bodySilhouetteFront

/**
 * Which way the body is turned. Some muscles only show on one of the two.
 */
enum class BodySide { Front, Back }

/**
 * Muscles that only have a shape on one side, for legends and pickers.
 */
val MuscleGroups.bodySide: BodySide
    get() = when (this) {
        MuscleGroups.Lats,
        MuscleGroups.UpperBack,
        MuscleGroups.Triceps,
        MuscleGroups.Glutes,
        MuscleGroups.Hamstrings,
        -> BodySide.Back

        else -> BodySide.Front
    }

/**
 * One region ready to draw: the parsed outline plus a mask for hit testing.
 */
private class DrawnRegion(
    val region: BodyRegion,
    val path: Path,
)

/**
 * Both sides of the body side by side, every muscle shaded by how much work it took.
 *
 * [load] is `0..1` per muscle, already normalised by the screen — the map only paints.
 */
@Composable
fun BodyHeatMap(
    load: Map<MuscleGroups, Float>,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    selected: MuscleGroups? = null,
    onMuscleClick: ((MuscleGroups) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val colors = MaterialTheme.colorScheme
        BodySide.entries.forEach { side ->
            BodyFigure(
                side = side,
                fillOf = { muscle ->
                    heatOf(
                        intensity = load[muscle] ?: 0F,
                        cold = colors.surfaceVariant,
                        warm = colors.primaryContainer,
                        hot = colors.primary,
                        peak = colors.error,
                    )
                },
                selected = selected,
                onMuscleClick = onMuscleClick,
                modifier = Modifier.weight(1F),
            )
        }
    }
}

/**
 * The body as a picker: the muscle an exercise trains is filled with the accent, the ones that
 * help are tinted, everything else waits. Tapping a muscle hands it back.
 */
@Composable
fun BodyPicker(
    primary: MuscleGroups?,
    secondary: Set<MuscleGroups>,
    onMuscleClick: (MuscleGroups) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 420.dp,
) {
    val colors = MaterialTheme.colorScheme
    // One figure at full width instead of two halves: on a phone the small ones were
    // impossible to hit — a muscle two millimetres wide is not a target.
    var side by rememberSaveable(primary) { mutableStateOf(primary?.bodySide ?: BodySide.Front) }
    // Pinch brings the small muscles within reach of a finger; the figure never goes below life
    // size, and turning the body around puts it back.
    var scale by remember(side) { mutableFloatStateOf(1F) }
    var pan by remember(side) { mutableStateOf(Offset.Zero) }
    val zoom = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_BODY_ZOOM, MAX_BODY_ZOOM)
        pan = if (scale <= MIN_BODY_ZOOM) Offset.Zero else pan + panChange
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SideSwitch(side = side, onSideChange = { side = it })
        BodyFigure(
            side = side,
            fillOf = { muscle ->
                when (muscle) {
                    primary -> colors.primary
                    in secondary -> colors.primaryContainer
                    else -> colors.surfaceVariant
                }
            },
            selected = primary,
            onMuscleClick = onMuscleClick,
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                }
                .transformable(zoom),
        )
    }
}

/**
 * Life size and the closest the body comes.
 */
private const val MIN_BODY_ZOOM = 1F
private const val MAX_BODY_ZOOM = 3F

/**
 * Front or back: the muscles of the hidden side keep their colour and come back with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SideSwitch(
    side: BodySide,
    onSideChange: (BodySide) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        BodySide.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == side,
                onClick = { onSideChange(entry) },
                shape = segmentShape(index, BodySide.entries.size),
                colors = kenkoSegmentColors,
            ) {
                Text(
                    text = stringResource(
                        when (entry) {
                            BodySide.Front -> R.string.label_body_front
                            BodySide.Back -> R.string.label_body_back
                        },
                    ),
                )
            }
        }
    }
}

/**
 * One muscle on a small figure, the way a list row needs it: the side it shows on, the body
 * quiet, the muscle itself in colour.
 */
@Composable
fun MuscleIcon(
    muscle: MuscleGroups,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.error,
    body: Color = MaterialTheme.colorScheme.surfaceBright,
) {
    val quiet = body
    Box(modifier = modifier.height(height)) {
        BodyFigure(
            side = muscle.bodySide,
            fillOf = { if (it == muscle) tint else quiet },
            selected = null,
            onMuscleClick = null,
            outlines = false,
            modifier = Modifier
                .height(height)
                .width(height * BODY_ICON_RATIO),
        )
    }
}

/**
 * A figure is about twice as tall as it is wide.
 */
private const val BODY_ICON_RATIO = 0.62F

@Composable
private fun BodyFigure(
    side: BodySide,
    fillOf: (MuscleGroups?) -> Color,
    selected: MuscleGroups?,
    onMuscleClick: ((MuscleGroups) -> Unit)?,
    modifier: Modifier = Modifier,
    outlines: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val neutral = if (outlines) colors.surfaceContainerHighest else fillOf(null)
    val outline = if (outlines) colors.outline else colors.outlineVariant
    val ring = colors.onSurface

    val regions = remember(side) {
        val source = if (side == BodySide.Front) bodyFront else bodyBack
        source.map { region ->
            DrawnRegion(
                region = region,
                path = PathParser().parsePathString(region.path).toPath(),
            )
        }
    }
    val silhouette = remember(side) {
        val data = if (side == BodySide.Front) bodySilhouetteFront else bodySilhouetteBack
        PathParser().parsePathString(data).toPath()
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (onMuscleClick == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(side, regions) {
                        detectTapGestures { offset ->
                            val scale = size.height / BODY_VIEW_HEIGHT
                            val inset = (size.width - BODY_VIEW_WIDTH * scale) / 2
                            val shift = if (side == BodySide.Front) 0F else BODY_BACK_OFFSET
                            val x = (offset.x - inset) / scale + shift
                            val y = offset.y / scale
                            regions.hit(x, y)?.let(onMuscleClick)
                        }
                    }
                },
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.height / BODY_VIEW_HEIGHT
            val inset = (size.width - BODY_VIEW_WIDTH * scale) / 2
            val shift = if (side == BodySide.Front) 0F else BODY_BACK_OFFSET
            translate(left = inset) {
                scale(scale = scale, pivot = Offset.Zero) {
                    translate(left = -shift) {
                        // Силуэт, потом тело, потом мышцы: тепло читается на цельной фигуре.
                        drawPath(path = silhouette, color = neutral)
                        drawPath(
                            path = silhouette,
                            color = outline,
                            style = Stroke(width = OUTLINE_WIDTH * 1.6F),
                        )
                        regions.filter { it.region.muscle == null }.forEach { drawn ->
                            drawPath(path = drawn.path, color = neutral)
                            if (outlines) {
                                drawPath(
                                    path = drawn.path,
                                    color = outline,
                                    style = Stroke(width = OUTLINE_WIDTH),
                                )
                            }
                        }
                        regions.filter { it.region.muscle != null }.forEach { drawn ->
                            val muscle = drawn.region.muscle ?: return@forEach
                            drawPath(path = drawn.path, color = fillOf(muscle))
                            if (outlines || muscle == selected) {
                                drawPath(
                                    path = drawn.path,
                                    color = if (muscle == selected) ring else outline,
                                    style = Stroke(
                                        width = if (muscle == selected) {
                                            OUTLINE_WIDTH * 2.5F
                                        } else {
                                            OUTLINE_WIDTH
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stroke inside the drawing's own coordinates: the whole body is 35 units wide.
 */
private const val OUTLINE_WIDTH = 1.8F

private fun heatOf(
    intensity: Float,
    cold: Color,
    warm: Color,
    hot: Color,
    peak: Color,
): Color = when {
    intensity <= 0F -> cold
    intensity < 0.5F -> lerp(warm, hot, intensity / 0.5F)
    else -> lerp(hot, peak, (intensity - 0.5F) / 0.5F)
}

/**
 * Which muscle sits under the finger. Regions overlap, so the smallest match wins — a biceps
 * inside an arm should not be swallowed by the arm.
 */
private fun List<DrawnRegion>.hit(x: Float, y: Float): MuscleGroups? =
    filter { it.region.muscle != null }
        .filter { drawn ->
            val bounds = drawn.path.getBounds()
            x >= bounds.left - 0.5F && x <= bounds.right + 0.5F &&
                y >= bounds.top - 0.5F && y <= bounds.bottom + 0.5F &&
                drawn.path.containsPoint(x, y)
        }
        .minByOrNull { drawn ->
            drawn.path.getBounds().let { bounds -> bounds.width * bounds.height }
        }
        ?.region
        ?.muscle

/**
 * Compose has no point test of its own, so the shape goes through a region mask.
 *
 * The mask is built ten times finer than the drawing: a muscle two units wide would otherwise
 * round away to nothing.
 */
private fun Path.containsPoint(x: Float, y: Float): Boolean {
    val platform = AndroidPath(asAndroidPath())
    platform.transform(Matrix().apply { setScale(MASK_SCALE, MASK_SCALE) })
    val rect = RectF()
    platform.computeBounds(rect, true)
    val clip = Rect(
        rect.left.toInt() - 1,
        rect.top.toInt() - 1,
        rect.right.toInt() + 1,
        rect.bottom.toInt() + 1,
    )
    val region = Region()
    region.setPath(platform, Region(clip))
    return region.contains((x * MASK_SCALE).toInt(), (y * MASK_SCALE).toInt())
}

private const val MASK_SCALE = 10F

/**
 * Heat swatch for legends and list rows, so the colour on the body and the colour next to the
 * number always come from the same place.
 */
@Composable
fun heatColor(intensity: Float): Color {
    val colors = MaterialTheme.colorScheme
    return heatOf(
        intensity = intensity,
        cold = colors.surfaceVariant,
        warm = colors.primaryContainer,
        hot = colors.primary,
        peak = colors.error,
    )
}
