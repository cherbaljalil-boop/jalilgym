package com.chdjalil.jalilgym.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.entity.AchievementUnlockEntity
import com.chdjalil.jalilgym.core.database.entity.ActiveProgramEntity
import com.chdjalil.jalilgym.core.database.entity.BodyMeasurementEntity
import com.chdjalil.jalilgym.core.database.entity.BodyWeightEntity
import com.chdjalil.jalilgym.core.database.entity.DietPlanEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodLogEntryEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.database.entity.MealPlanEntryEntity
import com.chdjalil.jalilgym.core.database.entity.PersonalRecordEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramDayEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ProgressPhotoEntity
import com.chdjalil.jalilgym.core.database.entity.ProgressionStateEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealItemEntity
import com.chdjalil.jalilgym.core.database.entity.SessionExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.SetLogEntity
import com.chdjalil.jalilgym.core.database.entity.WorkoutSessionEntity

/**
 * Schema changes after the first public release must ship with a migration
 * and a migration test; destructive migration is never enabled.
 */
@Database(
    version = 1,
    exportSchema = true,
    entities = [
        // Training
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseImageEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
        ProgressionStateEntity::class,
        ActiveProgramEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetLogEntity::class,
        PersonalRecordEntity::class,
        // Body, progress & gamification
        BodyWeightEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class,
        AchievementUnlockEntity::class,
        // Nutrition
        DietPlanEntity::class,
        FoodEntity::class,
        FoodServingEntity::class,
        FoodLogEntryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        SavedMealEntity::class,
        SavedMealItemEntity::class,
        MealPlanEntryEntity::class,
    ],
)
@TypeConverters(Converters::class)
abstract class JalilgymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val NAME = "jalilgym.db"
    }
}
