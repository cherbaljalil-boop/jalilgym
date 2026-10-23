plugins {
    alias(libs.plugins.jalilgym.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
}
