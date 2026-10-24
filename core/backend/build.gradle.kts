import java.util.Properties

/**
 * 역사담 백엔드 RAG — historydam backend(backend/main.py)의 인물 대화 로직을 앱 안에서 돌리는 모듈.
 * 의도 분류 → 공공기관 자료 7곳 검색 → Gemini 답변. 서버 없이 휴대폰이 직접 호출한다.
 */
plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties 에서 키를 읽어 BuildConfig 로 노출 (저장소 커밋 금지)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = localProps.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace = "com.samdori93.yeoksadam.core.backend"
    buildFeatures { buildConfig = true }

    defaultConfig {
        // backend/.env 와 같은 값 — local.properties.example 참고
        buildConfigField("String", "GEMINI_API_KEY", "\"${secret("GEMINI_API_KEY")}\"")
        buildConfigField("String", "PUBLIC_API_KEY", "\"${secret("PUBLIC_API_KEY")}\"")
        buildConfigField("String", "KDP_API_KEY", "\"${secret("KDP_API_KEY")}\"")
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
