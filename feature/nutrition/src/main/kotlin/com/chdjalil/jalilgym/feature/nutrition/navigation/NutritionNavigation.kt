package com.chdjalil.jalilgym.feature.nutrition.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.nutrition.NutritionScreen
import kotlinx.serialization.Serializable

@Serializable
data object NutritionRoute

fun NavController.navigateToNutrition(navOptions: NavOptions? = null) = navigate(NutritionRoute, navOptions)

fun NavGraphBuilder.nutritionScreen() {
    composable<NutritionRoute> { NutritionScreen() }
}
