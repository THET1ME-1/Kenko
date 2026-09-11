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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One thing an exercise can be told to do.
 */
@Immutable
data class RowAction(
    val label: String,
    /**
     * What the action will do to, shown on the right: the goal it will open, the handle it holds.
     */
    val hint: String? = null,
    val isDanger: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Everything that can be done to a row, in one list.
 *
 * The plan editor and the running session open the same sheet, so a movement is handled the same
 * way wherever the lifter meets it. The session adds writing a set on top; the rest is shared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsSheet(
    title: String,
    actions: List<RowAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            actions.forEachIndexed { index, action ->
                if (action.isDanger && index > 0 && !actions[index - 1].isDanger) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        thickness = KenkoBorderWidth,
                    )
                }
                SheetAction(
                    text = action.label,
                    hint = action.hint,
                    onClick = action.onClick,
                    contentColor = if (action.isDanger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/**
 * A row of the sheet with its value on the right.
 */
@Composable
internal fun SheetActionRow(
    text: String,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
