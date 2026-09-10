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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.PlanDayResolver
import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.PlanHistoryDao
import com.looker.kenko.data.local.dao.SessionDao
import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.local.model.SessionDataEntity
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.local.model.WeightNote
import com.looker.kenko.data.local.model.toEntity
import com.looker.kenko.data.local.model.toExternal
import com.looker.kenko.data.model.MAX_DROP_COUNT
import com.looker.kenko.data.model.MAX_DROP_PERCENT
import com.looker.kenko.data.model.MIN_DROP_PERCENT
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.buildDropChain
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.toLocalEpochDays
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class LocalSessionRepo @Inject constructor(
    private val dao: SessionDao,
    private val dayResolver: dagger.Lazy<PlanDayResolver>,
    private val setsDao: SetsDao,
    private val historyDao: PlanHistoryDao,
    private val exerciseDao: ExerciseDao,
    private val settingsRepo: SettingsRepo,
) : SessionRepo {

    override val stream: Flow<List<Session>> =
        dao.stream().map {
            it.map { session ->
                session.toExternal(session.sets.toExternal())
            }
        }
    override val setsCount: Flow<Int> =
        setsDao.totalSetCount()

    override val sessionsCount: Flow<Int> = dao.totalSessions()

    override suspend fun addSet(sessionId: Int, set: Set) {
        setsDao.insert(set.toEntity(sessionId, nextOrder(sessionId)))
    }

    override suspend fun addSet(
        sessionId: Int,
        exerciseId: Int,
        weight: Float,
        reps: Int,
        setType: SetType,
        rir: RepsInReserve,
        supersetId: Int?,
        dropCount: Int,
        dropPercent: Int,
        gripId: Int?,
        weightNote: WeightNote?,
    ) {
        setsDao.insert(
            SetEntity(
                repsOrDuration = reps,
                weight = weight,
                exerciseId = exerciseId,
                sessionId = sessionId,
                type = setType,
                order = nextOrder(sessionId),
                rir = rir.value,
                supersetId = supersetId,
                roundIndex = supersetId?.let {
                    setsDao.getSupersetSetCount(sessionId, exerciseId, it)
                },
                dropCount = dropCount,
                dropPercent = dropPercent,
                gripId = gripId,
                weightNote = weightNote,
            ),
        )
    }

    override suspend fun clearLastSupersetRound(sessionId: Int, supersetId: Int) {
        val performed = setsDao.getSupersetSets(sessionId, supersetId)
        val lastRound = performed.maxOfOrNull { it.roundIndex ?: 0 } ?: return
        performed
            .filter { (it.roundIndex ?: 0) == lastRound }
            .forEach { setsDao.delete(it.id) }
    }

    override suspend fun addDrop(
        parentSetId: Int,
        weight: Float,
        reps: Int,
        rir: RepsInReserve,
        dropIndex: Int,
    ) {
        val parent = requireNotNull(setsDao.get(parentSetId)) { "Parent set is gone" }
        require(parent.parentSetId == null) { "A drop cannot hang under another drop" }
        val index = dropIndex.takeIf { it > 0 } ?: ((setsDao.getMaxDropIndex(parentSetId) ?: 0) + 1)
        setsDao.insert(
            parent.copy(
                id = 0,
                repsOrDuration = reps,
                weight = weight,
                type = SetType.Drop,
                rir = rir.value,
                parentSetId = parentSetId,
                dropIndex = index,
                dropCount = 0,
            ),
        )
        if (parent.type != SetType.Drop) {
            setsDao.updateType(parentSetId, SetType.Drop)
        }
    }

    override suspend fun setDropSettings(setId: Int, count: Int, percent: Int) {
        setsDao.updateDropSettings(
            setId = setId,
            count = count.coerceIn(0, MAX_DROP_COUNT),
            percent = percent.coerceIn(MIN_DROP_PERCENT, MAX_DROP_PERCENT),
            type = if (count > 0) SetType.Drop else SetType.Standard,
        )
        val extra = setsDao.getDrops(setId).filter { it.dropIndex > count }
        for (drop in extra) {
            setsDao.delete(drop.id)
        }
    }

    override suspend fun markDropStep(parentSetId: Int, stepIndex: Int) {
        val parent = requireNotNull(setsDao.get(parentSetId)) { "Parent set is gone" }
        val performed = setsDao.getDrops(parentSetId)
        if (performed.any { it.dropIndex == stepIndex }) return
        val step = chainOf(parent).getOrNull(stepIndex) ?: return
        setsDao.insert(
            parent.copy(
                id = 0,
                repsOrDuration = step.reps,
                weight = step.weight,
                type = SetType.Drop,
                parentSetId = parentSetId,
                dropIndex = stepIndex,
                dropCount = 0,
            ),
        )
    }

    override suspend fun markWholeDropGroup(parentSetId: Int) {
        val parent = requireNotNull(setsDao.get(parentSetId)) { "Parent set is gone" }
        val done = setsDao.getDrops(parentSetId).map { it.dropIndex }.toSet()
        chainOf(parent).drop(1).forEach { step ->
            if (step.index !in done) {
                markDropStep(parentSetId, step.index)
            }
        }
    }

    override suspend fun clearDrops(parentSetId: Int) {
        setsDao.deleteDrops(parentSetId)
    }

    override suspend fun closeSupersetRound(
        sessionId: Int,
        supersetId: Int,
        items: List<PlanItem>,
    ) {
        if (items.isEmpty()) return
        val performed = setsDao.getSupersetSets(sessionId, supersetId)
        val round = performed
            .groupBy { it.roundIndex ?: 0 }
            .entries
            .sortedBy { it.key }
            .lastOrNull { (_, sets) -> sets.size < items.size }
            ?.key
            ?: performed.size.let { if (it == 0) 0 else (performed.maxOf { set -> set.roundIndex ?: 0 } + 1) }
        val alreadyIn = performed.filter { (it.roundIndex ?: 0) == round }.map { it.exerciseId }.toSet()
        for (item in items) {
            val exerciseId = item.exercise.id ?: continue
            if (exerciseId in alreadyIn) continue
            val weight = setsDao.getLastSetByExerciseId(exerciseId)?.weight ?: 0F
            setsDao.insert(
                SetEntity(
                    repsOrDuration = item.targetReps,
                    weight = weight,
                    type = SetType.Standard,
                    order = nextOrder(sessionId),
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    rir = RepsInReserve(2).value,
                    supersetId = supersetId,
                    roundIndex = round,
                ),
            )
        }
    }

    private fun chainOf(parent: SetEntity) = buildDropChain(
        base = parent.weight,
        drops = parent.dropCount,
        reps = parent.repsOrDuration,
        percent = parent.dropPercent,
    )

    private suspend fun nextOrder(sessionId: Int): Int =
        setsDao.getMaxOrder(sessionId)?.plus(1) ?: 0

    override suspend fun removeSet(setId: Int) {
        if (!dao.sessionExistsOn(localDate.toLocalEpochDays())) {
            error("Session does not exist so set cannot be removed")
        }
        setsDao.delete(setId)
    }

    override suspend fun getSessionIdOrCreate(date: LocalDate): Int {
        // Тренироваться можно и без программы: тогда сессия просто ни к чему не привязана.
        val currentPlanId = historyDao.getCurrentId()
        val existingId = dao.getSessionId(date.toLocalEpochDays())
        if (existingId != null) {
            return existingId
        }
        val day = dayResolver.get().dayFor(date, currentPlanId)
        return dao.insert(
            SessionDataEntity(
                date = date.toLocalEpochDays(),
                planId = currentPlanId,
                gymId = settingsRepo.stream.first().currentGymId,
                dayIndex = day,
                startedAt = Clock.System.now().epochSeconds,
            ),
        ).toInt()
    }

    override suspend fun finishSession(
        sessionId: Int,
        name: String?,
        note: String?,
        photoUri: String?,
        finishedAt: Long,
        minutes: Int,
    ) {
        dao.finishSession(
            sessionId = sessionId,
            name = name?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            photoUri = photoUri,
            finishedAt = finishedAt,
            startedAt = finishedAt - minutes.coerceAtLeast(0) * 60L,
        )
    }

    override suspend fun reopenSession(sessionId: Int) {
        dao.reopenSession(sessionId)
    }

    override suspend fun removeSession(sessionId: Int) {
        dao.deleteSession(sessionId)
    }

    override suspend fun moveSession(sessionId: Int, date: LocalDate): Boolean {
        val target = date.toLocalEpochDays()
        if (dao.sessionExistsOn(target)) return false
        dao.moveSession(sessionId, target)
        return true
    }

    override fun streamByDate(date: LocalDate): Flow<Session?> {
        return dao
            .session(date.toLocalEpochDays())
            .map { session ->
                if (session == null) return@map null
                session.toExternal(session.sets.toExternal())
            }
    }

    override suspend fun getSets(sessionId: Int): List<Set> =
        setsDao.getSetsBySessionId(sessionId).toExternal()

    override suspend fun getLastSetByExerciseId(exerciseId: Int): Set? = withContext(Dispatchers.IO) {
        val exercise = exerciseDao.get(exerciseId) ?: return@withContext null
        setsDao.getLastSetByExerciseId(exerciseId)?.toExternal(exercise.toExternal())
    }

    private suspend fun List<SetEntity>.toExternal(): List<Set> = mapNotNull {
        val exercise = exerciseDao.get(it.exerciseId) ?: return@mapNotNull null
        it.toExternal(exercise.toExternal())
    }
}
