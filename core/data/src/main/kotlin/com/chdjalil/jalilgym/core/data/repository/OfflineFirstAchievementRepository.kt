package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.BodyDao
import com.chdjalil.jalilgym.core.database.dao.DietPlanDao
import com.chdjalil.jalilgym.core.database.dao.FoodLogDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
import com.chdjalil.jalilgym.core.database.dao.ProgressDao
import com.chdjalil.jalilgym.core.database.entity.AchievementUnlockEntity
import com.chdjalil.jalilgym.core.domain.nutrition.bestLoggingStreak
import com.chdjalil.jalilgym.core.domain.nutrition.hitTargets
import com.chdjalil.jalilgym.core.domain.progress.ACHIEVEMENTS
import com.chdjalil.jalilgym.core.domain.progress.AchievementStats
import com.chdjalil.jalilgym.core.domain.progress.earnedAchievements
import com.chdjalil.jalilgym.core.domain.progress.trainingStreak
import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.model.AchievementStatus
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstAchievementRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val bodyDao: BodyDao,
    private val programDao: ProgramDao,
    private val foodLogDao: FoodLogDao,
    private val dietPlanDao: DietPlanDao,
) : AchievementRepository {

    private val syncLock = Mutex()

    /** Entries logged, best logging streak and days on target (judged against the current plan). */
    private val nutrition: Flow<Triple<Int, Int, Int>> = combine(
        foodLogDao.observeEntryCount(),
        foodLogDao.observeDailyTotals(),
        dietPlanDao.observeActive(),
    ) { entries, days, plan ->
        val targets = plan?.let { MacroTargets(it.calories, it.proteinG, it.carbsG, it.fatG) }
        val hit = if (targets == null) 0 else days.count { hitTargets(Nutrients(it.kcal, it.proteinG, 0.0, 0.0), targets) }
        Triple(entries, bestLoggingStreak(days.map { it.date }), hit)
    }

    private val trainingStats: Flow<AchievementStats> = combine(
        combine(progressDao.observeWorkoutStarts(), programDao.observeActive()) { starts, active ->
            val target = active?.program?.program?.daysPerWeek ?: DEFAULT_WEEKLY_TARGET
            starts.size to trainingStreak(starts.map { it.toLocalDate() }, target, LocalDate.now()).bestWeeks
        },
        progressDao.observeImprovedRecordCount(),
        progressDao.observeTotalVolume(),
        combine(bodyDao.observeLatestWeight(), progressDao.observeBestOneRepMax()) { weight, bests ->
            weight?.weightKg to bests.associate { it.exerciseId to it.value }
        },
        combine(progressDao.observeCustomProgramCount(), progressDao.observeCustomExerciseCount(), bodyDao.observePhotoCount()) {
                programs, exercises, photos ->
            Triple(programs, exercises, photos)
        },
    ) { (workouts, bestStreak), records, volume, (bodyWeight, bests), (programs, exercises, photos) ->
        AchievementStats(
            workouts = workouts,
            bestStreakWeeks = bestStreak,
            records = records,
            totalVolumeKg = volume,
            bodyWeightKg = bodyWeight,
            bestOneRepMaxKg = bests,
            customPrograms = programs,
            customExercises = exercises,
            photos = photos,
        )
    }

    private val stats: Flow<AchievementStats> = combine(trainingStats, nutrition) { training, (meals, streak, hit) ->
        training.copy(mealsLogged = meals, loggingStreakDays = streak, macroDaysHit = hit)
    }

    override fun observeAchievements(): Flow<List<AchievementStatus>> =
        combine(stats, progressDao.observeUnlocks()) { stats, unlocks ->
            val unlockedAt = unlocks.associate { it.achievementId to it.unlockedAt }
            ACHIEVEMENTS.map { definition ->
                AchievementStatus(
                    id = definition.id,
                    category = definition.category,
                    target = definition.target,
                    progress = definition.progress(stats),
                    unlockedAt = unlockedAt[definition.id],
                )
            }
        }

    override suspend fun syncUnlocks(): List<String> = syncLock.withLock {
        val earned = earnedAchievements(stats.first())
        val already = progressDao.observeUnlocks().first().map { it.achievementId }.toSet()
        val fresh = (earned - already).toList()
        if (fresh.isNotEmpty()) {
            val now = System.currentTimeMillis()
            progressDao.insertUnlocks(fresh.map { AchievementUnlockEntity(it, now, sourceRefId = null) })
        }
        // Catalog order, so the summary lists them consistently.
        ACHIEVEMENTS.map { it.id }.filter { it in fresh }
    }
}
