package com.chdjalil.jalilgym.feature.progress

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.progress.weekStart
import com.chdjalil.jalilgym.core.model.BodyMeasurement
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrainingStreak
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.chdjalil.jalilgym.feature.progress.body.BodyContent
import com.chdjalil.jalilgym.feature.progress.body.BodyUiState
import com.chdjalil.jalilgym.feature.progress.body.MeasurementSummary
import com.chdjalil.jalilgym.feature.progress.hub.ProgressContent
import com.chdjalil.jalilgym.feature.progress.hub.ProgressUiState
import com.chdjalil.jalilgym.feature.progress.hub.WeekVolume
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ProgressAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.now()
    private val weights = listOf(BodyWeight("a", today.minusDays(7), 81.0), BodyWeight("b", today, 80.2))

    @Test
    fun hub() {
        composeRule.setContent {
            JalilgymTheme {
                ProgressContent(
                    uiState = ProgressUiState(
                        isLoading = false,
                        streak = TrainingStreak(3, 5, 2, 3, today),
                        week = WeekVolume(today.weekStart(), isCurrentWeek = false, sets = mapOf(Muscle.CHEST to 12.0)),
                        recentWeights = weights,
                        latestWeight = weights.last(),
                    ),
                    onPreviousWeek = {},
                    onNextWeek = {},
                    onLogWeight = {},
                    onOpenStrength = {},
                    onOpenRecords = {},
                    onOpenBody = {},
                    onOpenPhotos = {},
                )
            }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun body() {
        val waist = BodyMeasurement("m", today, MeasurementType.WAIST, 84.0)
        composeRule.setContent {
            JalilgymTheme {
                BodyContent(
                    uiState = BodyUiState(
                        isLoading = false,
                        weights = weights,
                        measurements = listOf(MeasurementSummary(MeasurementType.WAIST, waist, -1.0)),
                    ),
                    onBack = {},
                    onLogWeight = {},
                    onDeleteWeight = {},
                    onLogMeasurement = { _, _ -> },
                )
            }
        }
        composeRule.assertActionsHaveLabels()
    }
}
