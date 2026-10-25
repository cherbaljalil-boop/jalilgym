plugins {
    alias(libs.plugins.jalilgym.android.feature)
}

android {
    namespace = "com.chdjalil.jalilgym.feature.exercises"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
