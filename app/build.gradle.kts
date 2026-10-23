plugins {
    alias(libs.plugins.jalilgym.android.application.compose)
    alias(libs.plugins.jalilgym.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chdjalil.jalilgym"

    defaultConfig {
        applicationId = "com.chdjalil.jalilgym"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.feature.onboarding)
    implementation(projects.feature.today)
    implementation(projects.feature.exercises)
    implementation(projects.feature.programs)
    implementation(projects.feature.nutrition)
    implementation(projects.feature.progress)
    implementation(projects.feature.profile)

    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
