package com.chdjalil.jalilgym.core.sync

import com.chdjalil.jalilgym.core.domain.repository.syncedPart
import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.ReminderSettings
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Settings travel as one pseudo-row next to the database rows. */
internal const val SETTINGS_TABLE = "_settings"
internal const val SETTINGS_KEY = "settings"

/** The account-wide settings as stored on the server; dates and times are ISO strings. */
@Serializable
internal data class SyncedSettings(
    val onboardingComplete: Boolean = false,
    val goal: Goal? = null,
    val experience: ExperienceLevel? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val effortScale: EffortScale = EffortScale.RPE,
    val starterProgram: StarterProgram? = null,
    val starterProgramApplied: Boolean = false,
    val sex: Sex? = null,
    val birthDate: String? = null,
    val heightCm: Double? = null,
    val activityLevel: ActivityLevel? = null,
    val halalFilter: HalalFilter = HalalFilter.ALL,
    val lastCheckIn: String? = null,
    val waterGoalMl: Int? = null,
    val reminderDays: List<Int> = emptyList(),
    val reminderTime: String = "18:00",
    val weeklySummary: Boolean = false,
) {
    fun toPreferences(): UserPreferences {
        val sex = sex
        val birthDate = birthDate
        val heightCm = heightCm
        val activityLevel = activityLevel
        val profile = if (sex != null && birthDate != null && heightCm != null && activityLevel != null) {
            runCatching { BodyProfile(sex, LocalDate.parse(birthDate), heightCm, activityLevel) }.getOrNull()
        } else {
            null
        }
        return UserPreferences(
            onboardingComplete = onboardingComplete,
            goal = goal,
            experience = experience,
            units = units,
            themeMode = themeMode,
            effortScale = effortScale,
            starterProgram = starterProgram,
            starterProgramApplied = starterProgramApplied,
            bodyProfile = profile,
            halalFilter = halalFilter,
            lastCheckIn = lastCheckIn?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            waterGoalMl = waterGoalMl,
            reminders = ReminderSettings(
                workoutDays = reminderDays.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }.toSet(),
                workoutTime = runCatching { LocalTime.parse(reminderTime) }.getOrDefault(LocalTime.of(18, 0)),
                weeklySummary = weeklySummary,
            ),
        )
    }

    companion object {
        fun from(preferences: UserPreferences): SyncedSettings {
            val prefs = preferences.syncedPart()
            return SyncedSettings(
                onboardingComplete = prefs.onboardingComplete,
                goal = prefs.goal,
                experience = prefs.experience,
                units = prefs.units,
                themeMode = prefs.themeMode,
                effortScale = prefs.effortScale,
                starterProgram = prefs.starterProgram,
                starterProgramApplied = prefs.starterProgramApplied,
                sex = prefs.bodyProfile?.sex,
                birthDate = prefs.bodyProfile?.birthDate?.toString(),
                heightCm = prefs.bodyProfile?.heightCm,
                activityLevel = prefs.bodyProfile?.activityLevel,
                halalFilter = prefs.halalFilter,
                lastCheckIn = prefs.lastCheckIn?.toString(),
                waterGoalMl = prefs.waterGoalMl,
                reminderDays = prefs.reminders.workoutDays.map { it.value }.sorted(),
                reminderTime = prefs.reminders.workoutTime.toString(),
                weeklySummary = prefs.reminders.weeklySummary,
            )
        }
    }
}

internal val settingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun SyncedSettings.toJson(): JsonObject = settingsJson.encodeToJsonElement(SyncedSettings.serializer(), this).jsonObject

internal fun JsonObject.toSyncedSettings(): SyncedSettings? =
    runCatching { settingsJson.decodeFromJsonElement(SyncedSettings.serializer(), this) }.getOrNull()

/** What to do with settings after a pull, given what was last synced from this phone. */
internal sealed interface SettingsDecision {
    data object Keep : SettingsDecision

    data class TakeRemote(val settings: SyncedSettings, val updatedAt: Long) : SettingsDecision

    data object PushLocal : SettingsDecision
}

/**
 * Newest wins: remote settings changed since the last sync replace this
 * phone's unless this phone changed them too (then its newer edit goes up). A
 * phone signing in for the first time takes the account's settings.
 */
internal fun decideSettings(local: SyncedSettings, lastSynced: SyncedSettings?, lastSyncedAt: Long?, remote: SyncRow?): SettingsDecision {
    val remoteSettings = remote?.data?.toSyncedSettings()
    val remoteIsNew = remote != null && remoteSettings != null && remote.updatedAt > (lastSyncedAt ?: Long.MIN_VALUE)
    val localChanged = local != lastSynced
    return when {
        remoteIsNew && (lastSynced == null || !localChanged) -> SettingsDecision.TakeRemote(remoteSettings!!, remote!!.updatedAt)
        localChanged -> SettingsDecision.PushLocal
        else -> SettingsDecision.Keep
    }
}
