plugins {
    alias(libs.plugins.jalilgym.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.domain)
    // WearJson is part of the protocol, so users of this module get the library too.
    api(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
