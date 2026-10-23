plugins {
    alias(libs.plugins.jalilgym.jvm.library)
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    // Generates Dagger factories for use cases with @Inject constructors.
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
}
