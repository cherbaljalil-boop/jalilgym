package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.AchievementStatus
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    /** Every badge in catalog order, with progress and unlock time. */
    fun observeAchievements(): Flow<List<AchievementStatus>>

    /** Stores newly earned badges and returns their ids. Safe to call any time. */
    suspend fun syncUnlocks(): List<String>
}
