package com.eigenlux.roamer.core

import android.os.IBinder
import android.telephony.SubscriptionInfo
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Read `ISub` directly via the Shizuku binder (the caller within the binder transaction is shell),
 * bypassing the redaction of identifiers such as ICCID for non-privileged Apps —
 * `SubscriptionManagerService#conditionallyRemoveIdentifiers` requires the caller to hold
 * `USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER`, otherwise it forces `setIccId(null)` (see AOSP source). A
 * regular App cannot obtain this permission; the shell identity can. Read-only, changes no state, and
 * does not involve the reject-shell write guard of `overrideConfig`.
 *
 * The signature is taken from the AOSP `SubscriptionManagerService.getActiveSubscriptionInfoList(String, String, boolean)`
 * source (not a guessed reflection signature).
 */
object PrivilegedSubscriptionReader {

    private const val TAG = "RoamerSubRead"

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
    }

    /** subId -> ICCID. Read failure / not authorized / no permission all gracefully degrade to an empty map, and the caller falls back to the App's own read. */
    fun readIccIds(): Map<Int, String> {
        if (!ShizukuManager.hasPermission()) return emptyMap()
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "isub") as IBinder
            val isub = Class.forName("com.android.internal.telephony.ISub\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))

            @Suppress("UNCHECKED_CAST")
            val list = isub.javaClass
                .getMethod(
                    "getActiveSubscriptionInfoList",
                    String::class.java, String::class.java, Boolean::class.javaPrimitiveType,
                )
                .invoke(isub, "com.android.shell", null, false) as? List<SubscriptionInfo>
                ?: emptyList()

            list.associate { it.subscriptionId to it.iccId.orEmpty() }
                .filterValues { it.isNotBlank() }
        }.getOrElse {
            Log.w(TAG, "readIccIds failed", it)
            emptyMap()
        }
    }
}
