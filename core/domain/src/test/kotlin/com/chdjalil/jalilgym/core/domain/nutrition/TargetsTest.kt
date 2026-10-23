package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Sex
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class TargetsTest {

    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun mifflinStJeorMatchesReferenceValues() {
        assertThat(mifflinStJeorBmr(Sex.MALE, 80.0, 180.0, 30)).isEqualTo(1780.0)
        assertThat(mifflinStJeorBmr(Sex.FEMALE, 60.0, 165.0, 25)).isWithin(1e-9).of(1345.25)
    }

    @Test
    fun tdeeUsesAgeOnTheGivenDayAndActivityFactor() {
        val profile = BodyProfile(Sex.MALE, LocalDate.of(1996, 10, 24), 180.0, ActivityLevel.MODERATE)
        // Turns 30 tomorrow, so still 29 today.
        assertThat(ageOn(profile.birthDate, today)).isEqualTo(29)
        assertThat(computeTdee(profile, 80.0, today)).isWithin(1e-9).of((1780.0 + 5) * 1.55)
    }

    @Test
    fun cutTakesTwentyPercentWithProteinFatAndCarbs() {
        val result = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 2759.0, 80.0, Sex.MALE)
        assertThat(result.targets).isEqualTo(MacroTargets(calories = 2210, proteinG = 176, carbsG = 239, fatG = 61))
        assertThat(result.clamps).isEmpty()
    }

    @Test
    fun cutDeficitNeverExceeds750() {
        val result = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 4500.0, 120.0, Sex.MALE)
        assertThat(result.targets.calories).isEqualTo(3750)
    }

    @Test
    fun leanBulkAddsTenPercentCappedAt400() {
        assertThat(computeTargets(DietGoal.LEAN_BULK, DietStyle.BALANCED, 2759.0, 80.0, Sex.MALE).targets)
            .isEqualTo(MacroTargets(calories = 3030, proteinG = 144, carbsG = 425, fatG = 84))
        assertThat(computeTargets(DietGoal.LEAN_BULK, DietStyle.BALANCED, 4500.0, 90.0, Sex.MALE).targets.calories)
            .isEqualTo(4900)
    }

    @Test
    fun maintenanceUses27PercentFatAndHighProteinOverridesProtein() {
        val balanced = computeTargets(DietGoal.MAINTAIN, DietStyle.BALANCED, 2500.0, 70.0, Sex.FEMALE).targets
        assertThat(balanced.calories).isEqualTo(2500)
        assertThat(balanced.proteinG).isEqualTo(140)
        assertThat(balanced.fatG).isEqualTo(75)

        val highProtein = computeTargets(DietGoal.MAINTAIN, DietStyle.HIGH_PROTEIN, 2500.0, 70.0, Sex.FEMALE).targets
        assertThat(highProtein.proteinG).isEqualTo(168)
        assertThat(highProtein.fatG).isEqualTo(69)
    }

    @Test
    fun lowCutsAreRaisedToTheCalorieFloor() {
        val result = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 1209.3, 50.0, Sex.FEMALE)
        assertThat(result.targets.calories).isEqualTo(1200)
        assertThat(result.clamps).containsExactly(TargetClamp.CALORIE_FLOOR)
    }

    @Test
    fun deficitIsCappedAtOnePercentOfBodyWeightPerWeek() {
        // 20% of 3,000 is 600 kcal, but 1% of 50 kg a week is only 550 kcal a day.
        val result = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 3000.0, 50.0, Sex.FEMALE)
        assertThat(result.targets.calories).isEqualTo(2450)
        assertThat(result.clamps).containsExactly(TargetClamp.WEEKLY_LOSS_CAP)
    }

    @Test
    fun fatNeverDropsBelowPointSixGramsPerKg() {
        // Heavy person on a low calorie target: 25% of kcal would be under 0.6 g/kg.
        val result = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 1900.0, 150.0, Sex.MALE)
        assertThat(result.targets.fatG).isEqualTo(90)
    }

    @Test
    fun macroCalories() {
        assertThat(caloriesFromMacros(150, 200, 70)).isEqualTo(2030)
    }
}
