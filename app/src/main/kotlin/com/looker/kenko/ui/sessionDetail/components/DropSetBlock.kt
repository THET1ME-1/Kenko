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

package com.looker.kenko.ui.sessionDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.model.DROP_PERCENT_STEP
import com.looker.kenko.data.model.DropStep
import com.looker.kenko.data.model.MAX_DROP_COUNT
import com.looker.kenko.data.model.MAX_DROP_PERCENT
import com.looker.kenko.data.model.MIN_DROP_COUNT
import com.looker.kenko.data.model.MIN_DROP_PERCENT
import com.looker.kenko.data.model.SetChain
import com.looker.kenko.data.model.chainLabel
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.MiniStepper
import com.looker.kenko.ui.components.SetTick
import com.looker.kenko.ui.components.TickState
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.theme.numbers
import kotlin.math.roundToInt

private const val DONE_ALPHA = 0.6F
private const val AHEAD_ALPHA = 0.45F

/**
 * A drop set open for work: settings on top, one row per cut, one button to close the group.
 *
 * The lifter types reps; every weight under the working set is computed.
 */
@Composable
fun DropSetCard(
    chain: SetChain,
    number: Int,
    isEditable: Boolean,
    onCollapse: () -> Unit,
    onDropsChange: (Int) -> Unit,
    onPercentChange: (Int) -> Unit,
    onMarkStep: (Int) -> Unit,
    onMarkGroup: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    onEditStep: (Int) -> Unit = {},
    /**
     * In a plan there is nothing to tick off yet: the group only shows what is coming.
     */
    isPlan: Boolean = false,
) {
    val steps = chain.steps
    val performed = chain.performedSteps
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(KenkoBorderWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onCollapse)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = normalizeInt(number),
                style = MaterialTheme.typography.headlineSmall.numbers(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(
                        R.string.label_drop_group,
                        pluralStringResource(R.plurals.plural_drops, chain.set.dropCount, chain.set.dropCount),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (isPlan) {
                        stringResource(
                            R.string.label_drop_group_planned,
                            pluralStringResource(R.plurals.plural_sets, steps.size, steps.size),
                        )
                    } else {
                        stringResource(
                            R.string.label_drop_group_progress,
                            performed,
                            steps.size,
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Badge(text = "−${chain.set.dropPercent}%")
        }
        if (isEditable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniStepper(
                    modifier = Modifier.weight(1F),
                    caption = stringResource(R.string.label_drops_caption),
                    value = chain.set.dropCount.toString(),
                    onMinus = { onDropsChange((chain.set.dropCount - 1).coerceAtLeast(MIN_DROP_COUNT)) },
                    onPlus = { onDropsChange((chain.set.dropCount + 1).coerceAtMost(MAX_DROP_COUNT)) },
                )
                MiniStepper(
                    modifier = Modifier.weight(1F),
                    caption = stringResource(R.string.label_drop_step),
                    value = "−${chain.set.dropPercent}%",
                    onMinus = {
                        onPercentChange(
                            (chain.set.dropPercent - DROP_PERCENT_STEP).coerceAtLeast(MIN_DROP_PERCENT),
                        )
                    },
                    onPlus = {
                        onPercentChange(
                            (chain.set.dropPercent + DROP_PERCENT_STEP).coerceAtMost(MAX_DROP_PERCENT),
                        )
                    },
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp),
        ) {
            steps.forEach { step ->
                DropStepRow(
                    step = step,
                    number = number,
                    isNext = !isPlan && !step.isPerformed && step.index == performed,
                    onTick = { onMarkStep(step.index) }.takeIf { isEditable && !isPlan },
                    onEdit = { onEditStep(step.index) }
                        .takeIf { isEditable && !isPlan && !step.isPerformed && step.index > 0 },
                    showTick = !isPlan,
                )
            }
        }
        if (isEditable && !isPlan) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier.weight(1F),
                    onClick = onMarkGroup,
                    enabled = performed <= chain.set.dropCount,
                ) {
                    Text(
                        text = stringResource(
                            if (performed > chain.set.dropCount) {
                                R.string.label_group_closed
                            } else {
                                R.string.label_mark_whole_group
                            },
                        ),
                    )
                }
                TextButton(onClick = onUndo) {
                    Text(text = stringResource(R.string.label_undo))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.label_drop_hint_editable, steps.chainLabel()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.label_group_volume, chain.volume.roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DropStepRow(
    step: DropStep,
    number: Int,
    isNext: Boolean,
    onTick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    showTick: Boolean = true,
) {
    val alpha = when {
        !showTick -> 1F
        step.isPerformed -> DONE_ALPHA
        isNext -> 1F
        else -> AHEAD_ALPHA
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(56.dp)) {
            Text(
                text = if (step.index == 0) {
                    normalizeInt(number)
                } else {
                    "${normalizeInt(number)}.${step.index}"
                },
                style = MaterialTheme.typography.titleMedium.numbers(),
            )
            Text(
                text = if (step.index == 0) {
                    stringResource(R.string.label_drop_base)
                } else {
                    stringResource(R.string.label_drop_index, step.index)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Row(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isNext) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            FieldValue(
                caption = stringResource(R.string.label_field_reps),
                value = if (step.isPerformed || isNext) step.reps.toString() else "—",
            )
            FieldValue(
                caption = stringResource(
                    if (step.isAutoWeight) {
                        R.string.label_field_weight_auto
                    } else {
                        R.string.label_field_weight
                    },
                ),
                value = "${formatWeight(step.weight)} ${stringResource(R.string.label_kg)}",
                alignEnd = true,
            )
        }
        if (showTick) {
            Spacer(Modifier.width(11.dp))
            SetTick(
                state = when {
                    step.isPerformed -> TickState.Done
                    isNext -> TickState.Active
                    else -> TickState.Ahead
                },
                onClick = onTick,
            )
        }
    }
}

/**
 * A drop set folded into one line: the chain of weights and the bars that show it falling.
 */
@Composable
fun DropSetRow(
    chain: SetChain,
    number: Int,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    isPlan: Boolean = false,
) {
    val steps = chain.steps
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = normalizeInt(number),
            style = MaterialTheme.typography.headlineSmall.numbers(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp),
        )
        Row(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onExpand)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1F)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.label_drop_short, chain.set.dropCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.label_volume_kg, chain.volume.roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = "${steps.chainLabel()} ${stringResource(R.string.label_kg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            DropBars(steps = steps)
        }
        if (!isPlan) {
            Spacer(Modifier.width(11.dp))
            SetTick(state = TickState.Done, size = 22.dp)
        }
    }
}

/**
 * Falling bars, one per step, so the shape of the drop reads without numbers.
 */
@Composable
private fun DropBars(steps: List<DropStep>) {
    val top = steps.firstOrNull()?.weight ?: return
    Row(verticalAlignment = Alignment.Bottom) {
        steps.forEach { step ->
            val share = if (top == 0F) 0F else step.weight / top
            Box(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .width(5.dp)
                    .height((share * 22).coerceIn(5F, 22F).dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun FieldValue(
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.numbers(),
        )
    }
}

@Composable
internal fun Badge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.numbers(),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
