package com.eigenlux.roamer.core

import android.telephony.SubscriptionInfo
import android.util.Log

/**
 * Reads subscription ICCIDs directly via Shizuku binder IPC to bypass App-side identifier redaction.
 */
object PrivilegedSubscriptionReader {

    private const val TAG = "RoamerSubRead"

    init {
        SystemServices.ensureHiddenApiAccess()
    }

    /**
     * Reads active SIM ICCIDs using shell identity. Returns a map of subId to ICCID.
     */
    fun readIccIds(): Map<Int, String> {
        if (!ShizukuManager.hasPermission()) return emptyMap()
        return runCatching {
            val isub = SystemServices.shizukuInterface("isub", "com.android.internal.telephony.ISub")

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
