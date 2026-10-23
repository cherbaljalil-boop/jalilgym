package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.model.AchievementStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAchievementRepository : AchievementRepository {
    val achievements = MutableStateFlow<List<AchievementStatus>>(emptyList())

    /** Returned by the next [syncUnlocks] call. */
    var pendingUnlocks: List<String> = emptyList()
    var syncCount = 0
        private set

    override fun observeAchievements(): Flow<List<AchievementStatus>> = achievements

    override suspend fun syncUnlocks(): List<String> {
        syncCount++
        return pendingUnlocks.also { pendingUnlocks = emptyList() }
    }
}
