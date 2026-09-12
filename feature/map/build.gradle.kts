plugins {
    alias(libs.plugins.yeoksadam.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.samdori93.yeoksadam.feature.map"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.naver.map.compose)
}
