package com.chdjalil.jalilgym.feature.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseFilter
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryContent
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryEvent
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ExerciseLibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listsExercisesAndOpensDetail() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            JalilgymTheme {
                ExerciseLibraryContent(
                    uiState = ExerciseLibraryUiState(
                        isLoading = false,
                        exercises = listOf(sampleExercise("bench", "Barbell Bench Press")),
                        totalCount = 1,
                    ),
                    onEvent = {},
                    onBack = {},
                    onExerciseClick = { opened += it },
                    onCreateExercise = {},
                )
            }
        }

        composeRule.onNodeWithText("1 exercise").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell Bench Press").performClick()

        assertThat(opened).containsExactly("bench")
    }

    @Test
    fun emptyResultsOfferToClearFilters() {
        val events = mutableListOf<ExerciseLibraryEvent>()
        composeRule.setContent {
            JalilgymTheme {
                ExerciseLibraryContent(
                    uiState = ExerciseLibraryUiState(isLoading = false, filter = ExerciseFilter(query = "zzz")),
                    onEvent = { events += it },
                    onBack = {},
                    onExerciseClick = {},
                    onCreateExercise = {},
                )
            }
        }

        composeRule.onNodeWithText("No exercises match").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").performClick()

        assertThat(events).containsExactly(
            ExerciseLibraryEvent.QueryChanged(""),
            ExerciseLibraryEvent.FiltersCleared,
        ).inOrder()
    }
}
