plugins {
    alias(libs.plugins.jalilgym.android.library.compose)
}

android {
    namespace = "com.chdjalil.jalilgym.core.uitest"
}

// Compose test helpers shared by feature tests (accessibility checks).
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
}
