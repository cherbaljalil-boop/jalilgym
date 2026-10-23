import com.android.build.api.dsl.LibraryExtension
import com.chdjalil.jalilgym.buildlogic.JAVA_VERSION
import com.chdjalil.jalilgym.buildlogic.addCommonTestDependencies
import com.chdjalil.jalilgym.buildlogic.intVersion
import com.chdjalil.jalilgym.buildlogic.libs
import com.chdjalil.jalilgym.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))

            extensions.configure<LibraryExtension> {
                compileSdk = libs.intVersion("compileSdk")
                defaultConfig {
                    minSdk = libs.intVersion("minSdk")
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                testOptions {
                    unitTests.isIncludeAndroidResources = true
                }
            }
            addCommonTestDependencies()
        }
    }
}
