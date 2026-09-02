plugins {
    id("spatiallink.android.application")
    id("spatiallink.android.compose")
}

android {
    namespace = "com.r2h.spatiallink"

    defaultConfig {
        applicationId = "com.r2h.spatiallink"
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:capabilities"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:discovery"))
    implementation(project(":core:identity"))
    implementation(project(":core:model"))
    implementation(project(":connectivity:ble"))
    implementation(project(":feature:diagnostics"))
    implementation(project(":feature:nearby"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
