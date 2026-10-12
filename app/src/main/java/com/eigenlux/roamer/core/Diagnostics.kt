package com.eigenlux.roamer.core

import android.content.Context
import android.os.Build
import rikka.shizuku.Shizuku

/**
 * Builds the environment line that accompanies a failure in the in-app log, so a report
 * carries what is needed to tell apart the causes of a failed privileged run.
 */
object Diagnostics {

    /**
     * Describes the app version, Android release, device brand, Android user, and Shizuku identity.
     */
    fun environment(ctx: Context): String = format(
        version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull().orEmpty(),
        release = Build.VERSION.RELEASE.orEmpty(),
        sdk = Build.VERSION.SDK_INT,
        brand = Build.MANUFACTURER.orEmpty(),
        userId = SystemServices.userId(),
        shizukuUid = runCatching { Shizuku.getUid() }.getOrNull(),
    )

    /**
     * Formats the environment line. A null [shizukuUid] means the Shizuku service is not running.
     */
    fun format(version: String, release: String, sdk: Int, brand: String, userId: Int, shizukuUid: Int?): String =
        listOf(
            "Roamer ${version.ifBlank { "?" }}",
            "Android ${release.ifBlank { "?" }} (API $sdk)",
            brand.ifBlank { "?" },
            "user $userId",
            shizukuUid?.let { "Shizuku uid $it" } ?: "Shizuku not running",
        ).joinToString(" · ")
}
