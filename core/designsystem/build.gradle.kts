plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.compose)
}

android {
    namespace = "com.samdori93.yeoksadam.core.designsystem"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
