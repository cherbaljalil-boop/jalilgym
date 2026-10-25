package com.chdjalil.jalilgym.core.database.di

import android.content.Context
import androidx.room.Room
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.dao.BodyDao
import com.chdjalil.jalilgym.core.database.dao.DietPlanDao
import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.dao.FoodDao
import com.chdjalil.jalilgym.core.database.dao.FoodLogDao
import com.chdjalil.jalilgym.core.database.dao.GroceryDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
import com.chdjalil.jalilgym.core.database.dao.ProgressDao
import com.chdjalil.jalilgym.core.database.dao.RecipeDao
import com.chdjalil.jalilgym.core.database.dao.SavedMealDao
import com.chdjalil.jalilgym.core.database.dao.WaterDao
import com.chdjalil.jalilgym.core.database.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JalilgymDatabase =
        Room.databaseBuilder(context, JalilgymDatabase::class.java, JalilgymDatabase.NAME).build()

    @Provides
    fun provideExerciseDao(database: JalilgymDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideProgramDao(database: JalilgymDatabase): ProgramDao = database.programDao()

    @Provides
    fun provideWorkoutDao(database: JalilgymDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideProgressDao(database: JalilgymDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideBodyDao(database: JalilgymDatabase): BodyDao = database.bodyDao()

    @Provides
    fun provideFoodDao(database: JalilgymDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideFoodLogDao(database: JalilgymDatabase): FoodLogDao = database.foodLogDao()

    @Provides
    fun provideSavedMealDao(database: JalilgymDatabase): SavedMealDao = database.savedMealDao()

    @Provides
    fun provideDietPlanDao(database: JalilgymDatabase): DietPlanDao = database.dietPlanDao()

    @Provides
    fun provideRecipeDao(database: JalilgymDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideWaterDao(database: JalilgymDatabase): WaterDao = database.waterDao()

    @Provides
    fun provideGroceryDao(database: JalilgymDatabase): GroceryDao = database.groceryDao()
}
