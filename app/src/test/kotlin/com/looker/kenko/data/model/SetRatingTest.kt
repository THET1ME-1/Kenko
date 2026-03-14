package com.looker.kenko.data.model

import com.looker.kenko.data.local.model.SetType
import kotlin.test.Test
import kotlin.test.assertEquals

class SetRatingTest {

    private val exercise = Exercise(name = "Bench Press", target = MuscleGroups.Chest, id = 1)

    @Test
    fun `standard set rating is reps times weight times rir modifier`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        // 10 * 100 * 1.0 * 1.04 = 1040
        assertEquals(1040f, set.rating.value, 0.001f)
    }

    @Test
    fun `drop set applies 1_35 type modifier`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.Drop, exercise = exercise, rir = RepsInReserve(2))
        // 10 * 100 * 1.35 * 1.04 = 1404
        assertEquals(1404f, set.rating.value, 0.001f)
    }

    @Test
    fun `rest pause set applies 1_20 type modifier`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.RestPause, exercise = exercise, rir = RepsInReserve(2))
        // 10 * 100 * 1.2 * 1.04 = 1248
        assertEquals(1248f, set.rating.value, 0.001f)
    }

    @Test
    fun `rating with zero weight is zero`() {
        val set = Set(repsOrDuration = 10, weight = 0f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        assertEquals(0f, set.rating.value)
    }

    @Test
    fun `rating with zero reps is zero`() {
        val set = Set(repsOrDuration = 0, weight = 100f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        assertEquals(0f, set.rating.value)
    }

    @Test
    fun `rir 0 applies 1_20 modifier boosting rating`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(0))
        // 10 * 100 * 1.0 * 1.20 = 1200
        assertEquals(1200f, set.rating.value, 0.001f)
    }

    @Test
    fun `rir negative applies 1_20 modifier same as rir 0`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(-5))
        assertEquals(1200f, set.rating.value, 0.001f)
    }

    @Test
    fun `rir 5 applies 0_80 modifier reducing rating`() {
        val set = Set(repsOrDuration = 10, weight = 100f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(5))
        // 10 * 100 * 1.0 * 0.80 = 800
        assertEquals(800f, set.rating.value, 0.001f)
    }

    @Test
    fun `rating result is a Rating instance`() {
        val set = Set(repsOrDuration = 5, weight = 50f, type = SetType.Standard, exercise = exercise, rir = RepsInReserve(2))
        val rating = set.rating
        assertEquals(5 * 50f * 1.0f * 1.04f, rating.value, 0.001f)
    }
}
