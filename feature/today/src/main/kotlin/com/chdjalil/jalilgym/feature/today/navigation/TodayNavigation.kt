package com.chdjalil.jalilgym.feature.today.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.today.TodayScreen
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

fun NavController.navigateToToday(navOptions: NavOptions? = null) = navigate(TodayRoute, navOptions)

fun NavGraphBuilder.todayScreen() {
    composable<TodayRoute> { TodayScreen() }
}
