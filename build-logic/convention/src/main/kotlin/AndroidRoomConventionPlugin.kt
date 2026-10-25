import androidx.room.gradle.RoomExtension
import com.chdjalil.jalilgym.buildlogic.lib
import com.chdjalil.jalilgym.buildlogic.libs
import com.chdjalil.jalilgym.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Room with exported schemas (required for migration tests). */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("room"))
            pluginManager.apply(libs.pluginId("ksp"))

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }
            dependencies.apply {
                add("implementation", libs.lib("androidx-room-runtime"))
                add("implementation", libs.lib("androidx-room-ktx"))
                add("ksp", libs.lib("androidx-room-compiler"))
                add("testImplementation", libs.lib("androidx-room-testing"))
            }
        }
    }
}
