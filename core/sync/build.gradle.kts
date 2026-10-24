plugins {
    alias(libs.plugins.jalilgym.android.library)
    alias(libs.plugins.jalilgym.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The Supabase project comes from ~/.gradle/gradle.properties or the CI environment,
// never from the repository (docs/supabase/SETUP.md). Empty means cloud sync is off.
val supabaseUrl = providers.gradleProperty("jalilgym.supabaseUrl")
    .orElse(providers.environmentVariable("SUPABASE_URL"))
    .getOrElse("")
val supabaseKey = providers.gradleProperty("jalilgym.supabaseKey")
    .orElse(providers.environmentVariable("SUPABASE_PUBLISHABLE_KEY"))
    .getOrElse("")

android {
    namespace = "com.chdjalil.jalilgym.core.sync"
    buildFeatures.buildConfig = true
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.trimEnd('/')}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
