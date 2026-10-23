package com.chdjalil.jalilgym.core.data.di

import com.chdjalil.jalilgym.core.data.media.PrivateExerciseImageStore
import com.chdjalil.jalilgym.core.data.repository.OfflineFirstExerciseRepository
import com.chdjalil.jalilgym.core.data.repository.OfflineFirstProgramRepository
import com.chdjalil.jalilgym.core.data.repository.OfflineFirstSeedRepository
import com.chdjalil.jalilgym.core.data.repository.OfflineFirstUserPreferencesRepository
import com.chdjalil.jalilgym.core.data.repository.OfflineFirstWorkoutRepository
import com.chdjalil.jalilgym.core.data.workout.InMemoryRestTimer
import com.chdjalil.jalilgym.core.domain.repository.ExerciseImageStore
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.SeedRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindsUserPreferencesRepository(
        impl: OfflineFirstUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    abstract fun bindsExerciseRepository(impl: OfflineFirstExerciseRepository): ExerciseRepository

    @Binds
    abstract fun bindsExerciseImageStore(impl: PrivateExerciseImageStore): ExerciseImageStore

    @Binds
    abstract fun bindsProgramRepository(impl: OfflineFirstProgramRepository): ProgramRepository

    @Binds
    abstract fun bindsWorkoutRepository(impl: OfflineFirstWorkoutRepository): WorkoutRepository

    @Binds
    abstract fun bindsRestTimer(impl: InMemoryRestTimer): RestTimer

    @Binds
    abstract fun bindsSeedRepository(impl: OfflineFirstSeedRepository): SeedRepository
}
