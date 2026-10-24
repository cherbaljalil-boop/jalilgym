package com.chdjalil.jalilgym.feature.widgets

import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.testing.repository.FakeBodyRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeNutritionRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeProgramRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeProgressRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeWaterRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TodayWidgetStateTest {

    private val template = Program(
        "tpl", "PPL", ProgramType.PPL, 3, true, null,
        listOf(ProgramDay("push", "tpl", 0, "Push", emptyList()), ProgramDay("pull", "tpl", 1, "Pull", emptyList())),
    )
    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()

    @Test
    fun `combines the next workout, food and water`() = runTest {
        val programs = FakeProgramRepository(templates = listOf(template))
        val nutrition = FakeNutritionRepository()
        val water = FakeWaterRepository()
        val preferences = FakeUserPreferencesRepository()
        val body = FakeBodyRepository()
        val sources = TodayWidgetSources(
            preferences, programs, FakeWorkoutRepository(programs = programs), FakeProgressRepository(), nutrition, water, body,
        )

        assertThat(sources.observeState(today, zone).first().workoutName).isNull()

        programs.activate(programs.copyProgram("tpl"))
        nutrition.savePlan(DietGoal.MAINTAIN, DietStyle.BALANCED, false, MacroTargets(2400, 160, 280, 70), isManualOverride = false)
        nutrition.quickAdd(today, MealType.LUNCH, "Bowl", Nutrients(650.0, 45.0, 70.0, 18.0))
        water.add(today, 500)
        body.logWeight(today, 80.0)
        preferences.setUnitSystem(UnitSystem.IMPERIAL)

        val state = sources.observeState(today, zone).first()
        assertThat(state.workoutName).isEqualTo("Push")
        assertThat(state.workoutInProgress).isFalse()
        assertThat(state.kcalEaten).isEqualTo(650)
        assertThat(state.kcalTarget).isEqualTo(2400)
        assertThat(state.proteinTarget).isEqualTo(160)
        assertThat(state.waterMl).isEqualTo(500)
        assertThat(state.waterGoalMl).isEqualTo(2_750)
        assertThat(state.waterStepMl).isEqualTo(237)
    }
}
