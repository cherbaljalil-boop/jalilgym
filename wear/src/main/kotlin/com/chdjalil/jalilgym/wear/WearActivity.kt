package com.chdjalil.jalilgym.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme

class WearActivity : ComponentActivity() {

    private val viewModel: WearWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                WearWorkoutScreen(
                    state = state,
                    actions = WearActions(
                        onWeight = viewModel::changeWeight,
                        onReps = viewModel::changeReps,
                        onDone = viewModel::completeSet,
                        onSkipRest = viewModel::skipRest,
                    ),
                )
            }
        }
    }
}
