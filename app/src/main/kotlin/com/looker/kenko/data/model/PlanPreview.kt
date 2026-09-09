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

package com.looker.kenko.data.model

import com.looker.kenko.data.local.model.SetType

/**
 * Turns a planned exercise into the same shape a performed set has.
 *
 * That way the plan editor draws exactly what the session will look like — same rows,
 * same drop groups, same superset rounds — only without ticks and with the starting weight.
 */
fun PlanItem.previewSet(): Set = Set(
    repsOrDuration = targetReps,
    weight = targetWeight,
    type = if (dropCount > 0) SetType.Drop else SetType.Standard,
    exercise = exercise,
    rir = RepsInReserve(2),
    dropCount = dropCount,
    dropPercent = dropPercent,
)

fun PlanItem.previewChain(): SetChain = SetChain(previewSet())

/**
 * The day as the session will read it: one block per exercise, supersets kept together.
 */
fun List<PlanItem>.toPreviewBlocks(): List<SessionBlock> = toDayGroups().map { group ->
    when (group) {
        is PlanDayGroup.Single -> SessionBlock.SingleExercise(
            exercise = group.item.exercise,
            chains = List(group.item.targetSets) { group.item.previewChain() },
            plan = group.item,
        )

        is PlanDayGroup.Superset -> SessionBlock.Superset(
            id = group.id,
            exercises = group.items.map { it.exercise },
            rounds = List(group.items.minOfOrNull { it.targetSets } ?: 0) { round ->
                SupersetRound(
                    index = round,
                    chains = group.items.map { it.previewChain() },
                )
            },
            plan = group.items,
        )
    }
}
