package com.chdjalil.jalilgym.core.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType as HcMealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.FoodLogEntry
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Client record IDs tie Health Connect records to rows here, so writes replace instead of duplicating.
internal fun workoutClientId(id: String) = "jalilgym-workout-$id"

internal fun weightClientId(id: String) = "jalilgym-weight-$id"

internal fun mealClientId(date: LocalDate, meal: MealType) = "jalilgym-meal-$date-${meal.name.lowercase()}"

/** Diary days have no times; meals are placed at typical times so other apps can chart them. */
internal fun MealType.typicalTime(): LocalTime = when (this) {
    MealType.BREAKFAST -> LocalTime.of(8, 0)
    MealType.LUNCH -> LocalTime.of(12, 30)
    MealType.SNACK -> LocalTime.of(16, 0)
    MealType.DINNER -> LocalTime.of(19, 0)
}

private fun MealType.healthConnectType(): Int = when (this) {
    MealType.BREAKFAST -> HcMealType.MEAL_TYPE_BREAKFAST
    MealType.LUNCH -> HcMealType.MEAL_TYPE_LUNCH
    MealType.DINNER -> HcMealType.MEAL_TYPE_DINNER
    MealType.SNACK -> HcMealType.MEAL_TYPE_SNACK
}

private fun offsetAt(instant: Instant, zone: ZoneId) = zone.rules.getOffset(instant)

internal fun workoutRecord(workout: WorkoutHistoryItem, version: Long, zone: ZoneId): ExerciseSessionRecord {
    val start = Instant.ofEpochMilli(workout.startedAt)
    // Health Connect requires the end to be after the start.
    val end = Instant.ofEpochMilli(maxOf(workout.endedAt, workout.startedAt + Duration.ofMinutes(1).toMillis()))
    return ExerciseSessionRecord(
        startTime = start,
        startZoneOffset = offsetAt(start, zone),
        endTime = end,
        endZoneOffset = offsetAt(end, zone),
        metadata = Metadata.manualEntry(clientRecordId = workoutClientId(workout.id), clientRecordVersion = version),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        title = workout.name,
    )
}

/** Weigh-ins have a date only; they're recorded at 07:00 local time. */
internal fun weightRecord(weight: BodyWeight, version: Long, zone: ZoneId): WeightRecord {
    val time = weight.date.atTime(7, 0).atZone(zone).toInstant()
    return WeightRecord(
        time = time,
        zoneOffset = offsetAt(time, zone),
        weight = Mass.kilograms(weight.weightKg),
        metadata = Metadata.manualEntry(clientRecordId = weightClientId(weight.id), clientRecordVersion = version),
    )
}

/** One record per meal per day, with the meal's totals. Null when nothing has energy. */
internal fun mealRecord(date: LocalDate, meal: MealType, entries: List<FoodLogEntry>, version: Long, zone: ZoneId): NutritionRecord? {
    val totals = entries.fold(Nutrients.ZERO) { sum, e -> sum + e.nutrients }
    if (totals.kcal <= 0) return null
    val start = date.atTime(meal.typicalTime()).atZone(zone).toInstant()
    val end = start.plus(Duration.ofMinutes(15))
    return NutritionRecord(
        startTime = start,
        startZoneOffset = offsetAt(start, zone),
        endTime = end,
        endZoneOffset = offsetAt(end, zone),
        metadata = Metadata.manualEntry(clientRecordId = mealClientId(date, meal), clientRecordVersion = version),
        energy = Energy.kilocalories(totals.kcal),
        protein = Mass.grams(totals.proteinG),
        totalCarbohydrate = Mass.grams(totals.carbsG),
        totalFat = Mass.grams(totals.fatG),
        dietaryFiber = totals.fiberG?.let(Mass::grams),
        sugar = totals.sugarG?.let(Mass::grams),
        saturatedFat = totals.satFatG?.let(Mass::grams),
        sodium = totals.sodiumMg?.let { Mass.grams(it / 1000) },
        mealType = meal.healthConnectType(),
    )
}
