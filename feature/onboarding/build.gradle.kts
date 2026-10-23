plugins {
    alias(libs.plugins.jalilgym.android.feature)
}

android {
    namespace = "com.chdjalil.jalilgym.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
