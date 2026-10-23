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

fun NavGraphBuilder.todayScreen(
    onOpenDay: (String) -> Unit,
    onChooseProgram: () -> Unit,
    onResumeWorkout: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    composable<TodayRoute> {
        TodayScreen(
            onOpenDay = onOpenDay,
            onChooseProgram = onChooseProgram,
            onResumeWorkout = onResumeWorkout,
            onOpenProgress = onOpenProgress,
        )
    }
}
