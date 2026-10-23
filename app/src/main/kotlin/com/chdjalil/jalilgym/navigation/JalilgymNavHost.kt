package com.chdjalil.jalilgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.chdjalil.jalilgym.feature.nutrition.navigation.nutritionScreen
import com.chdjalil.jalilgym.feature.profile.navigation.profileScreen
import com.chdjalil.jalilgym.feature.programs.navigation.trainScreen
import com.chdjalil.jalilgym.feature.progress.navigation.progressScreen
import com.chdjalil.jalilgym.feature.today.navigation.TodayRoute
import com.chdjalil.jalilgym.feature.today.navigation.todayScreen

@Composable
fun JalilgymNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TodayRoute,
        modifier = modifier,
    ) {
        todayScreen()
        trainScreen()
        nutritionScreen()
        progressScreen()
        profileScreen()
    }
}
