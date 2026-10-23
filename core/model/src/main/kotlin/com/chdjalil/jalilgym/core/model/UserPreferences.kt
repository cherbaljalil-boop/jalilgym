package com.chdjalil.jalilgym.core.model

/** App-wide user settings. Stored in DataStore; never contains secrets. */
data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val goal: Goal? = null,
    val experience: ExperienceLevel? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val effortScale: EffortScale = EffortScale.RPE,
    val defaultRestCompoundSec: Int = 180,
    val defaultRestIsolationSec: Int = 90,
    val keepScreenOn: Boolean = true,
)

enum class Goal { BUILD_MUSCLE, LOSE_FAT, GET_STRONGER, GENERAL_FITNESS, ENDURANCE }

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class UnitSystem { METRIC, IMPERIAL }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** How effort is captured per set. */
enum class EffortScale { RPE, RIR, OFF }
