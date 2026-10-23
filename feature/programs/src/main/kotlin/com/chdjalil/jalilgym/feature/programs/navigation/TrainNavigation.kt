package com.chdjalil.jalilgym.feature.programs.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.programs.builder.ProgramBuilderScreen
import com.chdjalil.jalilgym.feature.programs.day.DayEditorScreen
import com.chdjalil.jalilgym.feature.programs.preview.DayPreviewScreen
import com.chdjalil.jalilgym.feature.programs.templates.TemplateGalleryScreen
import com.chdjalil.jalilgym.feature.programs.train.TrainScreen
import kotlinx.serialization.Serializable

@Serializable
data object TrainRoute

@Serializable
data object TemplateGalleryRoute

@Serializable
data class ProgramBuilderRoute(val programId: String)

@Serializable
data class DayEditorRoute(val dayId: String)

/** Read-only view of a program day; becomes the workout launcher in Phase 3. */
@Serializable
data class DayPreviewRoute(val dayId: String)

/** Argument keys match the route property names. */
internal const val PROGRAM_ID_ARG = "programId"
internal const val DAY_ID_ARG = "dayId"

fun NavController.navigateToTrain(navOptions: NavOptions? = null) = navigate(TrainRoute, navOptions)

fun NavController.navigateToDayPreview(dayId: String) = navigate(DayPreviewRoute(dayId))

/**
 * Train tab screens. [onOpenLibrary] and [onPickExercises] lead into the
 * exercises feature; the picker reports back via ExercisePickerResult.
 */
fun NavGraphBuilder.trainScreens(
    navController: NavController,
    onOpenLibrary: () -> Unit,
    onPickExercises: () -> Unit,
) {
    composable<TrainRoute> {
        TrainScreen(
            onOpenLibrary = onOpenLibrary,
            onOpenTemplates = { navController.navigate(TemplateGalleryRoute) },
            onOpenProgram = { navController.navigate(ProgramBuilderRoute(it)) },
            onOpenDay = { navController.navigateToDayPreview(it) },
        )
    }
    composable<TemplateGalleryRoute> {
        TemplateGalleryScreen(
            onBack = navController::popBackStack,
            onProgramCreated = { id ->
                navController.popBackStack()
                navController.navigate(ProgramBuilderRoute(id))
            },
        )
    }
    composable<ProgramBuilderRoute> {
        ProgramBuilderScreen(
            onBack = navController::popBackStack,
            onOpenDay = { navController.navigate(DayEditorRoute(it)) },
            onOpenProgram = { navController.navigate(ProgramBuilderRoute(it)) },
        )
    }
    composable<DayEditorRoute> {
        DayEditorScreen(
            onBack = navController::popBackStack,
            onAddExercises = onPickExercises,
        )
    }
}

/** Outside the Train graph so it can be opened from Today without switching tabs. */
fun NavGraphBuilder.dayPreviewScreen(navController: NavController, onEditDay: (String) -> Unit) {
    composable<DayPreviewRoute> {
        DayPreviewScreen(onBack = navController::popBackStack, onEditDay = onEditDay)
    }
}
