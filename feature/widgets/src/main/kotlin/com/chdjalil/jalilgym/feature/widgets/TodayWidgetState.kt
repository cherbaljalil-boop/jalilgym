package com.chdjalil.jalilgym.feature.widgets

import com.chdjalil.jalilgym.core.domain.hydration.quickWaterAmountsMl
import com.chdjalil.jalilgym.core.domain.hydration.waterGoalMl
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WaterRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.UnitSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** What the Today widget shows; everything comes from the same repositories as the app. */
data class TodayWidgetState(
    /** Name of the next program day, or of the workout in progress. */
    val workoutName: String? = null,
    val programName: String? = null,
    val workoutInProgress: Boolean = false,
    val trainedToday: Boolean = false,
    val streakWeeks: Int = 0,
    val kcalEaten: Int = 0,
    /** Null until nutrition setup is done. */
    val kcalTarget: Int? = null,
    val proteinEaten: Int = 0,
    val proteinTarget: Int? = null,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 0,
    /** What the widget's water button adds. */
    val waterStepMl: Int = 250,
    val units: UnitSystem = UnitSystem.METRIC,
)

internal class TodayWidgetSources(
    val preferences: UserPreferencesRepository,
    val programs: ProgramRepository,
    val workouts: WorkoutRepository,
    val progress: ProgressRepository,
    val nutrition: NutritionRepository,
    val water: WaterRepository,
    val body: BodyRepository,
)

internal fun TodayWidgetSources.observeState(today: LocalDate, zone: ZoneId): Flow<TodayWidgetState> {
    val training = combine(
        programs.observeActiveProgram(),
        workouts.observeActiveSession(),
        workouts.observeHistory(),
        progress.observeTrainingStreak(),
    ) { active, session, history, streak ->
        TodayWidgetState(
            workoutName = session?.name ?: active?.nextDay?.name,
            programName = active?.program?.name,
            workoutInProgress = session != null,
            trainedToday = history.any { Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() == today },
            streakWeeks = streak.currentWeeks,
        )
    }
    val food = combine(nutrition.observeDiary(today), nutrition.observeActivePlan()) { day, plan -> day.totals to plan?.targets }
    return combine(
        training,
        food,
        water.observeDay(today),
        preferences.userPreferences,
        body.observeWeights(),
    ) { state, (eaten, targets), water, prefs, weights ->
        state.copy(
            kcalEaten = eaten.kcal.roundToInt(),
            kcalTarget = targets?.calories,
            proteinEaten = eaten.proteinG.roundToInt(),
            proteinTarget = targets?.proteinG,
            waterMl = water.totalMl,
            waterGoalMl = waterGoalMl(prefs.waterGoalMl, weights.lastOrNull()?.weightKg),
            waterStepMl = quickWaterAmountsMl(prefs.units).first(),
            units = prefs.units,
        )
    }
}
