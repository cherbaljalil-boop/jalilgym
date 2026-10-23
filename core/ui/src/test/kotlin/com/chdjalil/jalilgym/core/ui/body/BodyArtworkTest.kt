package com.chdjalil.jalilgym.core.ui.body

import com.chdjalil.jalilgym.core.model.Muscle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BodyArtworkTest {

    @Test
    fun everyMuscleIsDrawnOnAtLeastOneSide() {
        val drawn = BodyArtwork.front.keys + BodyArtwork.back.keys
        assertThat(drawn).containsExactlyElementsIn(Muscle.entries)
    }

    @Test
    fun regionsAreSymmetricPairs() {
        (BodyArtwork.front.values + BodyArtwork.back.values).forEach { paths ->
            assertThat(paths.size % 2).isEqualTo(0)
        }
    }

    @Test
    fun targetIntensitiesFavorPrimaryMuscles() {
        val intensities = muscleTargetIntensities(
            primary = listOf(Muscle.CHEST),
            secondary = listOf(Muscle.TRICEPS, Muscle.CHEST),
        )
        assertThat(intensities).containsExactly(Muscle.CHEST, 1f, Muscle.TRICEPS, 0.5f)
    }
}
