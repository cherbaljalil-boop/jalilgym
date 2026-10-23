package com.chdjalil.jalilgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.chdjalil.jalilgym.feature.exercises.navigation.exerciseScreens
import com.chdjalil.jalilgym.feature.exercises.navigation.navigateToExerciseLibrary
import com.chdjalil.jalilgym.feature.nutrition.navigation.nutritionScreen
import com.chdjalil.jalilgym.feature.onboarding.navigation.OnboardingRoute
import com.chdjalil.jalilgym.feature.onboarding.navigation.onboardingScreen
import com.chdjalil.jalilgym.feature.profile.navigation.profileScreen
import com.chdjalil.jalilgym.feature.programs.navigation.TrainRoute
import com.chdjalil.jalilgym.feature.programs.navigation.trainScreen
import com.chdjalil.jalilgym.feature.progress.navigation.progressScreen
import com.chdjalil.jalilgym.feature.today.navigation.TodayRoute
import com.chdjalil.jalilgym.feature.today.navigation.todayScreen
import kotlinx.serialization.Serializable

/** The Train tab: programs plus the exercise library, so the tab stays selected inside the library. */
@Serializable
data object TrainGraph

@Composable
fun JalilgymNavHost(
    navController: NavHostController,
    startWithOnboarding: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = if (startWithOnboarding) OnboardingRoute else TodayRoute,
        modifier = modifier,
    ) {
        onboardingScreen(
            onFinished = {
                navController.navigate(TodayRoute) {
                    popUpTo<OnboardingRoute> { inclusive = true }
                }
            },
        )
        todayScreen()
        navigation<TrainGraph>(startDestination = TrainRoute) {
            trainScreen(onOpenLibrary = { navController.navigateToExerciseLibrary() })
            exerciseScreens(navController)
        }
        nutritionScreen()
        progressScreen()
        profileScreen()
    }
}
