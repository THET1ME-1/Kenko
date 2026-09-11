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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The settings screen, built the way Togetherly builds it: a section title in capitals, a group
 * where every item is its own block, and a row of icon chip, title, note and a chevron.
 *
 * Outer corners of a group are round, inner ones are small, so the blocks read as one section
 * instead of a scattering of cards.
 */
private val OuterCorner = 28.dp
private val InnerCorner = 8.dp
private val BlockGap = 4.dp

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = icon,
                contentDescription = null,
                tint = color,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            ),
            color = color,
        )
    }
}

/**
 * Blocks of one section. The shape of a block comes from its place: the first one carries the
 * round top, the last one the round bottom, the middle ones are square-ish.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val scope = remember { SettingsGroupScope() }
    scope.items.clear()
    scope.content()
    val rows = scope.items.toList()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        rows.forEachIndexed { index, row ->
            val shape = RoundedCornerShape(
                topStart = if (index == 0) OuterCorner else InnerCorner,
                topEnd = if (index == 0) OuterCorner else InnerCorner,
                bottomStart = if (index == rows.lastIndex) OuterCorner else InnerCorner,
                bottomEnd = if (index == rows.lastIndex) OuterCorner else InnerCorner,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                row()
            }
        }
    }
}

/**
 * Collects the blocks of a group, so their shapes can be decided by their place in it.
 */
class SettingsGroupScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun block(content: @Composable () -> Unit) {
        items += content
    }
}

/**
 * A row of settings: round icon chip, title with a note under it, and whatever stands on the
 * right — a chevron for a screen behind the row, a switch for a thing to turn on.
 */
@Composable
fun SettingsRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconChip(icon = icon, background = iconBackground, color = iconColor)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = titleColor,
            )
            if (subtitle != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * A block that holds something of its own — a theme picker, a palette row — with the title above.
 */
@Composable
fun SettingsBlock(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun SettingsIconChip(
    icon: Painter,
    background: Color,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = icon,
            contentDescription = null,
            tint = color,
        )
    }
}

/**
 * The arrow of a row that leads somewhere, so it is not repeated at every call.
 */
@Composable
fun SettingsChevron(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        painter = com.looker.kenko.ui.theme.KenkoIcons.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
    )
}
