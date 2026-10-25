package com.chdjalil.jalilgym.feature.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SessionStatus
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutContent
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutEvent
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ActiveWorkoutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val set = WorkoutSet("s1", 0, SetType.WORKING, 100.0, 5, null, null, completed = false, completedAt = null)
    private val session = WorkoutSession(
        id = "w1",
        name = "Push",
        programDayId = null,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        status = SessionStatus.ACTIVE,
        notes = null,
        exercises = listOf(
            SessionExercise(
                id = "e1",
                exercise = sampleExercise("bench", "Bench Press"),
                position = 0,
                supersetGroup = null,
                programExerciseId = "slot",
                target = Prescription(targetSets = 1, repMin = 5, repMax = 8, restSeconds = 180, incrementKg = 2.5),
                sets = listOf(set),
                previous = listOf(set.copy(weightKg = 97.5, completed = true)),
            ),
        ),
    )

    private fun render(units: UnitSystem = UnitSystem.METRIC, onEvent: (ActiveWorkoutEvent) -> Unit = {}) {
        composeRule.setContent {
            JalilgymTheme {
                ActiveWorkoutContent(
                    uiState = ActiveWorkoutUiState.Active(session, rest = null, preferences = UserPreferences(units = units)),
                    onEvent = onEvent,
                    onMinimize = {},
                    onAddExercise = {},
                    onSwapExercise = {},
                )
            }
        }
    }

    @Test
    fun showsTargetsPreviousAndCompletes() {
        val events = mutableListOf<ActiveWorkoutEvent>()
        render(onEvent = { events += it })

        composeRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeRule.onNodeWithText("Target 1 × 5–8 · 3:00 rest").assertIsDisplayed()
        composeRule.onNodeWithText("97.5×5").assertIsDisplayed()

        composeRule.onNodeWithTag("complete_s1").performClick()
        assertThat(events.single()).isEqualTo(ActiveWorkoutEvent.ToggleComplete(set))
    }

    @Test
    fun typingWeightConvertsFromPounds() {
        val events = mutableListOf<ActiveWorkoutEvent>()
        render(units = UnitSystem.IMPERIAL, onEvent = { events += it })

        composeRule.onNodeWithText("LB").assertIsDisplayed()
        composeRule.onNodeWithTag("weight_s1").performTextReplacement("225")

        val edit = events.filterIsInstance<ActiveWorkoutEvent.EditSet>().last()
        assertThat(edit.transform(set).weightKg!!).isWithin(1e-3).of(102.058)
    }
}
