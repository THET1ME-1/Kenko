package com.looker.kenko.data.model

import com.looker.kenko.data.local.model.SetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class SessionTest {

    private val date = LocalDate(2025, 3, 14)
    private val rir = RepsInReserve(2)
    private val exercise1 = Exercise("Bench Press", MuscleGroups.Chest, id = 1)
    private val exercise2 = Exercise("Squat", MuscleGroups.Quads, id = 2)
    private val exercise3 = Exercise("Deadlift", MuscleGroups.Hamstrings, id = 3)

    @Test
    fun `performExercises is empty for session with no sets`() {
        val session = Session(date = date, sets = emptyList(), planId = 1)
        assertTrue(session.performExercises.isEmpty())
    }

    @Test
    fun `performExercises returns single exercise for multiple sets of same exercise`() {
        val set1 = Set(10, 100f, SetType.Standard, exercise1, rir)
        val set2 = Set(10, 80f, SetType.Standard, exercise1, rir)
        val session = Session(date = date, sets = listOf(set1, set2), planId = 1)
        assertEquals(1, session.performExercises.size)
        assertEquals(exercise1, session.performExercises.first())
    }

    @Test
    fun `performExercises returns all distinct exercises`() {
        val set1 = Set(10, 100f, SetType.Standard, exercise1, rir)
        val set2 = Set(10, 80f, SetType.Standard, exercise2, rir)
        val session = Session(date = date, sets = listOf(set1, set2), planId = 1)
        assertEquals(2, session.performExercises.size)
        assertTrue(exercise1 in session.performExercises)
        assertTrue(exercise2 in session.performExercises)
    }

    @Test
    fun `performExercises preserves first-appearance order`() {
        val set1 = Set(10, 100f, SetType.Standard, exercise1, rir)
        val set2 = Set(10, 80f, SetType.Standard, exercise2, rir)
        val set3 = Set(8, 100f, SetType.Standard, exercise1, rir)
        val session = Session(date = date, sets = listOf(set1, set2, set3), planId = 1)
        assertEquals(listOf(exercise1, exercise2), session.performExercises)
    }

    @Test
    fun `performExercises with three distinct exercises returns three`() {
        val sets = listOf(
            Set(10, 100f, SetType.Standard, exercise1, rir),
            Set(10, 80f, SetType.Standard, exercise2, rir),
            Set(5, 120f, SetType.Standard, exercise3, rir),
        )
        val session = Session(date = date, sets = sets, planId = 1)
        assertEquals(3, session.performExercises.size)
    }

    @Test
    fun `session can have null planId`() {
        val session = Session(date = date, sets = emptyList(), planId = null)
        assertEquals(null, session.planId)
    }

    @Test
    fun `session with null id is valid`() {
        val session = Session(date = date, sets = emptyList(), planId = 1, id = null)
        assertEquals(null, session.id)
    }

    @Test
    fun `performExercises with interleaved exercises deduplicates correctly`() {
        val sets = listOf(
            Set(10, 100f, SetType.Standard, exercise1, rir),
            Set(10, 80f, SetType.Standard, exercise2, rir),
            Set(8, 100f, SetType.Standard, exercise1, rir),
            Set(6, 80f, SetType.Standard, exercise2, rir),
        )
        val session = Session(date = date, sets = sets, planId = 1)
        assertEquals(listOf(exercise1, exercise2), session.performExercises)
    }
}
