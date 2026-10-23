package com.chdjalil.jalilgym.feature.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.progress.ProgressScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProgressRoute

fun NavController.navigateToProgress(navOptions: NavOptions? = null) = navigate(ProgressRoute, navOptions)

fun NavGraphBuilder.progressScreen() {
    composable<ProgressRoute> { ProgressScreen() }
}
