package com.chdjalil.jalilgym.core.database.di

import android.content.Context
import androidx.room.Room
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
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
}
