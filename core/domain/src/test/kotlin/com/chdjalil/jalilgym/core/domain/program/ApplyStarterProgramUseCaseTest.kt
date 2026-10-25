package com.chdjalil.jalilgym.core.domain.program

import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.testing.repository.FakeProgramRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApplyStarterProgramUseCaseTest {

    private val template = Program(
        id = ProgramTemplates.PPL_3,
        name = "Push / Pull / Legs",
        type = ProgramType.PPL,
        daysPerWeek = 3,
        isTemplate = true,
        sourceTemplateId = null,
        days = listOf(ProgramDay("t-push", ProgramTemplates.PPL_3, 0, "Push", emptyList())),
    )
    private val programs = FakeProgramRepository(templates = listOf(template))

    private fun useCase(preferences: UserPreferences) =
        FakeUserPreferencesRepository(preferences).let { it to ApplyStarterProgramUseCase(it, programs) }

    @Test
    fun `copies and activates the chosen template once`() = runTest {
        val (preferences, apply) = useCase(UserPreferences(onboardingComplete = true, starterProgram = StarterProgram.PPL_3))

        val id = apply("My program")

        assertThat(id).isNotNull()
        assertThat(programs.activeProgramId).isEqualTo(id)
        val copy = programs.currentPrograms.single { it.id == id }
        assertThat(copy.isTemplate).isFalse()
        assertThat(copy.sourceTemplateId).isEqualTo(ProgramTemplates.PPL_3)
        assertThat(preferences.current.starterProgramApplied).isTrue()

        assertThat(apply("My program")).isNull()
        assertThat(programs.currentPrograms.count { !it.isTemplate }).isEqualTo(1)
    }

    @Test
    fun `custom choice creates an empty program`() = runTest {
        val (_, apply) = useCase(UserPreferences(onboardingComplete = true, starterProgram = StarterProgram.CUSTOM))

        val id = apply("My program")

        assertThat(programs.currentPrograms.single { it.id == id }.name).isEqualTo("My program")
    }

    @Test
    fun `does nothing without a choice or when the user already has programs`() = runTest {
        val (_, noChoice) = useCase(UserPreferences(onboardingComplete = true))
        assertThat(noChoice("x")).isNull()

        programs.createProgram("Existing")
        val (preferences, apply) = useCase(UserPreferences(onboardingComplete = true, starterProgram = StarterProgram.PPL_3))
        assertThat(apply("x")).isNull()
        assertThat(preferences.current.starterProgramApplied).isTrue()
        assertThat(programs.activeProgramId).isNull()
    }
}
