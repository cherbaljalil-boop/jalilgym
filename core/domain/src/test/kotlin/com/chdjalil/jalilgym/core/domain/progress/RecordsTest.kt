package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordsTest {

    private var id = 0
    private fun set(weight: Double?, reps: Int?, type: SetType = SetType.WORKING, completed: Boolean = true) =
        WorkoutSet("s${id++}", id, type, weight, reps, null, null, completed, 1L)

    @Test
    fun `session bests ignore warm-ups, unchecked and unloaded sets`() {
        val bests = sessionBests(
            listOf(
                set(140.0, 1, SetType.WARMUP),
                set(100.0, 5),
                set(110.0, 2),
                set(200.0, 1, completed = false),
                set(null, 10),
                set(60.0, 20),
            ),
        ).associateBy { it.type }

        assertThat(bests.getValue(PrType.MAX_WEIGHT).value).isEqualTo(110.0)
        // Epley: 100×5 → 116.7 and 110×2 → 117.3, so the heavier double wins.
        assertThat(bests.getValue(PrType.EST_1RM).value).isWithin(0.01).of(117.33)
        assertThat(bests.getValue(PrType.EST_1RM).set!!.weightKg).isEqualTo(110.0)
        assertThat(bests.getValue(PrType.MAX_VOLUME).value).isEqualTo(100.0 * 5 + 110.0 * 2 + 60.0 * 20)
    }

    @Test
    fun `high-rep sets don't produce a 1RM estimate`() {
        val bests = sessionBests(listOf(set(60.0, 20))).map { it.type }
        assertThat(bests).containsExactly(PrType.MAX_WEIGHT, PrType.MAX_VOLUME)
        assertThat(sessionBests(listOf(set(60.0, 5, completed = false)))).isEmpty()
    }

    @Test
    fun `only improvements beat existing records, missing types are baselines`() {
        val candidates = sessionBests(listOf(set(100.0, 5)))
        val result = newRecords(candidates, mapOf(PrType.MAX_WEIGHT to 100.0, PrType.EST_1RM to 110.0))
            .associate { (candidate, previous) -> candidate.type to previous }

        assertThat(result.keys).containsExactly(PrType.EST_1RM, PrType.MAX_VOLUME)
        assertThat(result[PrType.EST_1RM]).isEqualTo(110.0)
        assertThat(result[PrType.MAX_VOLUME]).isNull()
    }
}
