package com.looker.kenko

import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.MuscleGroups
import com.looker.kenko.data.repository.ExerciseRepo
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ExerciseRepoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var exerciseRepo: ExerciseRepo

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun streamEmitsNonEmptyListFromPrepopulatedDatabase() = runTest {
        assertTrue(exerciseRepo.stream.first().isNotEmpty())
    }

    @Test
    fun numberOfExerciseMatchesStreamCount() = runTest {
        val count = exerciseRepo.numberOfExercise.first()
        val exercises = exerciseRepo.stream.first()
        assertEquals(exercises.size, count)
    }

    @Test
    fun getReturnsExerciseWithMatchingId() = runTest {
        val exercise = exerciseRepo.stream.first().first()
        val fetched = exerciseRepo.get(exercise.id!!)
        assertNotNull(fetched)
        assertEquals(exercise.id, fetched.id)
        assertEquals(exercise.name, fetched.name)
    }

    @Test
    fun getReturnsNullForNonExistentId() = runTest {
        assertNull(exerciseRepo.get(Int.MAX_VALUE))
    }

    @Test
    fun upsertNewExerciseIncreasesCount() = runTest {
        val countBefore = exerciseRepo.numberOfExercise.first()
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_NewExercise", target = MuscleGroups.Biceps))
        assertEquals(countBefore + 1, exerciseRepo.numberOfExercise.first())
    }

    @Test
    fun upsertNewExerciseAppearsInStream() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_StreamCheck", target = MuscleGroups.Triceps))
        val names = exerciseRepo.stream.first().map { it.name }
        assertTrue("ExerciseRepoTest_StreamCheck" in names)
    }

    @Test
    fun isExerciseAvailableReturnsTrueForPrepopulatedExercise() = runTest {
        val exercise = exerciseRepo.stream.first().first()
        assertTrue(exerciseRepo.isExerciseAvailable(exercise.name))
    }

    @Test
    fun isExerciseAvailableReturnsFalseForUnknownName() = runTest {
        assertFalse(exerciseRepo.isExerciseAvailable("ExerciseRepoTest_NonExistent_XYZ_99999"))
    }

    @Test
    fun isExerciseAvailableReturnsTrueAfterUpsert() = runTest {
        assertFalse(exerciseRepo.isExerciseAvailable("ExerciseRepoTest_AvailCheck"))
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_AvailCheck", target = MuscleGroups.Lats))
        assertTrue(exerciseRepo.isExerciseAvailable("ExerciseRepoTest_AvailCheck"))
    }

    @Test
    fun removeDecreasesExerciseCount() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_ToRemove", target = MuscleGroups.Core))
        val exercise = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_ToRemove" }
        val countBefore = exerciseRepo.numberOfExercise.first()
        exerciseRepo.remove(exercise.id!!)
        assertEquals(countBefore - 1, exerciseRepo.numberOfExercise.first())
    }

    @Test
    fun isExerciseAvailableReturnsFalseAfterRemove() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_RemoveAvail", target = MuscleGroups.Shoulders))
        val exercise = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_RemoveAvail" }
        assertTrue(exerciseRepo.isExerciseAvailable("ExerciseRepoTest_RemoveAvail"))
        exerciseRepo.remove(exercise.id!!)
        assertFalse(exerciseRepo.isExerciseAvailable("ExerciseRepoTest_RemoveAvail"))
    }

    @Test
    fun upsertWithExistingIdUpdatesExercise() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_UpdateOriginal", target = MuscleGroups.Glutes))
        val original = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_UpdateOriginal" }
        exerciseRepo.upsert(original.copy(name = "ExerciseRepoTest_UpdateModified"))
        val fetched = exerciseRepo.get(original.id!!)
        assertEquals("ExerciseRepoTest_UpdateModified", fetched?.name)
    }

    @Test
    fun upsertWithExistingIdDoesNotIncreaseCount() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_UpdateCount", target = MuscleGroups.Calves))
        val original = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_UpdateCount" }
        val countAfterInsert = exerciseRepo.numberOfExercise.first()
        exerciseRepo.upsert(original.copy(name = "ExerciseRepoTest_UpdateCountModified"))
        assertEquals(countAfterInsert, exerciseRepo.numberOfExercise.first())
    }

    @Test
    fun upsertIsometricExercisePreservesFlag() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_Isometric", target = MuscleGroups.Core, isIsometric = true))
        val fetched = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_Isometric" }
        assertTrue(fetched.isIsometric)
    }

    @Test
    fun upsertExerciseWithReferencePreservesReference() = runTest {
        exerciseRepo.upsert(
            Exercise(name = "ExerciseRepoTest_WithRef", target = MuscleGroups.Chest, reference = "https://example.com")
        )
        val fetched = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_WithRef" }
        assertEquals("https://example.com", fetched.reference)
    }

    @Test
    fun getAfterRemoveReturnsNull() = runTest {
        exerciseRepo.upsert(Exercise(name = "ExerciseRepoTest_GetAfterRemove", target = MuscleGroups.Traps))
        val exercise = exerciseRepo.stream.first().first { it.name == "ExerciseRepoTest_GetAfterRemove" }
        val id = exercise.id!!
        exerciseRepo.remove(id)
        assertNull(exerciseRepo.get(id))
    }
}
