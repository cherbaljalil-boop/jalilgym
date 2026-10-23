package com.chdjalil.jalilgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.chdjalil.jalilgym.feature.exercises.navigation.exerciseScreens
import com.chdjalil.jalilgym.feature.exercises.navigation.navigateToExerciseLibrary
import com.chdjalil.jalilgym.feature.exercises.navigation.navigateToExercisePicker
import com.chdjalil.jalilgym.feature.nutrition.navigation.nutritionScreen
import com.chdjalil.jalilgym.feature.onboarding.navigation.OnboardingRoute
import com.chdjalil.jalilgym.feature.onboarding.navigation.onboardingScreen
import com.chdjalil.jalilgym.feature.profile.navigation.profileScreen
import com.chdjalil.jalilgym.feature.programs.navigation.DayEditorRoute
import com.chdjalil.jalilgym.feature.programs.navigation.TrainRoute
import com.chdjalil.jalilgym.feature.programs.navigation.dayPreviewScreen
import com.chdjalil.jalilgym.feature.programs.navigation.navigateToDayPreview
import com.chdjalil.jalilgym.feature.programs.navigation.trainScreens
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
    onSelectTab: (TopLevelDestination) -> Unit,
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
        todayScreen(
            onOpenDay = navController::navigateToDayPreview,
            onChooseProgram = { onSelectTab(TopLevelDestination.TRAIN) },
        )
        navigation<TrainGraph>(startDestination = TrainRoute) {
            trainScreens(
                navController = navController,
                onOpenLibrary = { navController.navigateToExerciseLibrary() },
                onPickExercises = { navController.navigateToExercisePicker() },
            )
            exerciseScreens(navController)
        }
        dayPreviewScreen(navController, onEditDay = { navController.navigate(DayEditorRoute(it)) })
        nutritionScreen()
        progressScreen()
        profileScreen()
    }
}
