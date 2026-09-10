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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.data.model.WEIGHT_STEP
import com.looker.kenko.data.model.roundToStep
import com.looker.kenko.ui.theme.Numbers
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.roundToInt

private val RULER_HEIGHT = 84.dp
private val STEP_WIDTH = 16.dp

/**
 * Every fourth tick is a tall one, which lands on a whole five kilograms.
 */
private const val TICKS_PER_LABEL = 4

private const val MAX_WEIGHT = 500F

/**
 * Weight picked by dragging a ruler: the scale moves under the finger and clicks onto
 * loadable steps of 1.25 kg.
 *
 * The number itself lives above the ruler — here only the scale, so the eye has one place
 * to read from and one place to pull.
 *
 * Дробный вес, пришедший со стороны (прошлый подход, поправка зала), первым же движением
 * встаёт на сетку: иначе риска показывает 75, а в подходе пишется 75.4.
 */
@Composable
fun WeightRuler(
    weight: Float,
    onWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val stepPx = with(density) { STEP_WIDTH.toPx() }

    var carried by remember { mutableFloatStateOf(0F) }

    val labelStyle = remember {
        TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Numbers)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RULER_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainer)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    carried -= delta
                    val steps = (carried / stepPx).let { if (it > 0) it.toInt() else -((-it).toInt()) }
                    if (steps != 0) {
                        carried -= steps * stepPx
                        val base = roundToStep(weight)
                        val next = (base + steps * WEIGHT_STEP).coerceIn(0F, MAX_WEIGHT)
                        if (next != weight) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            onWeightChange(next)
                        }
                    }
                },
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(RULER_HEIGHT)) {
            drawRuler(
                weight = weight,
                stepPx = stepPx,
                measurer = measurer,
                labelStyle = labelStyle.copy(color = colors.outline),
                tick = colors.outlineVariant,
                majorTick = colors.outline,
                cursor = colors.primary,
                halo = colors.primaryContainer,
                edge = colors.surfaceContainer,
            )
        }
    }
}

private fun DrawScope.drawRuler(
    weight: Float,
    stepPx: Float,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    tick: Color,
    majorTick: Color,
    cursor: Color,
    halo: Color,
    edge: Color,
) {
    val center = size.width / 2
    val visibleSteps = (size.width / stepPx / 2).toInt() + 2
    val currentStep = (weight / WEIGHT_STEP).roundToInt()
    // Насколько вес не дотягивает до своей риски: шкала сдвигается на эту долю, чтобы
    // курсор не врал про ровное число.
    val drift = (weight / WEIGHT_STEP - currentStep) * stepPx
    val baseline = size.height - 20.dp.toPx()

    // Подсветка под текущим значением: глазу нужно место, к которому он возвращается.
    drawRoundRect(
        color = halo,
        topLeft = Offset(center - stepPx * 1.4F, 6.dp.toPx()),
        size = Size(stepPx * 2.8F, size.height - 12.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
    )

    for (offset in -visibleSteps..visibleSteps) {
        val step = currentStep + offset
        if (step < 0) continue
        val x = center + offset * stepPx - drift
        val isMajor = step % TICKS_PER_LABEL == 0
        val height = if (isMajor) 34.dp.toPx() else 18.dp.toPx()
        drawLine(
            color = if (isMajor) majorTick else tick,
            start = Offset(x, baseline - height),
            end = Offset(x, baseline),
            strokeWidth = KenkoBorderWidth.toPx(),
        )
        if (isMajor) {
            val label = (step * WEIGHT_STEP).roundToInt().toString()
            val measured = measurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x - measured.size.width / 2, baseline + 2.dp.toPx()),
            )
        }
    }

    // Края растворяются, чтобы риски не обрубались стенкой.
    val fade = 24.dp.toPx()
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colors = listOf(edge, Color.Transparent),
            startX = 0F,
            endX = fade,
        ),
        size = Size(fade, size.height),
    )
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colors = listOf(Color.Transparent, edge),
            startX = size.width - fade,
            endX = size.width,
        ),
        topLeft = Offset(size.width - fade, 0F),
        size = Size(fade, size.height),
    )

    drawLine(
        color = cursor,
        start = Offset(center, 10.dp.toPx()),
        end = Offset(center, size.height - 10.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
    )
}
