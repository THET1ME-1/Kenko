package com.looker.kenko.data.local.model

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseEntityTest {

    @Test
    fun `toExternal maps name correctly`() {
        val entity = ExerciseEntity(name = "Bench Press", target = MuscleGroups.Chest, id = 1)
        assertEquals("Bench Press", entity.toExternal().name)
    }

    @Test
    fun `toExternal maps target correctly`() {
        val entity = ExerciseEntity(name = "Squat", target = MuscleGroups.Quads, id = 2)
        assertEquals(MuscleGroups.Quads, entity.toExternal().target)
    }

    @Test
    fun `toExternal maps id correctly`() {
        val entity = ExerciseEntity(name = "Curl", target = MuscleGroups.Biceps, id = 42)
        assertEquals(42, entity.toExternal().id)
    }

    @Test
    fun `toExternal preserves null reference`() {
        val entity = ExerciseEntity(name = "Plank", target = MuscleGroups.Core, reference = null, id = 1)
        assertNull(entity.toExternal().reference)
    }

    @Test
    fun `toExternal preserves non-null reference`() {
        val entity = ExerciseEntity(name = "Curl", target = MuscleGroups.Biceps, reference = "https://example.com", id = 1)
        assertEquals("https://example.com", entity.toExternal().reference)
    }

    @Test
    fun `toExternal preserves isIsometric true`() {
        val entity = ExerciseEntity(name = "Plank", target = MuscleGroups.Core, isIsometric = true, id = 1)
        assertEquals(true, entity.toExternal().isIsometric)
    }

    @Test
    fun `toExternal preserves isIsometric false`() {
        val entity = ExerciseEntity(name = "Curl", target = MuscleGroups.Biceps, isIsometric = false, id = 1)
        assertEquals(false, entity.toExternal().isIsometric)
    }

    @Test
    fun `toEntity maps name correctly`() {
        val exercise = Exercise(name = "Deadlift", target = MuscleGroups.Hamstrings, id = 5)
        assertEquals("Deadlift", exercise.toEntity().name)
    }

    @Test
    fun `toEntity maps target correctly`() {
        val exercise = Exercise(name = "Deadlift", target = MuscleGroups.Hamstrings, id = 5)
        assertEquals(MuscleGroups.Hamstrings, exercise.toEntity().target)
    }

    @Test
    fun `toEntity with null id uses 0`() {
        val exercise = Exercise(name = "Curl", target = MuscleGroups.Biceps, id = null)
        assertEquals(0, exercise.toEntity().id)
    }

    @Test
    fun `toEntity with explicit id preserves it`() {
        val exercise = Exercise(name = "Squat", target = MuscleGroups.Quads, id = 7)
        assertEquals(7, exercise.toEntity().id)
    }

    @Test
    fun `round trip toEntity then toExternal preserves all data`() {
        val exercise = Exercise(
            name = "Deadlift",
            target = MuscleGroups.Hamstrings,
            reference = "https://ref.com",
            isIsometric = false,
            id = 99,
        )
        assertEquals(exercise, exercise.toEntity().toExternal())
    }

    @Test
    fun `round trip works for isometric exercise with null reference`() {
        val exercise = Exercise(
            name = "Plank",
            target = MuscleGroups.Core,
            reference = null,
            isIsometric = true,
            id = 3,
        )
        assertEquals(exercise, exercise.toEntity().toExternal())
    }

    @Test
    fun `toEntity sets default id to 0 when exercise has no id`() {
        val exercise = Exercise(name = "Push-up", target = MuscleGroups.Chest)
        assertEquals(0, exercise.toEntity().id)
    }
}
