package com.chdjalil.jalilgym.core.datastore

import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
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
    /** Version of the bundled seed data last imported into Room. */
    val installedSeedVersion: Int = 0,
)

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
)
