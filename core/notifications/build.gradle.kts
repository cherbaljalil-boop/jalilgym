plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.hilt)
}

android {
    namespace = "com.chdjalil.jalilgym.core.notifications"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(libs.androidx.core)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.work.testing)
}
