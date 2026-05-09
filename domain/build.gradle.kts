plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.notaskflow.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core"))
}
