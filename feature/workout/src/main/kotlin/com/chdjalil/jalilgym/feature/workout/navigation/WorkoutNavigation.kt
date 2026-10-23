package com.chdjalil.jalilgym.feature.workout.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutScreen
import com.chdjalil.jalilgym.feature.workout.history.HistoryScreen
import com.chdjalil.jalilgym.feature.workout.history.SessionDetailScreen
import com.chdjalil.jalilgym.feature.workout.summary.WorkoutSummaryScreen
import kotlinx.serialization.Serializable

/** The workout in progress. Opened from the notification via [WORKOUT_DEEP_LINK]. */
@Serializable
data object ActiveWorkoutRoute

@Serializable
data class WorkoutSummaryRoute(val sessionId: String)

@Serializable
data object WorkoutHistoryRoute

@Serializable
data class SessionDetailRoute(val sessionId: String)

const val WORKOUT_DEEP_LINK = "jalilgym://workout"

internal const val SESSION_ID_ARG = "sessionId"

fun NavController.navigateToActiveWorkout() = navigate(ActiveWorkoutRoute) { launchSingleTop = true }

fun NavController.navigateToWorkoutHistory() = navigate(WorkoutHistoryRoute)

/**
 * Full-screen workout flow. [onPickExercises] opens the exercise picker (results
 * come back through ExercisePickerResult); [onExit] leaves the flow entirely.
 */
fun NavGraphBuilder.workoutScreens(
    navController: NavController,
    onPickExercises: () -> Unit,
    onExit: () -> Unit,
) {
    composable<ActiveWorkoutRoute>(deepLinks = listOf(navDeepLink<ActiveWorkoutRoute>(basePath = WORKOUT_DEEP_LINK))) {
        ActiveWorkoutScreen(
            onMinimize = navController::popBackStack,
            onPickExercises = onPickExercises,
            onFinished = { sessionId ->
                navController.navigate(WorkoutSummaryRoute(sessionId)) {
                    popUpTo<ActiveWorkoutRoute> { inclusive = true }
                }
            },
            onDiscarded = onExit,
        )
    }
    composable<WorkoutSummaryRoute> {
        WorkoutSummaryScreen(onDone = onExit)
    }
}

/** History lives in the Train tab. */
fun NavGraphBuilder.workoutHistoryScreens(navController: NavController) {
    composable<WorkoutHistoryRoute> {
        HistoryScreen(
            onBack = navController::popBackStack,
            onOpenSession = { navController.navigate(SessionDetailRoute(it)) },
        )
    }
    composable<SessionDetailRoute> {
        SessionDetailScreen(onBack = navController::popBackStack)
    }
}
