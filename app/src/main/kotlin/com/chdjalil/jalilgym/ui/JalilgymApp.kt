package com.chdjalil.jalilgym.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.util.Consumer
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBar
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBarItem
import com.chdjalil.jalilgym.feature.onboarding.navigation.OnboardingRoute
import com.chdjalil.jalilgym.feature.programs.navigation.DayPreviewRoute
import com.chdjalil.jalilgym.feature.today.navigation.TodayRoute
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutMiniBar
import com.chdjalil.jalilgym.feature.workout.navigation.ActiveWorkoutRoute
import com.chdjalil.jalilgym.feature.workout.navigation.WorkoutSummaryRoute
import com.chdjalil.jalilgym.feature.workout.navigation.navigateToActiveWorkout
import com.chdjalil.jalilgym.navigation.JalilgymNavHost
import com.chdjalil.jalilgym.navigation.TopLevelDestination

@Composable
fun JalilgymApp(
    startWithOnboarding: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Onboarding and the workout screens are full-screen flows without the tab bar.
    val fullScreen = currentDestination?.isFullScreen() ?: startWithOnboarding

    // Notification taps arrive as new intents while the app is open.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!fullScreen) {
                Column {
                    ActiveWorkoutMiniBar(onOpen = navController::navigateToActiveWorkout)
                    JgNavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            JgNavigationBarItem(
                                selected = currentDestination.isOn(destination),
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                                selectedIcon = {
                                    Icon(painterResource(destination.selectedIcon), contentDescription = null)
                                },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        JalilgymNavHost(
            navController = navController,
            startWithOnboarding = startWithOnboarding,
            onSelectTab = { navController.navigateToTopLevel(it) },
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavDestination.isFullScreen(): Boolean =
    hasRoute(OnboardingRoute::class) ||
        hasRoute(DayPreviewRoute::class) ||
        hasRoute(ActiveWorkoutRoute::class) ||
        hasRoute(WorkoutSummaryRoute::class)

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/**
 * Tab switching keeps one copy of each tab and restores its state. Today is the
 * root of the back stack (onboarding, when shown, is removed once finished).
 */
internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<TodayRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
