plugins {
    alias(libs.plugins.yeoksadam.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.samdori93.yeoksadam.feature.camera"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // CameraX 실시간 프리뷰
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
