package com.chdjalil.jalilgym

import android.app.Application
import android.util.Log
import com.chdjalil.jalilgym.core.common.ApplicationScope
import com.chdjalil.jalilgym.core.domain.repository.SeedRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JalilgymApplication : Application() {

    @Inject
    lateinit var seedRepository: SeedRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Screens observe Room, so they update as soon as the seed lands.
        applicationScope.launch {
            runCatching { seedRepository.seedIfNeeded() }
                .onFailure { Log.e(TAG, "Seed import failed", it) }
        }
    }

    private companion object {
        const val TAG = "Jalilgym"
    }
}
