import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose 를 사용하는 Android 모듈에 적용.
 * Kotlin 2.x 의 Compose Compiler Gradle Plugin 을 적용하고 BOM 기반 의존성을 추가한다.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.findByName("android") as? CommonExtension<*, *, *, *, *, *>
            extension?.let { configureAndroidCompose(it) }
        }
    }
}

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))

        listOf(
            "androidx-compose-ui",
            "androidx-compose-ui-graphics",
            "androidx-compose-ui-tooling-preview",
            "androidx-compose-material3",
            "androidx-compose-material-icons-extended",
            "androidx-lifecycle-runtime-compose",
            "androidx-lifecycle-viewmodel-compose",
        ).forEach { alias ->
            add("implementation", libs.findLibrary(alias).get())
        }

        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    }
}
