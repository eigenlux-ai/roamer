package com.eigenlux.roamer.core

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Checks Shizuku service status and handles permission requests.
 */
object ShizukuManager {

    const val REQUEST_CODE = 2001

    fun isBinderAlive(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission(): Boolean = runCatching {
        if (!isBinderAlive()) return false
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
        true
    }.getOrDefault(false)
}
