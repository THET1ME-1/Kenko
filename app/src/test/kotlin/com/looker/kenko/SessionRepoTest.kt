package com.looker.kenko

import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SessionRepoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionRepo: SessionRepo

    @Inject
    lateinit var planRepo: PlanRepo

    @Inject
    lateinit var exerciseRepo: ExerciseRepo

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun getSessionIdOrCreateReturnsSameIdOnSecondCallSameDate() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_SameId")
        planRepo.setCurrent(planId)
        val date = localDate.minus(100, DateTimeUnit.DAY)
        val id1 = sessionRepo.getSessionIdOrCreate(date)
        val id2 = sessionRepo.getSessionIdOrCreate(date)
        assertEquals(id1, id2)
    }

    @Test
    fun getSessionIdOrCreateReturnsDifferentIdsForDifferentDates() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_DiffId")
        planRepo.setCurrent(planId)
        val date1 = localDate.minus(200, DateTimeUnit.DAY)
        val date2 = localDate.minus(201, DateTimeUnit.DAY)
        val id1 = sessionRepo.getSessionIdOrCreate(date1)
        val id2 = sessionRepo.getSessionIdOrCreate(date2)
        assertTrue(id1 != id2)
    }

    @Test
    fun streamByDateEmitsNullForDateWithNoSession() = runTest {
        val farPastDate = LocalDate(2000, 1, 1)
        val session = sessionRepo.streamByDate(farPastDate).first()
        assertNull(session)
    }

    @Test
    fun streamByDateEmitsSessionAfterCreation() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_StreamAfterCreate")
        planRepo.setCurrent(planId)
        val date = localDate.minus(300, DateTimeUnit.DAY)
        sessionRepo.getSessionIdOrCreate(date)
        val session = sessionRepo.streamByDate(date).first()
        assertNotNull(session)
        assertEquals(date, session.date)
    }

    @Test
    fun addSetIncreasesSetCountForSession() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_AddSet")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(400, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        val countBefore = sessionRepo.getSets(sessionId).size
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2)))
        assertEquals(countBefore + 1, sessionRepo.getSets(sessionId).size)
    }

    @Test
    fun addMultipleSetsAllAppearInGetSets() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_MultiSet")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(500, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        val initialCount = sessionRepo.getSets(sessionId).size
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2)))
        sessionRepo.addSet(sessionId, Set(8, 90f, SetType.Drop, exercise, RepsInReserve(1)))
        sessionRepo.addSet(sessionId, Set(6, 100f, SetType.RestPause, exercise, RepsInReserve(0)))
        assertEquals(initialCount + 3, sessionRepo.getSets(sessionId).size)
    }

    @Test
    fun removeSetDecreasesCount() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_RemoveSet")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val sessionId = sessionRepo.getSessionIdOrCreate(localDate)
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2)))
        val countBefore = sessionRepo.getSets(sessionId).size
        val set = sessionRepo.getSets(sessionId).last()
        sessionRepo.removeSet(set.id!!)
        assertEquals(countBefore - 1, sessionRepo.getSets(sessionId).size)
    }

    @Test
    fun addSetWithExplicitParametersCreatesCorrectSet() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_ExplicitAdd")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(700, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        val countBefore = sessionRepo.getSets(sessionId).size
        sessionRepo.addSet(
            sessionId = sessionId,
            exerciseId = exercise.id!!,
            weight = 95f,
            reps = 5,
            setType = SetType.Standard,
            rir = RepsInReserve(3),
        )
        val sets = sessionRepo.getSets(sessionId)
        assertEquals(countBefore + 1, sets.size)
        val addedSet = sets.last()
        assertEquals(5, addedSet.repsOrDuration)
        assertEquals(95f, addedSet.weight)
    }

    @Test
    fun sessionHasPlanIdOfCurrentPlan() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_PlanId")
        planRepo.setCurrent(planId)
        val date = localDate.minus(800, DateTimeUnit.DAY)
        sessionRepo.getSessionIdOrCreate(date)
        val session = sessionRepo.streamByDate(date).first()
        assertNotNull(session)
        assertEquals(planId, session.planId)
    }

    @Test
    fun setsCountFlowIncreasesAfterAddSet() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_SetsCount")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(900, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        val countBefore = sessionRepo.setsCount.first()
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Standard, exercise, RepsInReserve(2)))
        assertEquals(countBefore + 1, sessionRepo.setsCount.first())
    }

    @Test
    fun getSetsReturnsEmptyListForNewSession() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_EmptySets")
        planRepo.setCurrent(planId)
        val date = localDate.minus(1000, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        assertTrue(sessionRepo.getSets(sessionId).isEmpty())
    }

    @Test
    fun setRepsAndWeightArePreservedAfterAdd() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_RepsWeight")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(1100, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        sessionRepo.addSet(sessionId, Set(15, 42.5f, SetType.Standard, exercise, RepsInReserve(2)))
        val sets = sessionRepo.getSets(sessionId)
        val added = sets.last()
        assertEquals(15, added.repsOrDuration)
        assertEquals(42.5f, added.weight)
    }

    @Test
    fun setRirIsPreservedAfterAdd() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_Rir")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(1200, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Standard, exercise, RepsInReserve(4)))
        val sets = sessionRepo.getSets(sessionId)
        assertEquals(RepsInReserve(4), sets.last().rir)
    }

    @Test
    fun setTypeIsPreservedAfterAdd() = runTest {
        val planId = planRepo.createPlan("SessionRepoTest_SetType")
        planRepo.setCurrent(planId)
        val exercise = exerciseRepo.stream.first().first()
        val date = localDate.minus(1300, DateTimeUnit.DAY)
        val sessionId = sessionRepo.getSessionIdOrCreate(date)
        sessionRepo.addSet(sessionId, Set(10, 80f, SetType.Drop, exercise, RepsInReserve(2)))
        val sets = sessionRepo.getSets(sessionId)
        assertEquals(SetType.Drop, sets.last().type)
    }
}
