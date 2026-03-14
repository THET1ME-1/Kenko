package com.looker.kenko

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.Labels.Difficulty
import com.looker.kenko.data.model.Labels.Equipment
import com.looker.kenko.data.model.Labels.Focus
import com.looker.kenko.data.model.Labels.Time
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PlanRepoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var planRepo: PlanRepo

    @Inject
    lateinit var exerciseRepo: ExerciseRepo

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun createPlanReturnsPositiveId() = runTest {
        val id = planRepo.createPlan("PlanRepoTest_ReturnId")
        assertTrue(id > 0)
    }

    @Test
    fun createdPlanAppearsInPlansFlow() = runTest {
        planRepo.createPlan("PlanRepoTest_AppearInFlow")
        val names = planRepo.plans.first().map { it.name }
        assertTrue("PlanRepoTest_AppearInFlow" in names)
    }

    @Test
    fun planNameExistsReturnsTrueForCreatedPlan() = runTest {
        planRepo.createPlan("PlanRepoTest_NameExists")
        assertTrue(planRepo.planNameExists("PlanRepoTest_NameExists"))
    }

    @Test
    fun planNameExistsReturnsFalseForUnknownPlan() = runTest {
        assertFalse(planRepo.planNameExists("PlanRepoTest_NeverCreated_XYZ_99999"))
    }

    @Test
    fun planByIdReturnsPlanWithMatchingId() = runTest {
        val id = planRepo.createPlan("PlanRepoTest_GetById")
        val plan = planRepo.plan(id)
        assertNotNull(plan)
        assertEquals(id, plan.id)
        assertEquals("PlanRepoTest_GetById", plan.name)
    }

    @Test
    fun planByIdReturnsNullForNonExistentId() = runTest {
        assertNull(planRepo.plan(Int.MAX_VALUE))
    }

    @Test
    fun createPlanWithAllMetadataPreservesFields() = runTest {
        val id = planRepo.createPlan(
            name = "PlanRepoTest_WithMeta",
            description = "A test plan",
            difficulty = Difficulty.INTERMEDIATE,
            focus = Focus.HYPERTROPHY,
            equipment = Equipment.FULL_GYM,
            time = Time.NORMAL,
        )
        val plan = planRepo.plan(id)
        assertNotNull(plan)
        assertEquals("A test plan", plan.description)
        assertEquals(Difficulty.INTERMEDIATE, plan.difficulty)
        assertEquals(Focus.HYPERTROPHY, plan.focus)
        assertEquals(Equipment.FULL_GYM, plan.equipment)
        assertEquals(Time.NORMAL, plan.time)
    }

    @Test
    fun updatePlanPreservesModifications() = runTest {
        val id = planRepo.createPlan("PlanRepoTest_UpdateOriginal")
        val original = planRepo.plan(id)!!
        val updated = original.copy(name = "PlanRepoTest_UpdateModified", description = "Updated")
        planRepo.updatePlan(updated)
        val fetched = planRepo.plan(id)
        assertEquals("PlanRepoTest_UpdateModified", fetched?.name)
        assertEquals("Updated", fetched?.description)
    }

    @Test
    fun addItemIncreasesItemCount() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_AddItem")
        val exercise = exerciseRepo.stream.first().first()
        val itemsBefore = planRepo.getPlanItems(planId).size
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = planId))
        assertEquals(itemsBefore + 1, planRepo.getPlanItems(planId).size)
    }

    @Test
    fun addItemsForSpecificDayAreReturnedByDayFilter() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_DayFilter")
        val exercise = exerciseRepo.stream.first().first()
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.TUESDAY, exercise = exercise, planId = planId))
        val tuesdayItems = planRepo.getPlanItems(planId, DayOfWeek.TUESDAY)
        assertTrue(tuesdayItems.isNotEmpty())
        assertTrue(tuesdayItems.all { it.dayOfWeek == DayOfWeek.TUESDAY })
    }

    @Test
    fun getPlanItemsByDayExcludesOtherDays() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_DayExclude")
        val exercises = exerciseRepo.stream.first().take(2)
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercises[0], planId = planId))
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.WEDNESDAY, exercise = exercises[1], planId = planId))
        val mondayItems = planRepo.getPlanItems(planId, DayOfWeek.MONDAY)
        assertTrue(mondayItems.all { it.dayOfWeek == DayOfWeek.MONDAY })
        assertFalse(mondayItems.any { it.dayOfWeek == DayOfWeek.WEDNESDAY })
    }

    @Test
    fun removeItemDecreasesItemCount() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_RemoveItem")
        val exercise = exerciseRepo.stream.first().first()
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.FRIDAY, exercise = exercise, planId = planId))
        val item = planRepo.getPlanItems(planId).first()
        val countBefore = planRepo.getPlanItems(planId).size
        planRepo.removeItem(item.id!!)
        assertEquals(countBefore - 1, planRepo.getPlanItems(planId).size)
    }

    @Test
    fun deletePlanRemovesItFromFlow() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_Delete")
        assertTrue(planRepo.plans.first().any { it.id == planId })
        planRepo.deletePlan(planId)
        assertFalse(planRepo.plans.first().any { it.id == planId })
    }

    @Test
    fun deletePlanRemovesAllItsItems() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_DeleteItems")
        val exercise = exerciseRepo.stream.first().first()
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = planId))
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.WEDNESDAY, exercise = exercise, planId = planId))
        planRepo.deletePlan(planId)
        assertEquals(0, planRepo.getPlanItems(planId).size)
    }

    @Test
    fun setCurrentMakesPlanActive() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_SetCurrent")
        planRepo.setCurrent(planId)
        val currentPlan = planRepo.current.first()
        assertNotNull(currentPlan)
        assertEquals(planId, currentPlan.id)
        assertTrue(currentPlan.isActive)
    }

    @Test
    fun setCurrentDeactivatesPreviousActivePlan() = runTest {
        val planId1 = planRepo.createPlan("PlanRepoTest_PrevActive1")
        val planId2 = planRepo.createPlan("PlanRepoTest_PrevActive2")
        planRepo.setCurrent(planId1)
        planRepo.setCurrent(planId2)
        val allPlans = planRepo.plans.first()
        val plan1 = allPlans.firstOrNull { it.id == planId1 }
        assertFalse(plan1?.isActive ?: true)
    }

    @Test
    fun deleteEmptyPlansRemovesPlansWithNoItems() = runTest {
        planRepo.createPlan("PlanRepoTest_EmptyToDelete")
        val emptyPlanExists = planRepo.plans.first().any { it.name == "PlanRepoTest_EmptyToDelete" }
        assertTrue(emptyPlanExists)
        planRepo.deleteEmptyPlans()
        val existsAfter = planRepo.plans.first().any { it.name == "PlanRepoTest_EmptyToDelete" }
        assertFalse(existsAfter)
    }

    @Test
    fun deleteEmptyPlansDoesNotRemovePlanWithItems() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_NotEmpty")
        val exercise = exerciseRepo.stream.first().first()
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = planId))
        planRepo.deleteEmptyPlans()
        assertNotNull(planRepo.plan(planId))
    }

    @Test
    fun planItemsFlowByIdEmitsCorrectItems() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_FlowById")
        val exercise = exerciseRepo.stream.first().first()
        planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.THURSDAY, exercise = exercise, planId = planId))
        val items = planRepo.planItems(planId).first()
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.planId == planId })
    }

    @Test
    fun addItemWithDeletedExerciseIdThrows() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_NullExerciseId")
        val exerciseWithNullId = Exercise(name = "Ghost", target = MuscleGroups.Core, id = null)
        kotlin.test.assertFails {
            planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exerciseWithNullId, planId = planId))
        }
    }

    @Test
    fun plansFlowInitiallyContainsPrepopulatedPlansOrEmpty() = runTest {
        val plans = planRepo.plans.first()
        assertNotNull(plans)
    }

    @Test
    fun newlyCreatedPlanIsNotActive() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_NotActiveByDefault")
        val plan = planRepo.plan(planId)
        assertNotNull(plan)
        assertFalse(plan.isActive)
    }

    @Test
    fun planStatReflectsAddedItems() = runTest {
        val planId = planRepo.createPlan("PlanRepoTest_StatCheck")
        val exercises = exerciseRepo.stream.first().take(3)
        exercises.forEach { exercise ->
            planRepo.addItem(PlanItem(dayOfWeek = DayOfWeek.MONDAY, exercise = exercise, planId = planId))
        }
        val plan = planRepo.plans.first().first { it.id == planId }
        assertEquals(3, plan.stat.exercises)
    }
}
