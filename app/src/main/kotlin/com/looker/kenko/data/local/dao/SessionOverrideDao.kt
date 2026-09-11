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
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.looker.kenko.data.local.model.SessionOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionOverrideDao {

    @Query("SELECT * FROM session_overrides WHERE sessionId = :sessionId")
    fun stream(sessionId: Int): Flow<List<SessionOverrideEntity>>

    @Query("SELECT * FROM session_overrides WHERE sessionId = :sessionId")
    suspend fun get(sessionId: Int): List<SessionOverrideEntity>

    @Query(
        """
        SELECT * FROM session_overrides
        WHERE sessionId = :sessionId AND exerciseId = :exerciseId
        """,
    )
    suspend fun get(sessionId: Int, exerciseId: Int): SessionOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: SessionOverrideEntity)

    @Query("DELETE FROM session_overrides WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun delete(sessionId: Int, exerciseId: Int)

    @Query("DELETE FROM session_overrides WHERE sessionId = :sessionId")
    suspend fun clear(sessionId: Int)
}
