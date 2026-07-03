package com.eigenlux.roamer.core

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku availability and authorization checks. The prerequisite for root-free override: borrowing
 * the shell (ADB) identity through Shizuku.
 */
object ShizukuManager {

    const val REQUEST_CODE = 2001

    fun isBinderAlive(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** @return true if the binder exists (request in progress or already granted); false if Shizuku is not running */
    fun requestPermission(): Boolean = runCatching {
        if (!isBinderAlive()) return false
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
        true
    }.getOrDefault(false)
}
