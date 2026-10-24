package com.chdjalil.jalilgym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme

/**
 * Shown by Health Connect when the user asks why Jalilgym wants their health
 * data (the permission rationale and "view permission usage" links).
 */
class HealthPermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JalilgymTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.health_rationale_title), style = MaterialTheme.typography.headlineSmall)
                        listOf(
                            R.string.health_rationale_intro,
                            R.string.health_rationale_write,
                            R.string.health_rationale_read,
                            R.string.health_rationale_private,
                        ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
                        TextButton(onClick = ::finish) { Text(stringResource(R.string.health_rationale_close)) }
                    }
                }
            }
        }
    }
}
