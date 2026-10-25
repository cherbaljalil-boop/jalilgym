package com.chdjalil.jalilgym.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.chdjalil.jalilgym.R
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.feature.nutrition.navigation.NutritionGraph
import com.chdjalil.jalilgym.feature.profile.navigation.ProfileGraph
import com.chdjalil.jalilgym.feature.progress.navigation.ProgressGraph
import com.chdjalil.jalilgym.feature.today.navigation.TodayRoute
import kotlin.reflect.KClass

/** The five bottom-navigation tabs (docs/BLUEPRINT.md §4). */
enum class TopLevelDestination(
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
    @StringRes val label: Int,
    val route: Any,
    val routeClass: KClass<*>,
) {
    TODAY(JgIcons.Home, JgIcons.HomeFilled, R.string.tab_today, TodayRoute, TodayRoute::class),
    TRAIN(JgIcons.Train, JgIcons.TrainFilled, R.string.tab_train, TrainGraph, TrainGraph::class),
    NUTRITION(
        JgIcons.Nutrition,
        JgIcons.NutritionFilled,
        R.string.tab_nutrition,
        NutritionGraph,
        NutritionGraph::class,
    ),
    PROGRESS(JgIcons.Progress, JgIcons.ProgressFilled, R.string.tab_progress, ProgressGraph, ProgressGraph::class),
    PROFILE(JgIcons.Profile, JgIcons.ProfileFilled, R.string.tab_profile, ProfileGraph, ProfileGraph::class),
}
