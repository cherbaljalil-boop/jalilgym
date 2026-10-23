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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBar
import com.chdjalil.jalilgym.core.designsystem.component.JgNavigationBarItem
import com.chdjalil.jalilgym.navigation.JalilgymNavHost
import com.chdjalil.jalilgym.navigation.TopLevelDestination

@Composable
fun JalilgymApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
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
        },
    ) { innerPadding ->
        JalilgymNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/** Tab switching keeps one copy of each tab and restores its state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
