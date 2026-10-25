package com.chdjalil.jalilgym.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chdjalil.jalilgym.feature.onboarding.OnboardingScreen
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

/** [onFinished] runs once answers are saved; the host should replace onboarding with the home screen. */
fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> { OnboardingScreen(onFinished = onFinished) }
}
