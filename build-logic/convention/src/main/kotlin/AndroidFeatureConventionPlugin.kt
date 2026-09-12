import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * 모든 feature 모듈 공통 설정.
 * android.library + compose + hilt + 공통 core 의존(domain/ui/designsystem) 을 묶는다.
 * 의존 규칙: feature 는 core:domain/ui/designsystem 만 의존한다.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("yeoksadam.android.library")
            apply("yeoksadam.android.hilt")
            apply("yeoksadam.android.compose")
        }

        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:common"))

            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("coil-compose").get())
        }
    }
}
