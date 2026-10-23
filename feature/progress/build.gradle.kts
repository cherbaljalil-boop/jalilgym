plugins {
    alias(libs.plugins.jalilgym.android.feature)
}

android {
    namespace = "com.chdjalil.jalilgym.feature.progress"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
}
