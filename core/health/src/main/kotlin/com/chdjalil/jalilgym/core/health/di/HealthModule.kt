package com.chdjalil.jalilgym.core.health.di

import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.health.ClientHealthConnectGateway
import com.chdjalil.jalilgym.core.health.HealthConnectGateway
import com.chdjalil.jalilgym.core.health.HealthConnectRepository
import com.chdjalil.jalilgym.core.health.SharedPreferencesSyncLedger
import com.chdjalil.jalilgym.core.health.SyncLedger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HealthModule {
    @Binds
    abstract fun bindsHealthRepository(impl: HealthConnectRepository): HealthRepository

    @Binds
    abstract fun bindsGateway(impl: ClientHealthConnectGateway): HealthConnectGateway

    @Binds
    abstract fun bindsLedger(impl: SharedPreferencesSyncLedger): SyncLedger

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
