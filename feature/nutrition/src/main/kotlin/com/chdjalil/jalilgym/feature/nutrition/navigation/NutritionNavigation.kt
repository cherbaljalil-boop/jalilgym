package com.chdjalil.jalilgym.feature.nutrition.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryActions
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryScreen
import com.chdjalil.jalilgym.feature.nutrition.food.CustomFoodScreen
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailScreen
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceryScreen
import com.chdjalil.jalilgym.feature.nutrition.ideas.MealIdeasScreen
import com.chdjalil.jalilgym.feature.nutrition.meals.SavedMealsScreen
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipeEditorScreen
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipesScreen
import com.chdjalil.jalilgym.feature.nutrition.scan.BarcodeScanScreen
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchActions
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchScreen
import com.chdjalil.jalilgym.feature.nutrition.setup.SetupScreen
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable

/** The Nutrition tab, so the tab stays selected on its inner screens. */
@Serializable
data object NutritionGraph

@Serializable
data object DiaryRoute

/** First-time setup and, later, editing targets. */
@Serializable
data object NutritionSetupRoute

/** [date] is ISO-8601; [meal] is a [MealType] name. */
@Serializable
data class FoodSearchRoute(val date: String, val meal: String)

@Serializable
data class FoodDetailRoute(val foodId: String, val date: String, val meal: String, val entryId: String? = null)

@Serializable
data class CustomFoodRoute(val date: String, val meal: String, val barcode: String? = null)

@Serializable
data class SavedMealsRoute(val date: String, val meal: String)

/** Without a date or meal it logs to today's meal for the current time of day. */
@Serializable
data class BarcodeScanRoute(val date: String? = null, val meal: String? = null)

@Serializable
data object RecipesRoute

@Serializable
data object MealIdeasRoute

@Serializable
data object GroceryRoute

/** Null [recipeId] creates a new recipe. */
@Serializable
data class RecipeEditorRoute(val recipeId: String? = null)

/** Argument keys match the route property names. */
internal const val DATE_ARG = "date"
internal const val MEAL_ARG = "meal"
internal const val FOOD_ID_ARG = "foodId"
internal const val ENTRY_ID_ARG = "entryId"
internal const val BARCODE_ARG = "barcode"
internal const val RECIPE_ID_ARG = "recipeId"

const val NUTRITION_SCAN_DEEP_LINK = "jalilgym://nutrition/scan"

/** A sensible default meal for "log something now". */
internal fun mealForNow(time: LocalTime = LocalTime.now()): MealType = when (time.hour) {
    in 4..10 -> MealType.BREAKFAST
    in 11..15 -> MealType.LUNCH
    in 17..21 -> MealType.DINNER
    else -> MealType.SNACK
}

fun NavController.navigateToNutrition(navOptions: NavOptions? = null) = navigate(NutritionGraph, navOptions)

fun NavGraphBuilder.nutritionGraph(navController: NavController) {
    navigation<NutritionGraph>(startDestination = DiaryRoute) {
        composable<DiaryRoute> {
            DiaryScreen(
                DiaryActions(
                    onAddFood = { date, meal -> navController.navigate(FoodSearchRoute("$date", meal.name)) },
                    onScan = { date, meal -> navController.navigate(BarcodeScanRoute("$date", meal.name)) },
                    onEditEntry = { entry ->
                        entry.foodId?.let { navController.navigate(FoodDetailRoute(it, "${entry.date}", entry.meal.name, entry.id)) }
                    },
                    onOpenSavedMeals = { date, meal -> navController.navigate(SavedMealsRoute("$date", meal.name)) },
                    onOpenSetup = { navController.navigate(NutritionSetupRoute) },
                    onOpenRecipes = { navController.navigate(RecipesRoute) },
                    onOpenMealIdeas = { navController.navigate(MealIdeasRoute) },
                    onOpenGroceries = { navController.navigate(GroceryRoute) },
                ),
            )
        }
        composable<NutritionSetupRoute> {
            SetupScreen(onBack = navController::popBackStack, onDone = navController::popBackStack)
        }
        composable<FoodSearchRoute> { entry ->
            val route = entry.toRoute<FoodSearchRoute>()
            FoodSearchScreen(
                FoodSearchActions(
                    onBack = navController::popBackStack,
                    onOpenFood = { navController.navigate(FoodDetailRoute(it, route.date, route.meal)) },
                    onScan = { navController.navigate(BarcodeScanRoute(route.date, route.meal)) },
                    onCreateFood = { navController.navigate(CustomFoodRoute(route.date, route.meal)) },
                ),
            )
        }
        composable<FoodDetailRoute> {
            FoodDetailScreen(
                onBack = navController::popBackStack,
                // Back to the diary after logging, past search or scan.
                onDone = { navController.popBackStack(DiaryRoute, inclusive = false) },
            )
        }
        composable<CustomFoodRoute> { entry ->
            val route = entry.toRoute<CustomFoodRoute>()
            CustomFoodScreen(
                onBack = navController::popBackStack,
                onSaved = { foodId ->
                    navController.navigate(FoodDetailRoute(foodId, route.date, route.meal)) {
                        popUpTo<CustomFoodRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<MealIdeasRoute> {
            MealIdeasScreen(
                onBack = navController::popBackStack,
                onOpenRecipe = { navController.navigate(RecipeEditorRoute(it)) },
                onOpenGroceries = { navController.navigate(GroceryRoute) },
                onOpenSetup = { navController.navigate(NutritionSetupRoute) },
            )
        }
        composable<GroceryRoute> { GroceryScreen(onBack = navController::popBackStack) }
        composable<RecipesRoute> {
            RecipesScreen(onBack = navController::popBackStack, onOpenRecipe = { navController.navigate(RecipeEditorRoute(it)) })
        }
        composable<RecipeEditorRoute> {
            RecipeEditorScreen(
                onBack = navController::popBackStack,
                onLogServing = { foodId -> navController.navigate(FoodDetailRoute(foodId, "${LocalDate.now()}", mealForNow().name)) },
                onOpenGroceries = { navController.navigate(GroceryRoute) },
            )
        }
        composable<SavedMealsRoute> { SavedMealsScreen(onBack = navController::popBackStack) }
        composable<BarcodeScanRoute>(deepLinks = listOf(navDeepLink<BarcodeScanRoute>(basePath = NUTRITION_SCAN_DEEP_LINK))) { entry ->
            val route = entry.toRoute<BarcodeScanRoute>()
            val date = route.date ?: "${LocalDate.now()}"
            val meal = route.meal ?: mealForNow().name
            BarcodeScanScreen(
                onBack = navController::popBackStack,
                onFound = { foodId ->
                    navController.navigate(FoodDetailRoute(foodId, date, meal)) {
                        popUpTo<BarcodeScanRoute> { inclusive = true }
                    }
                },
                onCreateFood = { barcode ->
                    navController.navigate(CustomFoodRoute(date, meal, barcode)) {
                        popUpTo<BarcodeScanRoute> { inclusive = true }
                    }
                },
            )
        }
    }
}
