plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
}

android {
    namespace = "com.samdori93.yeoksadam.core.database"
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
