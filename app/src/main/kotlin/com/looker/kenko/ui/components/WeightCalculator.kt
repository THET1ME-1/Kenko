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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.theme.numbers

/**
 * Bars a gym usually has. The last slot is whatever the lifter types in.
 */
val barPresets = listOf(0F, 7.5F, 10F, 15F, 20F, 25F)

/**
 * Adds up what is actually on the bar: the bar itself plus each side.
 *
 * Sides are separate on purpose — dumbbells, machines and loading pins rarely match.
 */
@Composable
fun WeightCalculator(
    bar: Float,
    left: Float,
    right: Float,
    onBarChange: (Float) -> Unit,
    onLeftChange: (Float) -> Unit,
    onRightChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mirrored by remember { mutableStateOf(left == right) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(OnSurfaceVariantBorder, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.label_weight_total).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "${formatWeight(bar + left + right)} ${stringResource(R.string.label_kg)}",
            style = MaterialTheme.typography.displaySmall.numbers(),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.label_bar).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            barPresets.forEach { preset ->
                WeightPill(
                    text = if (preset == 0F) {
                        stringResource(R.string.label_no_bar)
                    } else {
                        formatWeight(preset)
                    },
                    selected = bar == preset,
                    onClick = { onBarChange(preset) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        NumberField(
            label = stringResource(R.string.label_own_bar),
            value = bar,
            onValueChange = onBarChange,
        )

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                modifier = Modifier.weight(1F),
                label = stringResource(R.string.label_side_left),
                value = left,
                onValueChange = { value ->
                    onLeftChange(value)
                    if (mirrored) onRightChange(value)
                },
            )
            NumberField(
                modifier = Modifier.weight(1F),
                label = stringResource(R.string.label_side_right),
                value = right,
                onValueChange = { value ->
                    onRightChange(value)
                    if (mirrored) onLeftChange(value)
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = mirrored,
                onCheckedChange = { checked ->
                    mirrored = checked
                    if (checked) onRightChange(left)
                },
            )
            Text(
                text = stringResource(R.string.label_same_on_both_sides),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(formatWeight(value)) }
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = text,
        onValueChange = { input ->
            text = input.replace(',', '.')
            text.toFloatOrNull()?.let(onValueChange)
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = kenkoTextFieldColor(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        label = { Text(text = label) },
        textStyle = MaterialTheme.typography.titleMedium.numbers(),
    )
}

@Composable
private fun WeightPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
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
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
