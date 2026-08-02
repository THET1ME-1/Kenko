package com.looker.kenko.ui.addSet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.components.OnSurfaceBorder
import com.looker.kenko.ui.theme.numbers
import kotlin.math.abs
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val COUNT = 512
const val ITEMS = 5
val ItemSize = 40.dp
private val SelectorBoxHeight = 30.dp
private const val MAGNIFICATION = 1.3f

@Composable
fun VerticalSelector(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit = {},
    onChanged: (Int) -> Unit = {},
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(state)

    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        val center = snapshotFlow { state.layoutInfo.centerItemIndex() }
        val scrolling = snapshotFlow { state.isScrollInProgress }

        launch {
            center.collect { onChange(it ?: value) }
        }
        launch {
            scrolling.collect { if(!it) onChanged(center.firstOrNull() ?: value) }
        }
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier,
    ) {
        // This is dial's background
        Spacer(
            modifier = Modifier
                .size(ItemSize + 8.dp, ItemSize * ITEMS)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                ),
        )
        SelectorBackground()
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = ItemSize * (ITEMS / 2)),
            modifier = Modifier
                .height(ItemSize * ITEMS)
                .padding(horizontal = 4.dp)
                .drawWithContent {
                    val lensHeight = SelectorBoxHeight.toPx()
                    val lensTop = (size.height - lensHeight) / 2f
                    val lensBottom = lensTop + lensHeight

                    clipRect(0f, lensTop, size.width, lensBottom, clipOp = ClipOp.Difference) {
                        this@drawWithContent.drawContent()
                    }
                    clipRect(0f, lensTop, size.width, lensBottom) {
                        scale(MAGNIFICATION, pivot = center) {
                            this@drawWithContent.drawContent()
                        }
                    }
                },
        ) {
            items(COUNT) { i ->
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(ItemSize)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                scope.launch { state.animateScrollToItem(i) }
                            },
                        ),
                ) {
                    val density = LocalDensity.current
                    BasicText(
                        text = i.toString(),
                        style = MaterialTheme.typography.labelMedium.numbers(),
                        color = {
                            val itemSizePx = with(density) { ItemSize.toPx() }
                            val distance = abs(state.layoutInfo.distanceFromCenter(i, itemSizePx))
                            val fraction = (1f - distance).coerceIn(0f, 1f)
                            lerp(onSurface, onTertiaryContainer, fraction)
                        },
                    )
                }
            }
        }
        SelectedBox(label)
    }
}

@Composable
fun SelectedBox(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(SelectorBoxHeight)
            .border(
                border = OnSurfaceBorder,
                shape = CircleShape,
            ),
    ) {
        Spacer(Modifier.width(ItemSize))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
fun SelectorBackground(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .width(ItemSize + 8.dp)
            .height(SelectorBoxHeight)
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape,
            ),
    )
}

private fun LazyListLayoutInfo.centerItemIndex(): Int? {
    val mid = viewportStartOffset + (viewportEndOffset - viewportStartOffset) / 2
    val item = visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - mid) }
    return item?.index
}

private fun LazyListLayoutInfo.distanceFromCenter(index: Int, itemSizePx: Float): Float {
    val mid = viewportStartOffset + (viewportEndOffset - viewportStartOffset) / 2f
    val item = visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    return (item.offset + item.size / 2f - mid) / itemSizePx
}
