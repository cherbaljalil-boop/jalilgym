package com.chdjalil.jalilgym.feature.programs.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.programs.TrainScreen
import kotlinx.serialization.Serializable

@Serializable
data object TrainRoute

fun NavController.navigateToTrain(navOptions: NavOptions? = null) = navigate(TrainRoute, navOptions)

fun NavGraphBuilder.trainScreen(onOpenLibrary: () -> Unit) {
    composable<TrainRoute> { TrainScreen(onOpenLibrary = onOpenLibrary) }
}
