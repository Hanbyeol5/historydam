plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
}

android {
    namespace = "com.samdori93.yeoksadam.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
}
