plugins {
    id("spatiallink.android.library")
}

android {
    namespace = "com.r2h.spatiallink.connectivity.ble"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:discovery"))
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
}
