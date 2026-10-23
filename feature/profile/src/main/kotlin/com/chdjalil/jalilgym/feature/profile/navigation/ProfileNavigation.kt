package com.chdjalil.jalilgym.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.chdjalil.jalilgym.feature.profile.ProfileScreen
import com.chdjalil.jalilgym.feature.profile.achievements.AchievementsScreen
import kotlinx.serialization.Serializable

/** The Profile tab, so the tab stays selected on its inner screens. */
@Serializable
data object ProfileGraph

@Serializable
data object ProfileRoute

@Serializable
data object AchievementsRoute

fun NavController.navigateToProfile(navOptions: NavOptions? = null) = navigate(ProfileGraph, navOptions)

fun NavGraphBuilder.profileGraph(navController: NavController) {
    navigation<ProfileGraph>(startDestination = ProfileRoute) {
        composable<ProfileRoute> { ProfileScreen(onOpenAchievements = { navController.navigate(AchievementsRoute) }) }
        composable<AchievementsRoute> { AchievementsScreen(onBack = navController::popBackStack) }
    }
}
