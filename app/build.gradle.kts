plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.eigenlux.roamer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.eigenlux.roamer"
        minSdk = 31
        targetSdk = 36
        // CI (release workflow) derives these from the git tag; local builds fall back to 1 / "1.0".
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Populated from environment variables in CI (keystore in GitHub Secrets); absent locally,
            // so a local `assembleRelease` simply produces an unsigned APK instead of failing.
            System.getenv("KEYSTORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Only attach the signing config when CI provided a keystore; keeps local release builds working.
            if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Shizuku: borrow shell privileges to call hidden telephony APIs
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    // Bypass hidden-API reflection restrictions
    implementation(libs.hiddenapibypass)
    // Android 12+ system splash screen (branded; consistent API across versions)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}