package com.chdjalil.jalilgym.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBar
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBarItem
import com.chdjalil.jalilgym.navigation.JalilgymNavHost
import com.chdjalil.jalilgym.navigation.TopLevelDestination
import com.chdjalil.jalilgym.feature.onboarding.navigation.OnboardingRoute
import com.chdjalil.jalilgym.feature.programs.navigation.DayPreviewRoute
import com.chdjalil.jalilgym.feature.today.navigation.TodayRoute

@Composable
fun JalilgymApp(
    startWithOnboarding: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Onboarding and the workout preview are full-screen flows without the tab bar.
    val fullScreen = currentDestination?.let { it.hasRoute(OnboardingRoute::class) || it.hasRoute(DayPreviewRoute::class) }
        ?: startWithOnboarding

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!fullScreen) JgNavigationBar {
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
