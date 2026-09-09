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
import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.model.MuscleGroups
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One exercise of the shipped catalogue, as it lies in `assets/exercises.json`.
 */
@Serializable
private data class SeedExercise(
    val name: String,
    @SerialName("nameRu") val russianName: String? = null,
    val target: String,
    val secondary: List<String> = emptyList(),
    val illustration: String? = null,
    val frames: Int = 0,
)

/**
 * Fills the exercise library from the catalogue the app ships with.
 *
 * Runs on every start and only adds what is missing, so a lifter's own exercises and the ones
 * they renamed stay untouched. Existing rows get their Russian name and illustration filled in.
 */
@Singleton
class ExerciseSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: ExerciseDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seed() = withContext(Dispatchers.IO) {
        val catalogue = runCatching {
            context.assets.open(ASSET).use { stream ->
                json.decodeFromString<List<SeedExercise>>(stream.readBytes().decodeToString())
            }
        }.getOrNull() ?: return@withContext

        val existing = dao.all().associateBy { it.name.trim().lowercase() }
        catalogue.forEach { seed ->
            val target = seed.target.toMuscle() ?: return@forEach
            val current = existing[seed.name.trim().lowercase()]
            when {
                current == null -> dao.upsert(
                    ExerciseEntity(
                        name = seed.name,
                        target = target,
                        secondaryTargets = seed.secondary.joinToString(","),
                        nameRu = seed.russianName,
                        illustration = seed.illustration,
                        frames = seed.frames,
                    ),
                )

                current.illustration == null || current.nameRu == null -> dao.upsert(
                    current.copy(
                        nameRu = current.nameRu ?: seed.russianName,
                        illustration = current.illustration ?: seed.illustration,
                        frames = if (current.frames == 0) seed.frames else current.frames,
                    ),
                )
            }
        }
    }

    private fun String.toMuscle(): MuscleGroups? =
        MuscleGroups.entries.firstOrNull { it.name == this }
}

private const val ASSET = "exercises.json"
