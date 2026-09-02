plugins {
    id("spatiallink.android.library")
}

android {
    namespace = "com.r2h.spatiallink.capabilities"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
}
