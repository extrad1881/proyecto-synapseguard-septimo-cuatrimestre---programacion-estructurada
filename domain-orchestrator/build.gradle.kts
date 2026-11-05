plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.synapseguard.domain.orchestrator"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core-model"))
    implementation(project(":core-database"))
    implementation(project(":core-math-ml"))
    implementation(project(":data-ingestors:biometric-ble-hc"))
    implementation(project(":data-ingestors:biometric-camera-ppg"))
    implementation(project(":data-ingestors:telemetry-usage"))
    implementation(project(":data-ingestors:kinematics-accessibility"))
    implementation(project(":data-ingestors:context-sensors"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}

