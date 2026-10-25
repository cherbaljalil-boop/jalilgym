package com.chdjalil.jalilgym.core.datastore

import androidx.datastore.core.CorruptionException
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class UserPreferencesSerializerTest {

    @Test
    fun `defaults are dark theme and metric`() {
        val defaults = UserPreferencesSerializer.defaultValue
        assertThat(defaults.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(defaults.units).isEqualTo(UnitSystem.METRIC)
        assertThat(defaults.onboardingComplete).isFalse()
    }

    @Test
    fun `round trips through bytes`() = runTest {
        val prefs = UserPreferencesData(
            onboardingComplete = true,
            goal = Goal.BUILD_MUSCLE,
            units = UnitSystem.IMPERIAL,
            starterProgram = StarterProgram.PPL_3,
            installedSeedVersion = 3,
            waterGoalMl = 3_000,
            reminders = ReminderData(workoutDays = listOf(1, 4), workoutMinuteOfDay = 7 * 60 + 15, weeklySummary = true),
        )
        val out = ByteArrayOutputStream()
        UserPreferencesSerializer.writeTo(prefs, out)

        val read = UserPreferencesSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))

        assertThat(read).isEqualTo(prefs)
    }

    @Test
    fun `missing and unknown fields fall back to defaults`() = runTest {
        val legacy = """{"themeMode":"LIGHT","someFutureField":42}"""

        val read = UserPreferencesSerializer.readFrom(ByteArrayInputStream(legacy.toByteArray()))

        assertThat(read).isEqualTo(UserPreferencesData(themeMode = ThemeMode.LIGHT))
    }

    @Test
    fun `garbage input is reported as corruption`() {
        assertThrows(CorruptionException::class.java) {
            kotlinx.coroutines.runBlocking {
                UserPreferencesSerializer.readFrom(ByteArrayInputStream("not json".toByteArray()))
            }
        }
    }

    @Test
    fun `reminders map to the model and back`() {
        val data = ReminderData(workoutDays = listOf(1, 4, 9), workoutMinuteOfDay = 7 * 60 + 15, weeklySummary = true)
        val model = data.toModel()
        assertThat(model.workoutDays).containsExactly(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.THURSDAY)
        assertThat(model.workoutTime).isEqualTo(java.time.LocalTime.of(7, 15))
        assertThat(model.toData()).isEqualTo(data.copy(workoutDays = listOf(1, 4)))
    }
}
