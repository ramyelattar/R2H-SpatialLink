plugins {
    id("spatiallink.android.library")
    id("spatiallink.android.compose")
}

android {
    namespace = "com.r2h.spatiallink.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
}
