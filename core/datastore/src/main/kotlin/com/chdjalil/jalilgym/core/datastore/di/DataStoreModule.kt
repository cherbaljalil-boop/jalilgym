package com.chdjalil.jalilgym.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.chdjalil.jalilgym.core.common.ApplicationScope
import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import com.chdjalil.jalilgym.core.datastore.UserPreferencesData
import com.chdjalil.jalilgym.core.datastore.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(JgDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UserPreferencesData> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile("user_preferences.json") },
    )
}
