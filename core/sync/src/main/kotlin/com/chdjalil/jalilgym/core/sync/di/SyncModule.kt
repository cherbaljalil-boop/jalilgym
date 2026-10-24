package com.chdjalil.jalilgym.core.sync.di

import com.chdjalil.jalilgym.core.domain.repository.CloudSyncRepository
import com.chdjalil.jalilgym.core.sync.BuildConfig
import com.chdjalil.jalilgym.core.sync.SessionStore
import com.chdjalil.jalilgym.core.sync.SharedPreferencesSessionStore
import com.chdjalil.jalilgym.core.sync.SupabaseClient
import com.chdjalil.jalilgym.core.sync.SupabaseCloudSync
import com.chdjalil.jalilgym.core.sync.SupabaseConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SyncModule {

    @Binds
    abstract fun bindsCloudSyncRepository(impl: SupabaseCloudSync): CloudSyncRepository

    @Binds
    abstract fun bindsSessionStore(impl: SharedPreferencesSessionStore): SessionStore

    companion object {
        @Provides
        @Singleton
        fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)

        @Provides
        @Singleton
        fun provideSupabaseClient(client: OkHttpClient, config: SupabaseConfig): SupabaseClient = SupabaseClient(client, config)
    }
}
