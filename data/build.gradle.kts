plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.notaskflow.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.20:8080/\"")
            buildConfigField("String", "COLLAB_WS_URL", "\"ws://192.168.1.20:3000/ws\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.notaskflow.com/\"")
            buildConfigField("String", "COLLAB_WS_URL", "\"wss://api.notaskflow.com/ws\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.codegen)
}
