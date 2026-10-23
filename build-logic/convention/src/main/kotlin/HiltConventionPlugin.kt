import com.chdjalil.jalilgym.buildlogic.lib
import com.chdjalil.jalilgym.buildlogic.libs
import com.chdjalil.jalilgym.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Hilt for Android modules, with KSP as the annotation processor. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("ksp"))
        pluginManager.apply(libs.pluginId("hilt"))
        dependencies.apply {
            add("implementation", libs.lib("hilt-android"))
            add("ksp", libs.lib("hilt-compiler"))
        }
    }
}
