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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.GripDao
import com.looker.kenko.data.local.model.toEntity
import com.looker.kenko.data.local.model.toExternal
import com.looker.kenko.data.model.Grip
import com.looker.kenko.data.repository.GripRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalGripRepo @Inject constructor(
    private val dao: GripDao,
) : GripRepo {

    override fun grips(exerciseId: Int): Flow<List<Grip>> =
        dao.gripsFlow(exerciseId).map { list -> list.map { it.toExternal() } }

    override suspend fun getGrips(exerciseId: Int): List<Grip> =
        dao.getGrips(exerciseId).map { it.toExternal() }

    override suspend fun get(gripId: Int): Grip? = dao.getGrip(gripId)?.toExternal()

    override suspend fun upsert(grip: Grip) {
        if (grip.id == null) {
            dao.insert(grip.toEntity())
        } else {
            dao.update(grip.toEntity())
        }
    }

    override suspend fun delete(gripId: Int) {
        dao.delete(gripId)
    }
}
