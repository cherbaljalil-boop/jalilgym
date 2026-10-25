import com.chdjalil.jalilgym.buildlogic.lib
import com.chdjalil.jalilgym.buildlogic.libs
import com.chdjalil.jalilgym.buildlogic.pluginId
import org.gradle.api.Project

/** Applies the Compose compiler and adds the Compose BOM-managed dependencies. */
internal fun Project.configureCompose() {
    pluginManager.apply(libs.pluginId("kotlin-compose"))
    dependencies.apply {
        val bom = platform(libs.lib("androidx-compose-bom"))
        add("implementation", bom)
        add("testImplementation", bom)
        add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
        add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
        add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
        add("testImplementation", libs.lib("robolectric"))
        add("testImplementation", libs.lib("androidx-test-ext-junit"))
    }
}
