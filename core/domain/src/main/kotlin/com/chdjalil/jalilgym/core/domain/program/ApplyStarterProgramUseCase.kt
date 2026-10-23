package com.chdjalil.jalilgym.core.domain.program

import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Turns the starter program picked during onboarding into the user's active
 * program, once. Does nothing if the user already has programs of their own.
 */
class ApplyStarterProgramUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val programRepository: ProgramRepository,
) {
    /** Returns the id of the program that was created, or null if nothing was done. */
    suspend operator fun invoke(customProgramName: String): String? {
        val preferences = preferencesRepository.userPreferences.first()
        val starter = preferences.starterProgram
        if (!preferences.onboardingComplete || starter == null || preferences.starterProgramApplied) return null

        if (programRepository.observeUserPrograms().first().isNotEmpty()) {
            preferencesRepository.markStarterProgramApplied()
            return null
        }
        val programId = starter.templateId
            ?.let { programRepository.copyProgram(it) }
            ?: programRepository.createProgram(customProgramName)
        programRepository.activate(programId)
        preferencesRepository.markStarterProgramApplied()
        return programId
    }
}
