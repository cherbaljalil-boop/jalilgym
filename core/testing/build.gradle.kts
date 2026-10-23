plugins {
    alias(libs.plugins.jalilgym.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.truth)
    api(libs.turbine)
}
