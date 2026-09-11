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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * A row as the list currently draws it: where it sits and how tall it is.
 */
internal data class ReorderItem(
    val key: Any,
    val index: Int,
    val offset: Int,
    val size: Int,
)

/**
 * Where the rows are. A column and a grid answer the same three questions differently.
 */
internal fun interface ReorderLayout {
    fun visibleItems(): List<ReorderItem>
}

/**
 * Drag and drop for a lazy list or grid, held by a handle instead of a long press: the same
 * finger that reads the list should not be able to rearrange it by accident.
 *
 * The list keeps its own order while the finger is down and reports it once, when the finger
 * lifts — a row that moves on every pixel fights the data behind it.
 */
@Stable
class ReorderState internal constructor(
    private val layout: ReorderLayout,
    /**
     * Which rows take part: a block of a session is drawn by several items, and only the one
     * carrying the handle can be swapped with another block.
     */
    private val isReorderable: (Any) -> Boolean = { true },
) {

    var draggingKey: Any? by mutableStateOf(null)
        private set

    private var distance by mutableFloatStateOf(0F)

    /**
     * How far the dragged row stands from where the list drew it.
     */
    fun offsetOf(key: Any): Float = if (key == draggingKey) distance else 0F

    internal fun start(key: Any) {
        draggingKey = key
        distance = 0F
    }

    internal fun drag(delta: Float, onMove: (from: Any, to: Any) -> Unit) {
        val key = draggingKey ?: return
        val items = layout.visibleItems()
        val current = items.firstOrNull { it.key == key } ?: return
        distance += delta
        val top = current.offset + distance
        val middle = top + current.size / 2F
        val target = items.firstOrNull { item ->
            item.key != key &&
                isReorderable(item.key) &&
                item.index != current.index &&
                middle >= item.offset &&
                middle <= item.offset + item.size
        } ?: return
        onMove(key, target.key)
        // The row keeps sitting under the finger: the list moved it, so the offset moves back.
        distance -= (target.offset - current.offset)
    }

    internal fun stop() {
        draggingKey = null
        distance = 0F
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    isReorderable: (Any) -> Boolean = { true },
): ReorderState = remember(listState) {
    ReorderState(
        layout = ReorderLayout {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                val key = item.key ?: return@mapNotNull null
                ReorderItem(key = key, index = item.index, offset = item.offset, size = item.size)
            }
        },
        isReorderable = isReorderable,
    )
}

@Composable
fun rememberReorderState(
    gridState: LazyGridState,
    isReorderable: (Any) -> Boolean = { true },
): ReorderState = remember(gridState) {
    ReorderState(
        layout = ReorderLayout {
            gridState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                val key = item.key ?: return@mapNotNull null
                ReorderItem(
                    key = key,
                    index = item.index,
                    offset = item.offset.y,
                    size = item.size.height,
                )
            }
        },
        isReorderable = isReorderable,
    )
}

/**
 * Put on the row: lifts it above its neighbours while it is being dragged.
 */
fun Modifier.reorderableItem(state: ReorderState, key: Any): Modifier = this
    .zIndex(if (state.draggingKey == key) 1F else 0F)
    .graphicsLayer { translationY = state.offsetOf(key) }

/**
 * Put on the handle: the only place the row can be picked up by.
 */
fun Modifier.reorderHandle(
    state: ReorderState,
    key: Any,
    onMove: (from: Any, to: Any) -> Unit,
    onDropped: () -> Unit,
): Modifier = this.pointerInput(key) {
    detectDragGestures(
        onDragStart = { state.start(key) },
        onDragEnd = {
            state.stop()
            onDropped()
        },
        onDragCancel = {
            state.stop()
            onDropped()
        },
        onDrag = { change, dragAmount ->
            change.consume()
            state.drag(dragAmount.y, onMove)
        },
    )
}
