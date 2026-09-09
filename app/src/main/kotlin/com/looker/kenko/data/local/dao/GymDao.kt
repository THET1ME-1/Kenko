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

package com.looker.kenko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.local.model.GymEntity
import com.looker.kenko.data.local.model.GymExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {

    @Query("SELECT * FROM gyms ORDER BY name")
    fun gymsFlow(): Flow<List<GymEntity>>

    @Query("SELECT * FROM gyms WHERE id = :gymId")
    suspend fun getGym(gymId: Int): GymEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM gym_exercises
        WHERE gymId = :gymId
        """,
    )
    fun exerciseCountFlow(gymId: Int): Flow<Int>

    @Query(
        """
        SELECT exercises.*
        FROM exercises
        INNER JOIN gym_exercises ON exercises.id = gym_exercises.exerciseId
        WHERE gym_exercises.gymId = :gymId
        ORDER BY exercises.name
        """,
    )
    fun exercisesFlow(gymId: Int): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT exerciseId
        FROM gym_exercises
        WHERE gymId = :gymId
        """,
    )
    suspend fun getExerciseIds(gymId: Int): List<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM gym_exercises WHERE gymId = :gymId AND exerciseId = :exerciseId
        )
        """,
    )
    suspend fun hasExercise(gymId: Int, exerciseId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGym(gym: GymEntity): Long

    @Update
    suspend fun updateGym(gym: GymEntity)

    @Query("DELETE FROM gyms WHERE id = :gymId")
    suspend fun deleteGym(gymId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addExercise(link: GymExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addExercises(links: List<GymExerciseEntity>)

    @Query("DELETE FROM gym_exercises WHERE gymId = :gymId AND exerciseId = :exerciseId")
    suspend fun removeExercise(gymId: Int, exerciseId: Int)

    @Query("DELETE FROM gym_exercises WHERE gymId = :gymId")
    suspend fun clearExercises(gymId: Int)
}
