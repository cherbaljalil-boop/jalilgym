package com.chdjalil.jalilgym.feature.exercises.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.chdjalil.jalilgym.feature.exercises.detail.ExerciseDetailScreen
import com.chdjalil.jalilgym.feature.exercises.editor.ExerciseEditorScreen
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
data object ExerciseLibraryRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

/** [exerciseId] is null when creating a new custom exercise. */
@Serializable
data class ExerciseEditorRoute(val exerciseId: String? = null)

/** Argument keys match the route property names. */
internal const val EXERCISE_ID_ARG = "exerciseId"

fun NavController.navigateToExerciseLibrary(navOptions: NavOptions? = null) =
    navigate(ExerciseLibraryRoute, navOptions)

/** Library → detail → editor. All navigation stays inside this feature. */
fun NavGraphBuilder.exerciseScreens(navController: NavController) {
    composable<ExerciseLibraryRoute> {
        ExerciseLibraryScreen(
            onBack = navController::popBackStack,
            onExerciseClick = { navController.navigate(ExerciseDetailRoute(it)) },
            onCreateExercise = { navController.navigate(ExerciseEditorRoute()) },
        )
    }
    composable<ExerciseDetailRoute> {
        ExerciseDetailScreen(
            onBack = navController::popBackStack,
            onEdit = { navController.navigate(ExerciseEditorRoute(it)) },
        )
    }
    composable<ExerciseEditorRoute> { entry ->
        val editing = entry.toRoute<ExerciseEditorRoute>().exerciseId != null
        ExerciseEditorScreen(
            onClose = navController::popBackStack,
            onSaved = { id ->
                navController.popBackStack()
                // A new exercise opens its detail page; an edit returns to the page it came from.
                if (!editing) navController.navigate(ExerciseDetailRoute(id))
            },
        )
    }
}
