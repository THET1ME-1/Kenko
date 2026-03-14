package com.looker.kenko.data.local.model

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Labels.Difficulty
import com.looker.kenko.data.model.Labels.Equipment
import com.looker.kenko.data.model.Labels.Focus
import com.looker.kenko.data.model.Labels.Time
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.Plan
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.PlanStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.datetime.DayOfWeek

class PlanEntityTest {

    @Test
    fun `PlanEntity toExternal maps name correctly`() {
        val entity = PlanEntity(name = "PPL", description = null, difficulty = null, focus = null, equipment = null, time = null, id = 1)
        assertEquals("PPL", entity.toExternal(isActive = false, stat = PlanStat(0, 0)).name)
    }

    @Test
    fun `PlanEntity toExternal maps isActive correctly`() {
        val entity = PlanEntity(name = "Test", description = null, difficulty = null, focus = null, equipment = null, time = null, id = 1)
        assertEquals(true, entity.toExternal(isActive = true, stat = PlanStat(0, 0)).isActive)
        assertEquals(false, entity.toExternal(isActive = false, stat = PlanStat(0, 0)).isActive)
    }

    @Test
    fun `PlanEntity toExternal maps all label fields`() {
        val entity = PlanEntity(
            name = "PPL",
            description = "A push pull legs plan",
            difficulty = Difficulty.INTERMEDIATE,
            focus = Focus.HYPERTROPHY,
            equipment = Equipment.FULL_GYM,
            time = Time.NORMAL,
            id = 1,
        )
        val plan = entity.toExternal(isActive = true, stat = PlanStat(21, 5))
        assertEquals("A push pull legs plan", plan.description)
        assertEquals(Difficulty.INTERMEDIATE, plan.difficulty)
        assertEquals(Focus.HYPERTROPHY, plan.focus)
        assertEquals(Equipment.FULL_GYM, plan.equipment)
        assertEquals(Time.NORMAL, plan.time)
    }

    @Test
    fun `PlanEntity toExternal with all null labels preserves nulls`() {
        val entity = PlanEntity(name = "Simple", description = null, difficulty = null, focus = null, equipment = null, time = null)
        val plan = entity.toExternal(isActive = false, stat = PlanStat(0, 0))
        assertNull(plan.description)
        assertNull(plan.difficulty)
        assertNull(plan.focus)
        assertNull(plan.equipment)
        assertNull(plan.time)
    }

    @Test
    fun `PlanEntity toExternal passes stat through`() {
        val entity = PlanEntity(name = "Test", description = null, difficulty = null, focus = null, equipment = null, time = null, id = 1)
        val stat = PlanStat(exercises = 10, workDays = 4)
        val plan = entity.toExternal(isActive = false, stat = stat)
        assertEquals(10, plan.stat.exercises)
        assertEquals(4, plan.stat.workDays)
    }

    @Test
    fun `Plan toEntity maps name correctly`() {
        val plan = Plan(name = "Upper Lower", description = null, difficulty = null, focus = null, equipment = null, time = null, isActive = false)
        assertEquals("Upper Lower", plan.toEntity().name)
    }

    @Test
    fun `Plan toEntity with null id uses 0`() {
        val plan = Plan(name = "Test", description = null, difficulty = null, focus = null, equipment = null, time = null, isActive = false, id = null)
        assertEquals(0, plan.toEntity().id)
    }

    @Test
    fun `Plan toEntity with explicit id preserves it`() {
        val plan = Plan(name = "Test", description = null, difficulty = null, focus = null, equipment = null, time = null, isActive = false, id = 5)
        assertEquals(5, plan.toEntity().id)
    }

    @Test
    fun `Plan toEntity maps all label fields`() {
        val plan = Plan(
            name = "Test",
            description = "desc",
            difficulty = Difficulty.BEGINNER,
            focus = Focus.STRENGTH,
            equipment = Equipment.DUMBBELLS,
            time = Time.QUICK,
            isActive = false,
        )
        val entity = plan.toEntity()
        assertEquals("desc", entity.description)
        assertEquals(Difficulty.BEGINNER, entity.difficulty)
        assertEquals(Focus.STRENGTH, entity.focus)
        assertEquals(Equipment.DUMBBELLS, entity.equipment)
        assertEquals(Time.QUICK, entity.time)
    }

    @Test
    fun `PlanItem toEntity with null exercise id throws IllegalArgumentException`() {
        val exercise = Exercise(name = "Curl", target = MuscleGroups.Biceps, id = null)
        val item = PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = 1)
        assertFailsWith<IllegalArgumentException> {
            item.toEntity()
        }
    }

    @Test
    fun `PlanItem toEntity maps dayOfWeek as iso number`() {
        val exercise = Exercise(name = "Squat", target = MuscleGroups.Quads, id = 3)
        val item = PlanItem(dayOfWeek = DayOfWeek.WEDNESDAY, exercise = exercise, planId = 2)
        assertEquals(3, item.toEntity().dayOfWeek) // Wednesday = ISO 3
    }

    @Test
    fun `PlanItem toEntity maps Monday as iso number 1`() {
        val exercise = Exercise(name = "Bench", target = MuscleGroups.Chest, id = 1)
        val item = PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = 1)
        assertEquals(1, item.toEntity().dayOfWeek)
    }

    @Test
    fun `PlanItem toEntity maps Sunday as iso number 7`() {
        val exercise = Exercise(name = "Cardio", target = MuscleGroups.Core, id = 2)
        val item = PlanItem(dayOfWeek = DayOfWeek.SUNDAY, exercise = exercise, planId = 1)
        assertEquals(7, item.toEntity().dayOfWeek)
    }

    @Test
    fun `PlanItem toEntity maps planId correctly`() {
        val exercise = Exercise(name = "Squat", target = MuscleGroups.Quads, id = 3)
        val item = PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = 42)
        assertEquals(42, item.toEntity().planId)
    }

    @Test
    fun `PlanItem toEntity with null id uses 0`() {
        val exercise = Exercise(name = "Squat", target = MuscleGroups.Quads, id = 3)
        val item = PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = 1, id = null)
        assertEquals(0L, item.toEntity().id)
    }

    @Test
    fun `PlanDayEntity toExternal with existing exercise returns it`() {
        val exercise = Exercise(name = "Deadlift", target = MuscleGroups.Hamstrings, id = 10)
        val dayEntity = PlanDayEntity(planId = 1, exerciseId = 10, dayOfWeek = 1)
        val item = dayEntity.toExternal { exercise }
        assertEquals(exercise, item.exercise)
    }

    @Test
    fun `PlanDayEntity toExternal with null exercise block returns DefaultExercise`() {
        val dayEntity = PlanDayEntity(planId = 1, exerciseId = 999, dayOfWeek = 5)
        val item = dayEntity.toExternal { null }
        assertEquals(DefaultExercise, item.exercise)
    }

    @Test
    fun `PlanDayEntity toExternal maps dayOfWeek correctly`() {
        val exercise = Exercise(name = "Bench", target = MuscleGroups.Chest, id = 1)
        val dayEntity = PlanDayEntity(planId = 1, exerciseId = 1, dayOfWeek = 5) // Friday
        val item = dayEntity.toExternal { exercise }
        assertEquals(DayOfWeek.FRIDAY, item.dayOfWeek)
    }

    @Test
    fun `PlanDayEntity toExternal maps planId correctly`() {
        val exercise = Exercise(name = "Bench", target = MuscleGroups.Chest, id = 1)
        val dayEntity = PlanDayEntity(planId = 7, exerciseId = 1, dayOfWeek = 1)
        val item = dayEntity.toExternal { exercise }
        assertEquals(7, item.planId)
    }

    @Test
    fun `DefaultExercise has expected name`() {
        assertEquals("Exercise Deleted", DefaultExercise.name)
    }

    @Test
    fun `DefaultExercise targets Core muscle group`() {
        assertEquals(MuscleGroups.Core, DefaultExercise.target)
    }

    @Test
    fun `DefaultExercise has null id`() {
        assertNull(DefaultExercise.id)
    }
}
