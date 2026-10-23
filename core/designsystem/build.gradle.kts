plugins {
    alias(libs.plugins.jalilgym.android.library.compose)
}

android {
    namespace = "com.chdjalil.jalilgym.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
}
