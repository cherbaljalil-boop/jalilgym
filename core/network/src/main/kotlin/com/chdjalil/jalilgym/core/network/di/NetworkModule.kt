package com.chdjalil.jalilgym.core.network.di

import android.content.Context
import com.chdjalil.jalilgym.core.network.OFF_BASE_URL
import com.chdjalil.jalilgym.core.network.OkHttpOpenFoodFacts
import com.chdjalil.jalilgym.core.network.OpenFoodFactsDataSource
import com.chdjalil.jalilgym.core.network.userAgentInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {

    @Binds
    abstract fun bindsOpenFoodFacts(impl: OkHttpOpenFoodFacts): OpenFoodFactsDataSource

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                .getOrNull() ?: "dev"
            return OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor(version))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Named(OFF_BASE_URL)
        fun provideOffBaseUrl(): HttpUrl = "https://world.openfoodfacts.org/".toHttpUrl()
    }
}
