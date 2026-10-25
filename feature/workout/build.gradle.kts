plugins {
    alias(libs.plugins.jalilgym.android.feature)
}

android {
    namespace = "com.chdjalil.jalilgym.feature.workout"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(projects.core.wear)
    implementation(libs.play.services.wearable)
}
