package com.chdjalil.jalilgym.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
    /** Chosen at the end of onboarding; turned into a real program in Phase 2. */
    val starterProgram: StarterProgram? = null,
    /** True once [starterProgram] has been turned into a real, active program. */
    val starterProgramApplied: Boolean = false,
    /** Answers from nutrition setup; null until it's done. */
    val bodyProfile: BodyProfile? = null,
    val halalFilter: HalalFilter = HalalFilter.ALL,
    /** When the weekly nutrition check-in was last applied or dismissed. */
    val lastCheckIn: LocalDate? = null,
    val healthConnectEnabled: Boolean = false,
    /** Daily water goal set by the user; null means work it out from body weight. */
    val waterGoalMl: Int? = null,
    val reminders: ReminderSettings = ReminderSettings(),
)

enum class Goal { BUILD_MUSCLE, LOSE_FAT, GET_STRONGER, GENERAL_FITNESS, ENDURANCE }

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class UnitSystem { METRIC, IMPERIAL }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Starter programs offered at the end of onboarding (docs/BLUEPRINT.md §3.1). */
enum class StarterProgram { FULL_BODY_3, PPL_3, PPL_6, CUSTOM }

/** How effort is captured per set. */
enum class EffortScale { RPE, RIR, OFF }

/** Optional notifications; everything is off by default. */
data class ReminderSettings(
    /** Days to get a workout reminder on; empty means no reminders. */
    val workoutDays: Set<DayOfWeek> = emptySet(),
    val workoutTime: LocalTime = LocalTime.of(18, 0),
    /** A short recap of the week on Sunday evening. */
    val weeklySummary: Boolean = false,
)
