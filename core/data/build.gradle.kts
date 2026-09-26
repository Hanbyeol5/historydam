plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.samdori93.yeoksadam.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
