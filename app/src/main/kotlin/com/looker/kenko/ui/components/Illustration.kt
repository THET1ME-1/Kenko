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

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.looker.kenko.data.IllustrationStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Picture of a movement, downloaded the first time it is needed.
 *
 * With [animate] the two shots of the movement take turns, so the start and the end of the rep
 * read as one motion. A list shows only the first shot: a page of flickering figures helps nobody.
 */
@Composable
fun rememberIllustration(
    illustration: String?,
    frames: Int,
    animate: Boolean = false,
): ImageBitmap? {
    val store = rememberIllustrationStore()
    val shots by produceState(emptyList<ImageBitmap>(), illustration, frames, animate) {
        if (illustration == null || frames == 0) {
            value = emptyList()
            return@produceState
        }
        val loaded = mutableListOf<ImageBitmap>()
        val wanted = if (animate) frames else 1
        for (index in 0 until wanted) {
            val file = store.frame(illustration, index) ?: continue
            val shot = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(file.path)?.asImageBitmap()
            } ?: continue
            loaded += shot
            // Первый кадр показывается сразу, второй догоняет.
            value = loaded.toList()
        }
    }
    var index by remember(shots.size) { mutableIntStateOf(0) }
    LaunchedEffect(shots.size, animate) {
        if (!animate || shots.size < 2) return@LaunchedEffect
        while (true) {
            delay(SWAP_MILLIS)
            index = (index + 1) % shots.size
        }
    }
    return shots.getOrNull(index)
}

@Composable
fun rememberIllustrationStore(): IllustrationStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { illustrationStoreOf(context) }
}

/**
 * The store lives in the Hilt graph, but the composables that draw pictures have no view model
 * of their own — they reach for it here.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface IllustrationEntryPoint {
    fun illustrationStore(): IllustrationStore
}

private fun illustrationStoreOf(context: Context): IllustrationStore =
    EntryPointAccessors.fromApplication(context, IllustrationEntryPoint::class.java)
        .illustrationStore()

/**
 * Long enough to see the position, short enough to read as a movement.
 */
private const val SWAP_MILLIS = 900L
