package com.chdjalil.jalilgym

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.ui.AppLanguage
import com.chdjalil.jalilgym.ui.JalilgymApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState = it }
            }
        }
        // Keep the splash until settings are loaded so the theme never flickers.
        splashScreen.setKeepOnScreenCondition { uiState is MainActivityUiState.Loading }

        enableEdgeToEdge()
        setContent {
            val darkTheme = shouldUseDarkTheme(uiState)
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                onDispose {}
            }
            JalilgymTheme(darkTheme = darkTheme) {
                val state = uiState
                if (state is MainActivityUiState.Success) {
                    // Decided once per launch; finishing onboarding navigates away in-app.
                    val startWithOnboarding = remember { !state.onboardingComplete }
                    JalilgymApp(startWithOnboarding = startWithOnboarding)
                }
            }
        }
    }
}

@Composable
private fun shouldUseDarkTheme(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> true
    is MainActivityUiState.Success -> when (uiState.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}
