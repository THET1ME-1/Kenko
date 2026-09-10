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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.theme.Numbers
import com.looker.kenko.ui.theme.numbers
import kotlin.math.roundToInt

/**
 * Plates a gym normally stocks, heaviest first — that is the order they go on the bar.
 */
val platePresets = listOf(25F, 20F, 15F, 10F, 5F, 2.5F, 1.25F)

private val BAR_HEIGHT = 116.dp

/**
 * Что сейчас навешано: сколько блинов каждого номинала.
 *
 * Считается ровно то, что человек повесил: тап по блину добавляет один блин, а не пару.
 * Штанга собирается симметрично, поэтому у набора есть переключатель [mirrored] — он
 * удваивает навеску, но по своей воле, а не молча.
 */
data class Loadout(
    val bar: Float = 20F,
    val plates: Map<Float, Int> = emptyMap(),
    val mirrored: Boolean = false,
) {
    /** Вес всех навешанных блинов, без удвоения. */
    val plateWeight: Float
        get() = plates.entries.sumOf { (it.key * it.value).toDouble() }.toFloat()

    /** Сколько железа висит с учётом того, зеркалится набор или нет. */
    val hangingWeight: Float get() = plateWeight * if (mirrored) 2 else 1

    val total: Float get() = bar + hangingWeight

    fun countOf(plate: Float): Int = plates[plate] ?: 0

    fun hang(plate: Float): Loadout = copy(plates = plates + (plate to countOf(plate) + 1))

    fun takeOff(plate: Float): Loadout {
        val count = countOf(plate)
        return when {
            count <= 0 -> this
            count == 1 -> copy(plates = plates - plate)
            else -> copy(plates = plates + (plate to count - 1))
        }
    }

    fun cleared(): Loadout = copy(plates = emptyMap())
}

/**
 * Splits a target weight into plates, heaviest first, the way a lifter loads a bar.
 *
 * Anything the plates cannot reach stays off: 47.6 on a 20 kg bar loads to 47.5.
 */
fun loadoutFor(target: Float, bar: Float = 20F, mirrored: Boolean = false): Loadout {
    val sides = if (mirrored) 2 else 1
    var rest = ((target - bar) / sides).coerceAtLeast(0F)
    val plates = buildMap {
        platePresets.forEach { plate ->
            val count = (rest / plate).toInt()
            if (count > 0) {
                put(plate, count)
                rest -= count * plate
            }
        }
    }
    return Loadout(bar = bar, plates = plates, mirrored = mirrored)
}

/**
 * The bar as it will look on the rack: plates in place, the number they add up to under it.
 *
 * Тап по блину вешает один блин, тап по счётчику снимает его. Симметричную навеску включает
 * отдельный переключатель — тогда под итогом стоит удвоенный вес железа.
 */
@Composable
fun PlateCalculator(
    loadout: Loadout,
    onLoadoutChange: (Loadout) -> Unit,
    onApply: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BarDrawing(loadout = loadout)

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatWeight(loadout.total),
                style = MaterialTheme.typography.displayMedium.numbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(R.string.label_kg),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
            text = stringResource(
                R.string.label_plates_sum,
                formatWeight(loadout.plateWeight),
                formatWeight(loadout.bar),
            ),
            style = MaterialTheme.typography.labelMedium.numbers(),
            color = MaterialTheme.colorScheme.outline,
        )

        BarPill(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
            text = stringResource(R.string.label_both_sides),
            selected = loadout.mirrored,
            onClick = { onLoadoutChange(loadout.copy(mirrored = !loadout.mirrored)) },
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            platePresets.forEach { plate ->
                PlateChip(
                    plate = plate,
                    count = loadout.countOf(plate),
                    onClick = { onLoadoutChange(loadout.hang(plate)) },
                    onRemove = { onLoadoutChange(loadout.takeOff(plate)) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.label_hang_plates),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.label_bar).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            barPresets.forEach { preset ->
                BarPill(
                    text = if (preset == 0F) {
                        stringResource(R.string.label_no_bar)
                    } else {
                        formatWeight(preset)
                    },
                    selected = loadout.bar == preset,
                    onClick = { onLoadoutChange(loadout.copy(bar = preset)) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedKey(
                modifier = Modifier.weight(1F),
                label = stringResource(R.string.action_clear_plates),
                onClick = { onLoadoutChange(loadout.cleared()) },
            )
            Button(
                modifier = Modifier
                    .weight(1.6F)
                    .height(56.dp),
                onClick = { onApply(loadout.total) },
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = stringResource(
                        R.string.action_use_weight,
                        formatWeight(loadout.total),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BarDrawing(loadout: Loadout, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val barLabel = stringResource(R.string.label_bar) + " " + formatWeight(loadout.bar)
    val measurer = rememberTextMeasurer()
    val plateStyle = remember {
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Numbers)
    }
    val barStyle = remember { TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Numbers) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerHighest),
    ) {
        drawBar(
            loadout = loadout,
            barLabel = barLabel,
            measurer = measurer,
            plateStyle = plateStyle.copy(color = colors.onPrimary),
            barStyle = barStyle.copy(color = colors.onSurfaceVariant),
            shaft = colors.outline,
            plate = colors.primary,
        )
    }
}

private fun DrawScope.drawBar(
    loadout: Loadout,
    barLabel: String,
    measurer: TextMeasurer,
    plateStyle: TextStyle,
    barStyle: TextStyle,
    shaft: Color,
    plate: Color,
) {
    val middle = size.height / 2
    val shaftHeight = 9.dp.toPx()
    drawRoundRect(
        color = shaft,
        topLeft = Offset(8.dp.toPx(), middle - shaftHeight / 2),
        size = Size(size.width - 16.dp.toPx(), shaftHeight),
        cornerRadius = CornerRadius(shaftHeight / 2),
    )

    if (loadout.bar > 0F) {
        // Подпись живёт под грифом: на самой полосе она сливается с ней.
        val label = measurer.measure(barLabel, barStyle)
        drawText(
            textLayoutResult = label,
            topLeft = Offset(
                size.width / 2 - label.size.width / 2,
                middle + shaftHeight / 2 + 6.dp.toPx(),
            ),
        )
    }

    // Тяжёлое ближе к грифу — так штангу и собирают.
    val stack = platePresets.flatMap { p -> List(loadout.countOf(p)) { p } }
    drawStack(stack, toRight = true, measurer = measurer, style = plateStyle, color = plate)
    // Вторая сторона рисуется, только когда навеска зеркальная: иначе картинка врала бы про вес.
    if (loadout.mirrored) {
        drawStack(stack, toRight = false, measurer = measurer, style = plateStyle, color = plate)
    }
}

private fun DrawScope.drawStack(
    plates: List<Float>,
    toRight: Boolean,
    measurer: TextMeasurer,
    style: TextStyle,
    color: Color,
) {
    val middle = size.height / 2
    val gap = 3.dp.toPx()
    val startX = size.width / 2 + (if (toRight) 1 else -1) * 48.dp.toPx()
    var x = startX
    plates.forEach { weight ->
        val plateHeight = plateHeightPx(weight)
        val plateWidth = when {
            weight >= 15F -> 20.dp.toPx()
            weight >= 5F -> 16.dp.toPx()
            else -> 12.dp.toPx()
        }
        val left = if (toRight) x else x - plateWidth
        if (left < 4.dp.toPx() || left + plateWidth > size.width - 4.dp.toPx()) return
        drawRoundRect(
            color = color,
            topLeft = Offset(left, middle - plateHeight / 2),
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(5.dp.toPx()),
        )
        if (weight >= 5F) {
            val label = measurer.measure(weight.roundToInt().toString(), style)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    left + plateWidth / 2 - label.size.width / 2,
                    middle - label.size.height / 2,
                ),
            )
        }
        x += (if (toRight) 1 else -1) * (plateWidth + gap)
    }
}

/**
 * Heavier plates are taller, the way they look on a rack — 25 kg reaches the full height.
 */
private fun DrawScope.plateHeightPx(weight: Float): Float {
    // Steps, not a straight ratio: 1.25 next to 25 would otherwise be a crumb.
    val ratio = when {
        weight >= 25F -> 1F
        weight >= 20F -> 0.9F
        weight >= 15F -> 0.8F
        weight >= 10F -> 0.68F
        weight >= 5F -> 0.54F
        weight >= 2.5F -> 0.42F
        else -> 0.32F
    }
    return (size.height - 14.dp.toPx()) * ratio
}

@Composable
private fun PlateChip(
    plate: Float,
    count: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val hanging = count > 0
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (hanging) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = if (hanging) 8.dp else 14.dp, top = 11.dp, bottom = 11.dp),
            text = formatWeight(plate).removeSuffix(".0"),
            style = MaterialTheme.typography.labelLarge.numbers(),
            color = if (hanging) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (hanging) {
            // Снять блин: своя зона в палец шириной, иначе тап всегда попадал в «повесить».
            Text(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                text = "×$count −",
                style = MaterialTheme.typography.labelMedium.numbers(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BarPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.numbers(),
        textAlign = TextAlign.Center,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/**
 * A key with only an outline: it stands next to the filled one without competing with it.
 */
@Composable
fun OutlinedKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraLarge,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
