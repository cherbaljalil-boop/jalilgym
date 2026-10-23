package com.chdjalil.jalilgym.core.data.repository

import androidx.datastore.core.DataStoreFactory
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.datastore.UserPreferencesSerializer
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OfflineFirstUserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.repository(): OfflineFirstUserPreferencesRepository {
        val dataStore = DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            scope = backgroundScope,
            // A path that does not exist yet: DataStore creates it on first write.
            produceFile = { File(tmpFolder.root, "user_preferences_test.json") },
        )
        return OfflineFirstUserPreferencesRepository(JgPreferencesDataSource(dataStore))
    }

    @Test
    fun `starts with defaults`() = runTest {
        assertThat(repository().userPreferences.first()).isEqualTo(UserPreferences())
    }

    @Test
    fun `persists changes`() = runTest {
        val repository = repository()

        repository.setThemeMode(ThemeMode.SYSTEM)
        repository.setUnitSystem(UnitSystem.IMPERIAL)
        repository.completeOnboarding(Goal.GET_STRONGER, ExperienceLevel.ADVANCED, StarterProgram.PPL_6)

        assertThat(repository.userPreferences.first()).isEqualTo(
            UserPreferences(
                onboardingComplete = true,
                goal = Goal.GET_STRONGER,
                experience = ExperienceLevel.ADVANCED,
                units = UnitSystem.IMPERIAL,
                themeMode = ThemeMode.SYSTEM,
                starterProgram = StarterProgram.PPL_6,
            ),
        )
    }
}
