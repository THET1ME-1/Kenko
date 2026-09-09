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

package com.looker.kenko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.data.local.model.SetType
import kotlinx.coroutines.flow.Flow

@Dao
interface SetsDao {

    @Query(
        """
        SELECT *
        FROM sets
        WHERE sessionId = :sessionId
        ORDER BY `order`, dropIndex, id
        """,
    )
    fun setsBySessionId(sessionId: Int): Flow<List<SetEntity>>

    @Query(
        """
        SELECT sets.*
        FROM sets
        INNER JOIN sessions ON sets.sessionId = sessions.id
        WHERE sets.exerciseId = :exerciseId
        AND sets.parentSetId IS NULL
        AND sets.type != 'Warmup'
        ORDER BY sessions.date DESC, sets.`order` DESC
        LIMIT 1
        """
    )
    suspend fun getLastSetByExerciseId(exerciseId: Int): SetEntity?

    @Query(
        """
        SELECT *
        FROM sets
        WHERE sessionId = :sessionId
        ORDER BY `order`, dropIndex, id
        """,
    )
    suspend fun getSetsBySessionId(sessionId: Int): List<SetEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM sets
        WHERE sessionId = :sessionId
        """,
    )
    suspend fun getSetsCountBySessionId(sessionId: Int): Int?

    @Query(
        """
        SELECT *
        FROM sets
        WHERE (:exerciseId IS NULL OR exerciseId = :exerciseId)
        AND sessionId IN (
            SELECT id
            FROM sessions
            WHERE (:planId IS NULL OR planId = :planId)
        )
        ORDER BY `order`
        """,
    )
    fun setsByExerciseIdPerPlan(exerciseId: Int? = null, planId: Int? = null): Flow<List<SetEntity>>

    @Query(
        """
        SELECT *
        FROM sets
        WHERE (:exerciseId IS NULL OR exerciseId = :exerciseId)
        AND sessionId IN (
            SELECT id
            FROM sessions
            WHERE (:planId IS NULL OR planId = :planId)
        )
        ORDER BY `order`
        """,
    )
    suspend fun getSetsByExerciseIdPerPlan(
        exerciseId: Int? = null,
        planId: Int? = null,
    ): List<SetEntity>

    @Query(
        """
        SELECT COUNT (*)
        FROM sets
        """,
    )
    fun totalSetCount(): Flow<Int>

    @Query(
        """
        SELECT MAX(`order`)
        FROM sets
        WHERE sessionId = :sessionId
        AND parentSetId IS NULL
        """,
    )
    suspend fun getMaxOrder(sessionId: Int): Int?

    @Query(
        """
        SELECT MAX(dropIndex)
        FROM sets
        WHERE parentSetId = :parentSetId
        """,
    )
    suspend fun getMaxDropIndex(parentSetId: Int): Int?

    @Query(
        """
        SELECT *
        FROM sets
        WHERE id = :setId
        """,
    )
    suspend fun get(setId: Int): SetEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM sets
        WHERE sessionId = :sessionId
        AND exerciseId = :exerciseId
        AND supersetId = :supersetId
        AND parentSetId IS NULL
        """,
    )
    suspend fun getSupersetSetCount(sessionId: Int, exerciseId: Int, supersetId: Int): Int

    @Query(
        """
        SELECT MAX(supersetId)
        FROM sets
        WHERE sessionId = :sessionId
        """,
    )
    suspend fun getMaxSupersetId(sessionId: Int): Int?

    @Query(
        """
        SELECT MAX(roundIndex)
        FROM sets
        WHERE sessionId = :sessionId
        AND supersetId = :supersetId
        """,
    )
    suspend fun getMaxRoundIndex(sessionId: Int, supersetId: Int): Int?

    @Query(
        """
        UPDATE sets
        SET type = :type
        WHERE id = :setId
        """,
    )
    suspend fun updateType(setId: Int, type: SetType)

    @Query(
        """
        UPDATE sets
        SET dropCount = :count, dropPercent = :percent, type = :type
        WHERE id = :setId
        """,
    )
    suspend fun updateDropSettings(setId: Int, count: Int, percent: Int, type: SetType)

    @Query(
        """
        SELECT *
        FROM sets
        WHERE parentSetId = :parentSetId
        ORDER BY dropIndex
        """,
    )
    suspend fun getDrops(parentSetId: Int): List<SetEntity>

    @Query(
        """
        DELETE
        FROM sets
        WHERE parentSetId = :parentSetId
        """,
    )
    suspend fun deleteDrops(parentSetId: Int)

    @Query(
        """
        SELECT *
        FROM sets
        WHERE sessionId = :sessionId
        AND supersetId = :supersetId
        AND parentSetId IS NULL
        ORDER BY roundIndex, `order`
        """,
    )
    suspend fun getSupersetSets(sessionId: Int, supersetId: Int): List<SetEntity>

    @Insert
    suspend fun insert(set: SetEntity): Long

    @Query(
        """
        DELETE
        FROM sets
        WHERE id = :setId
        """,
    )
    suspend fun delete(setId: Int)
}
