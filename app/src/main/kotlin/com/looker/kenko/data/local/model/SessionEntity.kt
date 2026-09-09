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

package com.looker.kenko.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import com.looker.kenko.utils.EpochDays
import kotlinx.datetime.LocalDate

data class SessionEntity(
    @Embedded
    val data: SessionDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sets: List<SetEntity>,
)

@Entity(
    "sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GymEntity::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class SessionDataEntity(
    val date: EpochDays,
    @ColumnInfo(index = true)
    val planId: Int?,
    /**
     * Gym the session happened in. Weights of the same exercise differ from gym to gym, and the
     * app can only tell them apart if it knows where the set was written.
     */
    @ColumnInfo(index = true, defaultValue = "NULL")
    val gymId: Int? = null,
    /**
     * Name a lifter gave the session, when they gave one.
     */
    @ColumnInfo(defaultValue = "NULL")
    val name: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val note: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val photoUri: String? = null,
    /**
     * When the first set was written, in epoch seconds.
     */
    @ColumnInfo(defaultValue = "NULL")
    val startedAt: Long? = null,
    /**
     * When the session was closed. Until then the session is still running.
     */
    @ColumnInfo(defaultValue = "NULL")
    val finishedAt: Long? = null,
    /**
     * Day of the plan this session went through, so the next one knows what follows.
     */
    @ColumnInfo(defaultValue = "NULL")
    val dayIndex: Int? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun Session.data(): SessionDataEntity = SessionDataEntity(
    date = EpochDays(date.toEpochDays().toInt()),
    planId = planId,
    gymId = gymId,
    name = name,
    note = note,
    photoUri = photoUri,
    startedAt = startedAt,
    finishedAt = finishedAt,
    dayIndex = dayIndex,
    id = id ?: 0,
)

fun Session.sets(): List<SetEntity> = sets.map { it.toEntity(id!!, sets.indexOf(it)) }

fun SessionEntity.toExternal(
    setsMap: List<Set>,
): Session = Session(
    planId = data.planId,
    date = LocalDate.fromEpochDays(data.date.value),
    sets = setsMap,
    dayIndex = data.dayIndex,
    gymId = data.gymId,
    name = data.name,
    note = data.note,
    photoUri = data.photoUri,
    startedAt = data.startedAt,
    finishedAt = data.finishedAt,
    id = data.id,
)
