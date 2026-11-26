plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val legacyApiBaseUrl = providers.gradleProperty("credflowApiBaseUrl")
    .orElse(providers.environmentVariable("CRED_FLOW_API_BASE_URL"))
val configuredApiBaseUrl = legacyApiBaseUrl.getOrElse("")
val configuredDebugApiBaseUrl = providers.gradleProperty("credflowDebugApiBaseUrl")
    .orElse(providers.environmentVariable("CRED_FLOW_DEBUG_API_BASE_URL"))
    .orElse(legacyApiBaseUrl)
    .getOrElse("")
val configuredReleaseApiBaseUrl = providers.gradleProperty("credflowReleaseApiBaseUrl")
    .orElse(providers.environmentVariable("CRED_FLOW_RELEASE_API_BASE_URL"))
    .orElse(configuredApiBaseUrl)
    .getOrElse("")
val escapedApiBaseUrl = configuredApiBaseUrl
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val escapedDebugApiBaseUrl = configuredDebugApiBaseUrl
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val escapedReleaseApiBaseUrl = configuredReleaseApiBaseUrl
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

if (configuredReleaseApiBaseUrl.isNotEmpty()) {
    check(configuredReleaseApiBaseUrl.startsWith("https://", ignoreCase = true)) {
        "credflowReleaseApiBaseUrl must use HTTPS."
    }
}

android {
    namespace = "com.example.credflow"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.credflow"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "CRED_FLOW_API_BASE_URL", "\"$escapedApiBaseUrl\"")
    }

    buildTypes {
        release {
            buildConfigField("String", "CRED_FLOW_API_BASE_URL", "\"$escapedReleaseApiBaseUrl\"")
            optimization {
                enable = false
            }
        }
        debug {
            buildConfigField("String", "CRED_FLOW_API_BASE_URL", "\"$escapedDebugApiBaseUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}