package com.eigenlux.roamer.core

import android.os.IBinder
import android.telephony.SubscriptionInfo
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Reads subscription ICCIDs directly via Shizuku binder IPC to bypass App-side identifier redaction.
 */
object PrivilegedSubscriptionReader {

    private const val TAG = "RoamerSubRead"

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
    }

    /**
     * Reads active SIM ICCIDs using shell identity. Returns a map of subId to ICCID.
     */
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
