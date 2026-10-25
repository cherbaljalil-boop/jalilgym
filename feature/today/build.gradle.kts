plugins {
    alias(libs.plugins.jalilgym.android.feature)
}

android {
    namespace = "com.chdjalil.jalilgym.feature.today"
}

dependencies {
    testImplementation(projects.core.uitest)
}
