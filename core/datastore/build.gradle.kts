plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chdjalil.jalilgym.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    api(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
}
