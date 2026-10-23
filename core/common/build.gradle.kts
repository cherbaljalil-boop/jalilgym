plugins {
    alias(libs.plugins.jalilgym.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}
