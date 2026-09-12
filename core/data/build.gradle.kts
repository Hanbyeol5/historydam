plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
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

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
