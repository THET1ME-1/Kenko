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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.SessionBlock
import com.looker.kenko.data.model.SetChain
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.data.model.oneRepMax
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.SetTick
import com.looker.kenko.ui.components.TickState
import com.looker.kenko.ui.exercises.displayName
import com.looker.kenko.ui.theme.numbers
import kotlin.math.roundToInt

private const val DONE_ALPHA = 0.6F
private const val AHEAD_ALPHA = 0.45F

/**
 * A superset open for work: one card per round, and the round closes as a whole.
 *
 * Rest starts only after the last exercise of a round, which is what makes it a superset.
 */
@Composable
fun SupersetCard(
    block: SessionBlock.Superset,
    isEditable: Boolean,
    onCollapse: () -> Unit,
    onCloseRound: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * A plan shows the rounds ahead: nothing to close, nothing to tick.
     */
    isPlan: Boolean = false,
) {
    val plannedRounds = block.plannedRounds
    val closed = block.closedRounds
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
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                block.exercises.forEachIndexed { index, _ ->
                    LegBadge(index = index)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(R.string.label_superset_no_rest),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = block.exercises
                        .mapIndexed { index, exercise -> "${legLetter(index)} ${exercise.displayName()}" }
                        .joinToString(" → "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Badge(text = stringResource(R.string.label_round_of, closed, plannedRounds))
        }
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            repeat(plannedRounds) { index ->
                RoundCard(block = block, roundIndex = index, isPlan = isPlan)
            }
        }
        if (isEditable && !isPlan) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier.weight(1F),
                    onClick = onCloseRound,
                    enabled = closed < plannedRounds,
                ) {
                    Text(
                        text = if (closed >= plannedRounds) {
                            stringResource(R.string.label_superset_closed)
                        } else {
                            stringResource(R.string.label_close_round, closed + 1)
                        },
                    )
                }
                TextButton(onClick = onUndo) {
                    Text(text = stringResource(R.string.label_undo))
                }
            }
        }
        Text(
            text = stringResource(R.string.label_superset_hint, block.roundVolume.roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 15.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun RoundCard(
    block: SessionBlock.Superset,
    roundIndex: Int,
    modifier: Modifier = Modifier,
    isPlan: Boolean = false,
) {
    val round = block.rounds.firstOrNull { it.index == roundIndex }
    val performed = round?.chains.orEmpty()
    val isClosed = performed.size >= block.exercises.size && block.exercises.isNotEmpty()
    val isRunning = performed.isNotEmpty() && !isClosed
    val alpha = when {
        isPlan -> 1F
        isClosed -> DONE_ALPHA
        isRunning -> 1F
        else -> AHEAD_ALPHA
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 11.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isRunning) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .alpha(alpha),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_round_short, roundIndex + 1),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(9.dp))
            Box(
                modifier = Modifier
                    .weight(1F)
                    .height(KenkoBorderWidth)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(
                    when {
                        isPlan -> R.string.label_round_planned
                        isClosed -> R.string.label_round_closed
                        isRunning -> R.string.label_round_running
                        else -> R.string.label_round_planned
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(8.dp))
        block.exercises.forEachIndexed { index, exercise ->
            val chain = performed.firstOrNull { it.set.exercise == exercise }
            SupersetLegRow(
                index = index,
                exercise = exercise,
                chain = chain,
                plannedReps = block.plan.firstOrNull { it.exercise == exercise }?.targetReps,
                isNext = !isPlan && chain == null &&
                    (isRunning || isClosed.not() && roundIndex == block.closedRounds),
                showTick = !isPlan,
            )
        }
    }
}

@Composable
private fun SupersetLegRow(
    index: Int,
    exercise: Exercise,
    chain: SetChain?,
    plannedReps: Int?,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    showTick: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegBadge(index = index, dim = chain == null && !isNext)
        Spacer(Modifier.width(9.dp))
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val kg = stringResource(R.string.label_kg)
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = exercise.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = buildString {
                        append(chain?.set?.repsOrDuration?.toString() ?: plannedReps?.toString() ?: "—")
                        append(" × ")
                        append(chain?.set?.weight?.let { formatWeight(it) } ?: "—")
                        append(" $kg")
                    },
                    style = MaterialTheme.typography.titleMedium.numbers(),
                )
            }
            if (chain != null) {
                Text(
                    text = stringResource(
                        R.string.label_one_rep_max_short,
                        formatWeight(oneRepMax(chain.set.weight, chain.set.repsOrDuration)),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showTick) {
            Spacer(Modifier.width(9.dp))
            SetTick(
                state = when {
                    chain != null -> TickState.Done
                    isNext -> TickState.Active
                    else -> TickState.Ahead
                },
            )
        }
    }
}

/**
 * A superset folded into one line: who is in it, how much it weighs, how many rounds are closed.
 */
@Composable
fun SupersetRow(
    block: SessionBlock.Superset,
    number: Int,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    isPlan: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = com.looker.kenko.ui.extensions.normalizeInt(number),
            style = MaterialTheme.typography.headlineSmall.numbers(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp),
        )
        Column(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onExpand)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                block.exercises.forEachIndexed { index, _ ->
                    LegBadge(index = index, size = 18.dp)
                    Spacer(Modifier.width(3.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_round_of, block.closedRounds, block.plannedRounds),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1F),
                )
            }
            Spacer(Modifier.height(7.dp))
            val legs = block.exercises.mapIndexed { index, exercise ->
                val last = block.rounds
                    .flatMap { it.chains }
                    .lastOrNull { it.set.exercise == exercise }
                    ?.set
                val reps = last?.repsOrDuration
                    ?: block.plan.firstOrNull { it.exercise == exercise }?.targetReps
                val weight = last?.weight?.let { formatWeight(it) } ?: "—"
                "${legLetter(index)} ${exercise.displayName()} ${reps ?: "—"}×$weight"
            }
            Text(
                text = legs.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (!isPlan) {
            Spacer(Modifier.width(11.dp))
            SetTick(
                state = if (block.roundsLeft == 0) TickState.Done else TickState.Ahead,
                size = 22.dp,
            )
        }
    }
}

/**
 * The A / B mark that ties a row to its place in the superset.
 */
@Composable
private fun LegBadge(
    index: Int,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    size: Dp = 22.dp,
) {
    val colors = MaterialTheme.colorScheme
    val background = when (index % 4) {
        0 -> colors.primary
        1 -> colors.error
        2 -> colors.secondary
        else -> colors.tertiary
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .alpha(if (dim) AHEAD_ALPHA else 1F),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = legLetter(index),
            style = MaterialTheme.typography.labelSmall,
            color = colors.surface,
        )
    }
}

private fun legLetter(index: Int): String = ('A' + index).toString()
