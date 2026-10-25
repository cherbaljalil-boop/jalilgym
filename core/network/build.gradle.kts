plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.hilt)
}

android {
    namespace = "com.chdjalil.jalilgym.core.network"
}

dependencies {
    implementation(projects.core.common)
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
