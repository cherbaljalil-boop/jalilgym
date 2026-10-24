plugins {
    alias(libs.plugins.jalilgym.android.application.compose)
    alias(libs.plugins.jalilgym.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.chdjalil.jalilgym"

    defaultConfig {
        applicationId = "com.chdjalil.jalilgym"
        versionCode = 2
        versionName = "1.1.0"
    }

    // Release signing comes from the environment so no key or password is ever
    // committed (see docs/RELEASING.md). Without it the release build is unsigned.
    val keystoreFile = providers.environmentVariable("JALILGYM_KEYSTORE_FILE").orNull
    val releaseSigning = keystoreFile?.let { path ->
        signingConfigs.create("release") {
            storeFile = file(path)
            storePassword = providers.environmentVariable("JALILGYM_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("JALILGYM_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("JALILGYM_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
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
    implementation(projects.feature.workout)
    implementation(projects.feature.programs)
    implementation(projects.feature.nutrition)
    implementation(projects.feature.progress)
    implementation(projects.feature.profile)
    implementation(projects.feature.widgets)

    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.health)
    implementation(projects.core.notifications)
    implementation(projects.core.domain)
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
    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.baselineprofile)

    testImplementation(projects.core.testing)
}
