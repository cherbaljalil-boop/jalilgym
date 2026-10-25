plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.hilt)
}

// Release builds include Health Connect only when gradle.properties says so (off for the first
// Play release); app/build.gradle.kts drops its permissions from the manifest to match.
val healthConnectInRelease = providers.gradleProperty("jalilgym.healthConnectInRelease").map(String::toBoolean).getOrElse(false)

android {
    namespace = "com.chdjalil.jalilgym.core.health"
    buildFeatures.buildConfig = true
    buildTypes {
        getByName("debug") { buildConfigField("boolean", "HEALTH_CONNECT", "true") }
        getByName("release") { buildConfigField("boolean", "HEALTH_CONNECT", "$healthConnectInRelease") }
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
