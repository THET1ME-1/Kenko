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

package com.looker.kenko.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Pictures of the movements: kept out of the app and fetched when an exercise is first shown.
 *
 * A thousand illustrations would weigh more than everything else in Kenko put together, so the
 * app carries none of them. What it has seen once lives in its own folder and works offline.
 */
@Singleton
class IllustrationStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val folder: File
        get() = File(context.filesDir, "illustrations").apply { mkdirs() }

    private val locks = mutableMapOf<String, Mutex>()
    private val guard = Mutex()

    /**
     * Path of a frame, downloading it if this is the first time it is asked for.
     *
     * Returns null when there is no network and nothing cached — the screen then draws the
     * muscle on the body instead.
     */
    suspend fun frame(illustration: String, index: Int): File? = withContext(Dispatchers.IO) {
        val target = File(folder, "${illustration}_$index.webp")
        if (target.exists() && target.length() > 0) return@withContext target
        val mutex = guard.withLock { locks.getOrPut(target.name) { Mutex() } }
        mutex.withLock {
            if (target.exists() && target.length() > 0) return@withLock target
            download(illustration, index, target)
        }
    }

    /**
     * Pulls the whole catalogue at once, for a gym where the phone has no signal.
     *
     * A few requests run side by side: one at a time takes minutes, all at once floods the CDN.
     */
    suspend fun prefetch(
        wanted: List<Frame>,
        onProgress: (done: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val done = AtomicInteger()
        val gate = Semaphore(PARALLEL)
        coroutineScope {
            wanted.map { item ->
                async {
                    gate.withPermit { frame(item.illustration, item.index) }
                    onProgress(done.incrementAndGet(), wanted.size)
                }
            }.awaitAll()
        }
    }

    /**
     * Whether the frame is already on the phone, without going to the network.
     */
    fun cached(illustration: String, index: Int): File? =
        File(folder, "${illustration}_$index.webp").takeIf { it.exists() && it.length() > 0 }

    /**
     * Kilobytes the cache takes up right now.
     */
    fun size(): Long = folder.listFiles()?.sumOf { it.length() } ?: 0

    fun clear() {
        folder.listFiles()?.forEach { it.delete() }
    }

    private fun download(illustration: String, index: Int, target: File): File? = runCatching {
        val url = URL("$CDN/$illustration/$index.jpg")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            requestMethod = "GET"
        }
        val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
            ?: return@runCatching null
        val scaled = bitmap.scaledTo(MAX_WIDTH)
        target.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, out)
        }
        if (scaled != bitmap) bitmap.recycle()
        target
    }.getOrNull()

    private fun Bitmap.scaledTo(maxWidth: Int): Bitmap {
        if (width <= maxWidth) return this
        val height = (this.height.toFloat() * maxWidth / width).toInt()
        return Bitmap.createScaledBitmap(this, maxWidth, height, true)
    }
}

/**
 * One picture of one movement: which exercise it belongs to and which of its shots it is.
 */
data class Frame(val illustration: String, val index: Int)

/**
 * The free-exercise-db set, served through a CDN. Its pictures are public domain.
 */
private const val CDN = "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises"

/**
 * A phone screen never shows the picture wider than this, and the file stays around 8 KB.
 */
private const val MAX_WIDTH = 480
private const val QUALITY = 80
private const val TIMEOUT_MILLIS = 15_000

/**
 * How many pictures travel at once when the whole catalogue is being pulled.
 */
private const val PARALLEL = 6
