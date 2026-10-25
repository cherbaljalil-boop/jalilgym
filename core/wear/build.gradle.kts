plugins {
    alias(libs.plugins.jalilgym.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.domain)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
