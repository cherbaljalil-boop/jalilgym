package com.chdjalil.jalilgym.feature.profile

import com.chdjalil.jalilgym.core.model.AchievementCategory
import com.chdjalil.jalilgym.core.model.AchievementStatus
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeAchievementRepository
import com.chdjalil.jalilgym.feature.profile.achievements.AchievementsViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `groups badges by category and counts unlocks`() = runTest {
        val repository = FakeAchievementRepository()
        repository.achievements.value = listOf(
            AchievementStatus("first_workout", AchievementCategory.MILESTONES, 1.0, 1.0, unlockedAt = 10L),
            AchievementStatus("workouts_10", AchievementCategory.MILESTONES, 10.0, 1.0, unlockedAt = null),
            AchievementStatus("streak_4", AchievementCategory.STREAKS, 4.0, 0.0, unlockedAt = null),
        )
        val viewModel = AchievementsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertThat(state.unlocked).isEqualTo(1)
        assertThat(state.total).isEqualTo(3)
        assertThat(state.byCategory.keys).containsExactly(AchievementCategory.MILESTONES, AchievementCategory.STREAKS).inOrder()
        assertThat(state.byCategory.getValue(AchievementCategory.MILESTONES)[1].fraction).isEqualTo(0.1f)
    }
}
