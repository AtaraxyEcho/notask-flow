plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun normalizeApiBaseUrl(value: String): String = if (value.endsWith("/")) value else "$value/"

fun buildConfigString(value: String): String {
    val escapedValue = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escapedValue\""
}

android {
    namespace = "com.notaskflow.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                buildConfigString(normalizeApiBaseUrl(providers.gradleProperty("notask.debugApiBaseUrl").orElse("http://10.0.2.2:8080/").get()))
            )
            buildConfigField(
                "String",
                "COLLAB_WS_URL",
                buildConfigString(providers.gradleProperty("notask.debugCollabWsUrl").orElse("ws://10.0.2.2:3000/ws").get())
            )
        }
        release {
            buildConfigField(
                "String",
                "BASE_URL",
                buildConfigString(normalizeApiBaseUrl(providers.gradleProperty("notask.releaseApiBaseUrl").orElse("https://api.example.com/").get()))
            )
            buildConfigField(
                "String",
                "COLLAB_WS_URL",
                buildConfigString(providers.gradleProperty("notask.releaseCollabWsUrl").orElse("wss://api.example.com/ws").get())
            )
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
