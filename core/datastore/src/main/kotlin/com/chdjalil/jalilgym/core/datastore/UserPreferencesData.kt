package com.chdjalil.jalilgym.core.datastore

import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import java.time.LocalDate
import kotlinx.serialization.Serializable

/**
 * On-disk shape of the user's settings. Every field has a default so new
 * fields can be added without a migration; unknown fields are ignored on read.
 */
@Serializable
data class UserPreferencesData(
    val onboardingComplete: Boolean = false,
    val goal: Goal? = null,
    val experience: ExperienceLevel? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val effortScale: EffortScale = EffortScale.RPE,
    val defaultRestCompoundSec: Int = 180,
    val defaultRestIsolationSec: Int = 90,
    val keepScreenOn: Boolean = true,
    val starterProgram: StarterProgram? = null,
    val starterProgramApplied: Boolean = false,
    /** Version of the bundled exercise seed last imported into Room. */
    val installedSeedVersion: Int = 0,
    /** Version of the bundled program templates last imported into Room. */
    val installedProgramSeedVersion: Int = 0,
    /** Version of the bundled generic foods last imported into Room. */
    val installedFoodSeedVersion: Int = 0,
    /** Version of the built-in recipes last imported into Room. */
    val installedRecipeSeedVersion: Int = 0,
    val bodyProfile: BodyProfileData? = null,
    val halalFilter: HalalFilter = HalalFilter.ALL,
    /** ISO-8601 date. */
    val lastCheckIn: String? = null,
    val healthConnectEnabled: Boolean = false,
    val waterGoalMl: Int? = null,
)

/** [birthDate] is ISO-8601 (yyyy-MM-dd). */
@Serializable
data class BodyProfileData(
    val sex: Sex,
    val birthDate: String,
    val heightCm: Double,
    val activityLevel: ActivityLevel,
)

internal fun BodyProfileData.toModel(): BodyProfile? = runCatching {
    BodyProfile(sex, LocalDate.parse(birthDate), heightCm, activityLevel)
}.getOrNull()

internal fun BodyProfile.toData() = BodyProfileData(sex, birthDate.toString(), heightCm, activityLevel)

internal fun UserPreferencesData.toModel() = UserPreferences(
    onboardingComplete = onboardingComplete,
    goal = goal,
    experience = experience,
    units = units,
    themeMode = themeMode,
    effortScale = effortScale,
    defaultRestCompoundSec = defaultRestCompoundSec,
    defaultRestIsolationSec = defaultRestIsolationSec,
    keepScreenOn = keepScreenOn,
    starterProgram = starterProgram,
    starterProgramApplied = starterProgramApplied,
    bodyProfile = bodyProfile?.toModel(),
    halalFilter = halalFilter,
    lastCheckIn = lastCheckIn?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    healthConnectEnabled = healthConnectEnabled,
    waterGoalMl = waterGoalMl,
)
