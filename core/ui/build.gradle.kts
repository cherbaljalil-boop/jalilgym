plugins {
    alias(libs.plugins.jalilgym.android.library.compose)
}

android {
    namespace = "com.chdjalil.jalilgym.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(libs.coil.compose)
}
