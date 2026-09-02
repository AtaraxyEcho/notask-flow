plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.notaskflow.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose & UI
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material3)
    api(libs.compose.material.icons.extended)
    api(libs.compose.navigation)
    api(libs.lifecycle.runtime.compose)
    api(libs.lifecycle.viewmodel.compose)
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)
    api(libs.androidx.core.ktx)
    api(libs.activity.compose)

    // Networking (api 暴露给 data 模块)
    api(libs.retrofit)
    api(libs.retrofit.moshi)
    api(libs.moshi)
    ksp(libs.moshi.codegen)
    api(libs.okhttp)
    api(libs.okhttp.logging)

    // Room (api 暴露给 data 模块)
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
    api(libs.room.paging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Paging
    api(libs.paging.compose)

    // Coroutines
    implementation(libs.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
