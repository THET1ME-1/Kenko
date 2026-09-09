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

package com.looker.kenko.ui.sessionDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.ExercisesPreviewParameter
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.repDurationStringRes
import com.looker.kenko.ui.addSet.setTypeLabel
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.KenkoThemeConfig
import com.looker.kenko.ui.theme.KenkoThemePreviewParameter
import com.looker.kenko.ui.theme.numbers

@Composable
fun SetItem(
    set: Set,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(64.dp)
            .widthIn(240.dp, 420.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.outline,
            LocalTextStyle provides MaterialTheme.typography.displayMedium.numbers(),
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                title()
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PerformedItem(
                title = stringResource(set.exercise.repDurationStringRes),
                performance = "${set.repsOrDuration}",
            )
            PerformedItem(
                title = stringResource(R.string.label_weight),
                performance = "${set.weight} KG",
            )
        }
    }
}

@Composable
fun SuggestedSetItem(
    modifier: Modifier = Modifier,
    repCount: Int = 10,
    weight: Float = 100F,
    type: SetType = SetType.Standard,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    onAccept: () -> Unit = {},
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Surface(
        color = color,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            val typeLabel = remember(type) { setTypeLabel(type) }
            Text(
                text = typeLabel,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            val spanStyle1 = textStyle
                .copy(fontWeight = FontWeight.Bold)
                .toSpanStyle()
            val spanStyle2 = textStyle
                .copy(color = MaterialTheme.colorScheme.onSurface)
                .toSpanStyle()

            val annotatedString = remember {
                buildAnnotatedString {
                    withStyle(spanStyle1) {
                        append(repCount.toString())
                    }
                    withStyle(spanStyle2) {
                        append("@")
                    }
                    withStyle(spanStyle1) {
                        append(weight.toString())
                    }
                    withStyle(spanStyle2) {
                        append("KG")
                    }
                }
            }
            Text(annotatedString)
            Spacer(modifier = Modifier.weight(1F))
            FilledIconButton(onClick = onAccept) {
                Icon(
                    painter = KenkoIcons.Done,
                    contentDescription = null,
                )
            }
        }
    }
}

private val DropIndent = 64.dp

/**
 * One weight cut of a drop set, tucked under the set it belongs to.
 */
@Composable
fun DropRow(
    drop: Set,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .widthIn(240.dp, 420.dp)
            .padding(start = DropIndent, end = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.label_drop_number, drop.dropIndex).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "${drop.repsOrDuration} × ${drop.weight} KG",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Invitation to cut the weight once more, shown under a drop set while the session is open.
 */
@Composable
fun AddDropRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .widthIn(240.dp, 420.dp)
            .padding(start = DropIndent, end = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.label_add_drop).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Icon(
            painter = KenkoIcons.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun PerformedItem(
    title: String,
    performance: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = performance,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun SetItemPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    val exercise = ExercisesPreviewParameter().values.first().first()
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        SetItem(
            Set(12, 40F, SetType.Drop, exercise, RepsInReserve(2)),
        ) {
            Text(text = "01")
        }
    }
}

@Preview
@Composable
private fun SuggestionPreview(
    @PreviewParameter(KenkoThemePreviewParameter::class) config: KenkoThemeConfig,
) {
    KenkoTheme(colorSchemes = config.colorSchemes, theme = config.theme) {
        SuggestedSetItem()
    }
}
