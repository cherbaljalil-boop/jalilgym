plugins {
    alias(libs.plugins.jalilgym.android.application.compose)
}

android {
    namespace = "com.chdjalil.jalilgym.wear"

    defaultConfig {
        // Must match the phone app: the Wearable Data Layer only connects apps with the same package and signing key.
        applicationId = "com.chdjalil.jalilgym"
        // Wear OS 3 and later.
        minSdk = 30
        // Watch and phone builds of one release need different version codes on Play.
        versionCode = 1_000_002
        versionName = "1.1.0"
    }

    // Same upload key as the phone app (docs/RELEASING.md); unsigned without it.
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(projects.core.wear)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}
