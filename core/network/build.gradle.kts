import java.util.Properties

plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties 에서 비밀값을 읽어 BuildConfig 로 노출 (저장소 커밋 금지)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, default: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.samdori93.yeoksadam.core.network"
    buildFeatures { buildConfig = true }

    defaultConfig {
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${secret("API_BASE_URL", "https://api.example.com/")}\"",
        )
        // Gemini 멀티모달 비전 키 — local.properties 의 GEMINI_API_KEY (커밋 금지)
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${secret("GEMINI_API_KEY", "")}\"",
        )
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
}
