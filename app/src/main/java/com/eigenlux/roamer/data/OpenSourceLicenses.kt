package com.eigenlux.roamer.data

/**
 * A third-party library shipped in the app.
 *
 * @property license SPDX identifier of its license.
 */
data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val url: String,
)

/**
 * Libraries Roamer is built with, as shown on the About page. Keep in step with the
 * dependencies in `app/build.gradle.kts`.
 */
object OpenSourceLicenses {
    const val SOURCE_URL = "https://github.com/eigenlux-ai/roamer"
    const val APP_LICENSE = "MIT"

    val libraries: List<OpenSourceLibrary> = listOf(
        OpenSourceLibrary("Kotlin", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
        OpenSourceLibrary("kotlinx.coroutines", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
        OpenSourceLibrary("AndroidX", "Apache-2.0", "https://github.com/androidx/androidx"),
        OpenSourceLibrary("Jetpack Compose", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
        OpenSourceLibrary("Material Icons", "Apache-2.0", "https://github.com/google/material-design-icons"),
        OpenSourceLibrary("Shizuku API", "MIT", "https://github.com/RikkaApps/Shizuku-API"),
        OpenSourceLibrary("AndroidHiddenApiBypass", "Apache-2.0", "https://github.com/LSPosed/AndroidHiddenApiBypass"),
        OpenSourceLibrary("Haze", "Apache-2.0", "https://github.com/chrisbanes/haze"),
    )
}
