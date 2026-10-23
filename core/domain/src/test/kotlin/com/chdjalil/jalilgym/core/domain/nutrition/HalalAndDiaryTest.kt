package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HalalAndDiaryTest {

    @Test
    fun halalLabelsCertify() {
        assertThat(halalStatusOf(listOf("en:organic", "en:halal"), "chicken, salt")).isEqualTo(HalalStatus.CERTIFIED)
        assertThat(halalStatusOf(listOf("fr:halal-certifie-par-avs"), null)).isEqualTo(HalalStatus.CERTIFIED)
    }

    @Test
    fun flaggedIngredientsAndLabelsFlag() {
        assertThat(halalStatusOf(emptyList(), "Wheat flour, pork fat, salt")).isEqualTo(HalalStatus.FLAGGED)
        assertThat(halalStatusOf(emptyList(), "sugar, GELATINE, citric acid")).isEqualTo(HalalStatus.FLAGGED)
        assertThat(halalStatusOf(emptyList(), "cocoa, rum (2%)")).isEqualTo(HalalStatus.FLAGGED)
        assertThat(halalStatusOf(listOf("en:contains-pork"), null)).isEqualTo(HalalStatus.FLAGGED)
    }

    @Test
    fun wholeWordsAndStatedOriginsDontFlag() {
        assertThat(halalStatusOf(emptyList(), "graham crackers, hamburger buns")).isEqualTo(HalalStatus.UNKNOWN)
        assertThat(halalStatusOf(emptyList(), "beef gelatin, sugar alcohol (maltitol), red wine vinegar"))
            .isEqualTo(HalalStatus.UNKNOWN)
        assertThat(halalStatusOf(emptyList(), null)).isEqualTo(HalalStatus.UNKNOWN)
    }

    @Test
    fun filters() {
        assertThat(HalalFilter.ALL.allows(HalalStatus.FLAGGED)).isTrue()
        assertThat(HalalFilter.HIDE_FLAGGED.allows(HalalStatus.FLAGGED)).isFalse()
        assertThat(HalalFilter.HIDE_FLAGGED.allows(HalalStatus.UNKNOWN)).isTrue()
        assertThat(HalalFilter.CERTIFIED_ONLY.allows(HalalStatus.UNKNOWN)).isFalse()
        assertThat(HalalFilter.CERTIFIED_ONLY.allows(HalalStatus.USER_MARKED)).isTrue()
    }

    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun loggingStreaks() {
        val days = listOf(0L, 1, 2, 5, 6, 7, 8).map { today.minusDays(it) }
        assertThat(bestLoggingStreak(days)).isEqualTo(4)
        assertThat(currentLoggingStreak(days, today)).isEqualTo(3)
        // Nothing logged yet today: yesterday's run still counts.
        assertThat(currentLoggingStreak(days.drop(1), today)).isEqualTo(2)
        assertThat(currentLoggingStreak(emptyList(), today)).isEqualTo(0)
        assertThat(bestLoggingStreak(emptyList())).isEqualTo(0)
    }

    @Test
    fun targetHits() {
        val targets = MacroTargets(2000, 150, 200, 70)
        assertThat(hitTargets(Nutrients(1850.0, 140.0, 0.0, 0.0), targets)).isTrue()
        assertThat(hitTargets(Nutrients(1750.0, 160.0, 0.0, 0.0), targets)).isFalse()
        assertThat(hitTargets(Nutrients(2000.0, 130.0, 0.0, 0.0), targets)).isFalse()
    }

    @Test
    fun nutrientMath() {
        val per100 = Nutrients(200.0, 10.0, 20.0, 5.0, fiberG = 3.0)
        val half = per100 * 0.5
        assertThat(half.kcal).isEqualTo(100.0)
        assertThat(half.sodiumMg).isNull()
        val sum = half + Nutrients(50.0, 1.0, 1.0, 1.0, sodiumMg = 100.0)
        assertThat(sum.fiberG).isEqualTo(1.5)
        assertThat(sum.sodiumMg).isEqualTo(100.0)
        assertThat(progressFraction(150.0, 100)).isEqualTo(1f)
    }
}
