package com.looker.kenko.data.local.model

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SetEntityTest {

    private val exercise = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 5)

    @Test
    fun `toExternal maps repsOrDuration correctly`() {
        val entity = SetEntity(repsOrDuration = 12, weight = 80f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 2)
        assertEquals(12, entity.toExternal(exercise).repsOrDuration)
    }

    @Test
    fun `toExternal maps weight correctly`() {
        val entity = SetEntity(repsOrDuration = 10, weight = 75.5f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 2)
        assertEquals(75.5f, entity.toExternal(exercise).weight)
    }

    @Test
    fun `toExternal maps Standard set type`() {
        val entity = SetEntity(repsOrDuration = 10, weight = 80f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 2)
        assertEquals(SetType.Standard, entity.toExternal(exercise).type)
    }

    @Test
    fun `toExternal maps Drop set type`() {
        val entity = SetEntity(repsOrDuration = 12, weight = 60f, type = SetType.Drop, order = 1, sessionId = 1, exerciseId = 5, rir = 3)
        assertEquals(SetType.Drop, entity.toExternal(exercise).type)
    }

    @Test
    fun `toExternal maps RestPause set type`() {
        val entity = SetEntity(repsOrDuration = 8, weight = 100f, type = SetType.RestPause, order = 2, sessionId = 1, exerciseId = 5, rir = 0)
        assertEquals(SetType.RestPause, entity.toExternal(exercise).type)
    }

    @Test
    fun `toExternal wraps rir int into RepsInReserve`() {
        val entity = SetEntity(repsOrDuration = 10, weight = 50f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 4)
        assertEquals(RepsInReserve(4), entity.toExternal(exercise).rir)
    }

    @Test
    fun `toExternal maps id correctly`() {
        val entity = SetEntity(repsOrDuration = 10, weight = 80f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 2, id = 99)
        assertEquals(99, entity.toExternal(exercise).id)
    }

    @Test
    fun `toExternal assigns provided exercise`() {
        val entity = SetEntity(repsOrDuration = 10, weight = 80f, type = SetType.Standard, order = 0, sessionId = 1, exerciseId = 5, rir = 2)
        assertEquals(exercise, entity.toExternal(exercise).exercise)
    }

    @Test
    fun `toEntity with null set id uses 0`() {
        val set = Set(repsOrDuration = 10, weight = 80f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2), id = null)
        assertEquals(0, set.toEntity(sessionId = 1, order = 0).id)
    }

    @Test
    fun `toEntity with explicit id preserves it`() {
        val set = Set(repsOrDuration = 10, weight = 80f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2), id = 77)
        assertEquals(77, set.toEntity(sessionId = 1, order = 0).id)
    }

    @Test
    fun `toEntity preserves sessionId`() {
        val set = Set(repsOrDuration = 10, weight = 80f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        assertEquals(99, set.toEntity(sessionId = 99, order = 0).sessionId)
    }

    @Test
    fun `toEntity preserves order`() {
        val set = Set(repsOrDuration = 10, weight = 80f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        assertEquals(3, set.toEntity(sessionId = 1, order = 3).order)
    }

    @Test
    fun `toEntity maps rir value correctly`() {
        val set = Set(repsOrDuration = 8, weight = 60f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(3))
        assertEquals(3, set.toEntity(sessionId = 1, order = 0).rir)
    }

    @Test
    fun `toEntity with null exercise id throws IllegalArgumentException`() {
        val noIdExercise = Exercise(name = "Curl", target = MuscleGroups.Biceps, id = null)
        val set = Set(repsOrDuration = 10, weight = 50f, type = SetType.Standard, exercise = noIdExercise, rir = RepsInReserve(2))
        assertFailsWith<IllegalArgumentException> {
            set.toEntity(sessionId = 1, order = 0)
        }
    }

    @Test
    fun `toEntity maps exercise id from exercise`() {
        val set = Set(repsOrDuration = 10, weight = 80f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        assertEquals(5, set.toEntity(sessionId = 1, order = 0).exerciseId)
    }
}
