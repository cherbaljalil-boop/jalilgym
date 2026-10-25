package com.chdjalil.jalilgym.feature.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.chdjalil.jalilgym.feature.progress.body.BodyScreen
import com.chdjalil.jalilgym.feature.progress.hub.ProgressScreen
import com.chdjalil.jalilgym.feature.progress.photos.PhotoCompareScreen
import com.chdjalil.jalilgym.feature.progress.photos.PhotosScreen
import com.chdjalil.jalilgym.feature.progress.records.RecordsScreen
import com.chdjalil.jalilgym.feature.progress.strength.ExerciseProgressScreen
import com.chdjalil.jalilgym.feature.progress.strength.StrengthListScreen
import kotlinx.serialization.Serializable

/** The Progress tab, so the tab stays selected on its inner screens. */
@Serializable
data object ProgressGraph

@Serializable
data object ProgressRoute

@Serializable
data object StrengthListRoute

@Serializable
data class ExerciseProgressRoute(val exerciseId: String)

@Serializable
data object RecordsRoute

@Serializable
data object BodyRoute

@Serializable
data object PhotosRoute

@Serializable
data class PhotoCompareRoute(val beforeId: String, val afterId: String)

/** Argument keys match the route property names. */
internal const val EXERCISE_ID_ARG = "exerciseId"
internal const val BEFORE_ID_ARG = "beforeId"
internal const val AFTER_ID_ARG = "afterId"

fun NavController.navigateToProgress(navOptions: NavOptions? = null) = navigate(ProgressGraph, navOptions)

fun NavGraphBuilder.progressGraph(navController: NavController) {
    navigation<ProgressGraph>(startDestination = ProgressRoute) {
        composable<ProgressRoute> {
            ProgressScreen(
                onOpenStrength = { navController.navigate(StrengthListRoute) },
                onOpenRecords = { navController.navigate(RecordsRoute) },
                onOpenBody = { navController.navigate(BodyRoute) },
                onOpenPhotos = { navController.navigate(PhotosRoute) },
            )
        }
        composable<StrengthListRoute> {
            StrengthListScreen(
                onBack = navController::popBackStack,
                onOpenExercise = { navController.navigate(ExerciseProgressRoute(it)) },
            )
        }
        composable<ExerciseProgressRoute> { ExerciseProgressScreen(onBack = navController::popBackStack) }
        composable<RecordsRoute> { RecordsScreen(onBack = navController::popBackStack) }
        composable<BodyRoute> { BodyScreen(onBack = navController::popBackStack) }
        composable<PhotosRoute> {
            PhotosScreen(
                onBack = navController::popBackStack,
                onCompare = { before, after -> navController.navigate(PhotoCompareRoute(before, after)) },
            )
        }
        composable<PhotoCompareRoute> { PhotoCompareScreen(onBack = navController::popBackStack) }
    }
}
