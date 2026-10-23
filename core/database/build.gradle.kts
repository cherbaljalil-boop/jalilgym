plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.android.room)
    alias(libs.plugins.jalilgym.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chdjalil.jalilgym.core.database"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}

// Migration tests (Robolectric) read the exported schemas as test assets.
extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    sourceSets.getByName("test").assets.srcDir(file("schemas"))
}
