package com.chdjalil.jalilgym.core.sync

import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.ReminderSettings
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

class SettingsSyncTest {

    private val prefs = UserPreferences(
        onboardingComplete = true,
        units = UnitSystem.IMPERIAL,
        bodyProfile = BodyProfile(Sex.MALE, LocalDate.of(1990, 1, 2), 180.0, ActivityLevel.LIGHT),
        waterGoalMl = 2_800,
        healthConnectEnabled = true,
        reminders = ReminderSettings(setOf(DayOfWeek.FRIDAY), LocalTime.of(6, 45), weeklySummary = true),
    )
    private val local = SyncedSettings.from(prefs)
    private fun row(settings: SyncedSettings, at: Long) = SyncRow(SETTINGS_TABLE, SETTINGS_KEY, settings.toJson(), at)

    @Test
    fun `settings round-trip without the phone's own ones`() {
        val back = local.toJson().toSyncedSettings()!!.toPreferences()
        assertThat(back).isEqualTo(prefs.copy(healthConnectEnabled = false))
    }

    @Test
    fun `first sign-in takes the account's settings, or uploads when there are none`() {
        val remote = SyncedSettings(units = UnitSystem.METRIC)
        assertThat(decideSettings(local, lastSynced = null, lastSyncedAt = null, remote = row(remote, 5)))
            .isEqualTo(SettingsDecision.TakeRemote(remote, 5))
        assertThat(decideSettings(local, null, null, remote = null)).isEqualTo(SettingsDecision.PushLocal)
    }

    @Test
    fun `newest change wins`() {
        val remote = local.copy(waterGoalMl = 3_500)
        // Changed elsewhere only: take it.
        assertThat(decideSettings(local, lastSynced = local, lastSyncedAt = 10, remote = row(remote, 20)))
            .isEqualTo(SettingsDecision.TakeRemote(remote, 20))
        // Changed here after the last sync: this phone's edit is the newest and goes up.
        val edited = local.copy(waterGoalMl = 2_000)
        assertThat(decideSettings(edited, lastSynced = local, lastSyncedAt = 10, remote = row(remote, 20))).isEqualTo(SettingsDecision.PushLocal)
        // Nothing new anywhere.
        assertThat(decideSettings(local, lastSynced = local, lastSyncedAt = 20, remote = null)).isEqualTo(SettingsDecision.Keep)
        // Our own upload coming back isn't news.
        assertThat(decideSettings(local, lastSynced = local, lastSyncedAt = 20, remote = row(local, 20))).isEqualTo(SettingsDecision.Keep)
    }
}
