package com.looker.kenko.data.local.model

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import com.looker.kenko.utils.EpochDays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class SessionEntityTest {

    private val date = LocalDate(2025, 1, 15)
    private val exercise = Exercise("Squat", MuscleGroups.Quads, id = 1)

    @Test
    fun `Session data() converts date to EpochDays`() {
        val session = Session(date = date, sets = emptyList(), planId = 1, id = null)
        assertEquals(EpochDays(date.toEpochDays().toInt()), session.data().date)
    }

    @Test
    fun `Session data() with null id uses 0`() {
        val session = Session(date = date, sets = emptyList(), planId = 1, id = null)
        assertEquals(0, session.data().id)
    }

    @Test
    fun `Session data() with explicit id preserves it`() {
        val session = Session(date = date, sets = emptyList(), planId = 2, id = 55)
        assertEquals(55, session.data().id)
    }

    @Test
    fun `Session data() preserves planId`() {
        val session = Session(date = date, sets = emptyList(), planId = 7, id = 1)
        assertEquals(7, session.data().planId)
    }

    @Test
    fun `Session data() preserves null planId`() {
        val session = Session(date = date, sets = emptyList(), planId = null, id = 1)
        assertEquals(null, session.data().planId)
    }

    @Test
    fun `Session sets() throws NullPointerException when session id is null`() {
        val set = Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2))
        val session = Session(date = date, sets = listOf(set), planId = 1, id = null)
        assertFailsWith<NullPointerException> {
            session.sets()
        }
    }

    @Test
    fun `Session sets() maps order by index position`() {
        val set1 = Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2))
        val set2 = Set(8, 100f, SetType.Drop, exercise, RepsInReserve(1))
        val session = Session(date = date, sets = listOf(set1, set2), planId = 1, id = 10)
        val entities = session.sets()
        assertEquals(0, entities[0].order)
        assertEquals(1, entities[1].order)
    }

    @Test
    fun `Session sets() assigns session id to each set entity`() {
        val set = Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2))
        val session = Session(date = date, sets = listOf(set), planId = 1, id = 42)
        assertEquals(42, session.sets().first().sessionId)
    }

    @Test
    fun `SessionEntity toExternal reconstructs date from epoch days`() {
        val epochDays = EpochDays(date.toEpochDays().toInt())
        val sessionData = SessionDataEntity(date = epochDays, planId = 1, id = 5)
        val sessionEntity = SessionEntity(data = sessionData, sets = emptyList())
        assertEquals(date, sessionEntity.toExternal(setsMap = emptyList()).date)
    }

    @Test
    fun `SessionEntity toExternal preserves planId`() {
        val sessionData = SessionDataEntity(date = EpochDays(0), planId = 3, id = 1)
        val sessionEntity = SessionEntity(data = sessionData, sets = emptyList())
        assertEquals(3, sessionEntity.toExternal(setsMap = emptyList()).planId)
    }

    @Test
    fun `SessionEntity toExternal preserves null planId`() {
        val sessionData = SessionDataEntity(date = EpochDays(0), planId = null, id = 1)
        val sessionEntity = SessionEntity(data = sessionData, sets = emptyList())
        assertEquals(null, sessionEntity.toExternal(setsMap = emptyList()).planId)
    }

    @Test
    fun `SessionEntity toExternal preserves id`() {
        val sessionData = SessionDataEntity(date = EpochDays(0), planId = 1, id = 10)
        val sessionEntity = SessionEntity(data = sessionData, sets = emptyList())
        assertEquals(10, sessionEntity.toExternal(setsMap = emptyList()).id)
    }

    @Test
    fun `SessionEntity toExternal uses provided setsMap`() {
        val sessionData = SessionDataEntity(date = EpochDays(0), planId = 1, id = 1)
        val externalSet = Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2))
        val sessionEntity = SessionEntity(data = sessionData, sets = emptyList())
        val session = sessionEntity.toExternal(setsMap = listOf(externalSet))
        assertEquals(1, session.sets.size)
        assertEquals(externalSet, session.sets.first())
    }
}
