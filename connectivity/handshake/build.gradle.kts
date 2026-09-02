plugins {
    id("spatiallink.android.library")
}

android {
    namespace = "com.r2h.spatiallink.connectivity.handshake"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:handshake"))
    implementation(libs.androidx.annotation)
    implementation(libs.conscrypt.android)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
}
