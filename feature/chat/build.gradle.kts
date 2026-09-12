plugins {
    alias(libs.plugins.yeoksadam.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.samdori93.yeoksadam.feature.chat"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
