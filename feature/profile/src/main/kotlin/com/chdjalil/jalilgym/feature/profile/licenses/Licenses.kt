package com.chdjalil.jalilgym.feature.profile.licenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.feature.profile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A third-party component or dataset and the license it ships under. */
internal data class Notice(
    val name: String,
    val holder: String,
    val license: String,
    /** Full text under assets/licenses, or null when the license is only referenced. */
    val textAsset: String?,
    val url: String,
)

private const val APACHE = "licenses/apache-2.0.txt"

/** Everything bundled in or used by the app. Keep in sync with gradle/libs.versions.toml. */
internal val NOTICES = listOf(
    Notice("Android Jetpack (Compose, Room, DataStore, Navigation, Lifecycle, CameraX…)", "The Android Open Source Project", "Apache License 2.0", APACHE, "https://developer.android.com/jetpack"),
    Notice("Dagger and Hilt", "The Dagger Authors", "Apache License 2.0", APACHE, "https://dagger.dev"),
    Notice("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and contributors", "Apache License 2.0", APACHE, "https://kotlinlang.org"),
    Notice("OkHttp", "Square, Inc.", "Apache License 2.0", APACHE, "https://square.github.io/okhttp/"),
    Notice("Coil", "Coil Contributors", "Apache License 2.0", APACHE, "https://coil-kt.github.io/coil/"),
    Notice("ML Kit barcode scanning", "Google LLC", "ML Kit Terms of Service", null, "https://developers.google.com/ml-kit/terms"),
    Notice("Material Symbols", "Google LLC", "Apache License 2.0", APACHE, "https://fonts.google.com/icons"),
    Notice("Inter", "The Inter Project Authors", "SIL Open Font License 1.1", "licenses/ofl-inter.txt", "https://github.com/rsms/inter"),
    Notice("Bebas Neue", "Dharma Type", "SIL Open Font License 1.1", "licenses/ofl-bebas-neue.txt", "https://fonts.google.com/specimen/Bebas+Neue"),
    Notice(
        "free-exercise-db (exercise data and photos)",
        "yuhonas and contributors",
        "Unlicense (public domain)",
        "licenses/unlicense-free-exercise-db.txt",
        "https://github.com/yuhonas/free-exercise-db",
    ),
    Notice(
        "Open Food Facts (food product data)",
        "Open Food Facts contributors",
        "Open Database License (ODbL) 1.0; contents DbCL 1.0; images CC BY-SA",
        null,
        "https://world.openfoodfacts.org/terms-of-use",
    ),
    Notice("USDA FoodData Central (generic food values)", "U.S. Department of Agriculture", "Public domain (CC0 1.0)", null, "https://fdc.nal.usda.gov"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Bar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_profile_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val notice = NOTICES.find { it.name == open }
    // System back closes the license text before leaving the list.
    BackHandler(enabled = notice != null) { open = null }
    if (notice != null) {
        LicenseText(notice, onBack = { open = null })
        return
    }
    Scaffold(
        topBar = { Bar(stringResource(R.string.feature_profile_licenses), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(NOTICES, key = { it.name }) { item ->
                ListItem(
                    modifier = Modifier.clickable { open = item.name },
                    headlineContent = { Text(item.name) },
                    supportingContent = {
                        Text("${item.holder} · ${item.license}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

@Composable
private fun LicenseText(notice: Notice, onBack: () -> Unit) {
    val context = LocalContext.current
    val text by produceState<String?>(null, notice) {
        value = notice.textAsset?.let { asset ->
            withContext(Dispatchers.IO) {
                runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }.getOrNull()
            }
        }
    }
    Scaffold(
        topBar = { Bar(notice.name, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                listOfNotNull("${notice.holder}\n${notice.license}\n${notice.url}", text).joinToString("\n\n"),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (text != null) FontFamily.Monospace else null,
            )
        }
    }
}
