package com.eigenlux.roamer.core

import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.os.Process
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.InvocationTargetException

/**
 * Instrumentation proxy executing within the app process to manage CarrierConfig overrides and restorations.
 * Bypasses OEM shell restrictions by delegating shell permission identity to the app UID.
 */
class PrivilegedOverrideInstrumentation : Instrumentation() {

    private companion object {
        const val TAG = "RoamerPriv"
        val KEY_ISO = CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING
        val KEY_NAME_BOOL = CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL
        val KEY_NAME = CarrierConfigManager.KEY_CARRIER_NAME_STRING
        val KEY_FORCE_HOME = CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL
    }

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")
    }

    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        Thread { runPrivileged(arguments) }.start()
    }

    private fun runPrivileged(arguments: Bundle) {
        val result = Bundle()
        val log = StringBuilder("\n--- RoamerPriv ---\n")
        var error: Throwable? = null
        try {
            val action = arguments.getString("action")?.takeIf { it.isNotBlank() } ?: "set"
            val subIds = resolveSubIds(arguments)
            require(subIds.isNotEmpty()) { "cannot resolve subId" }

            val ctx = waitForTargetContext()
            waitForShizukuBinder()
            val mgr = ctx.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
            log.append("action=$action subIds=${subIds.joinToString(",")}\n")

            val failures = mutableListOf<Int>()
            withShellPermissionIdentity {
                for (subId in subIds) {
                    try {
                        applyAction(action, subId, mgr, ctx, arguments, log)
                        runCatching { notifyConfigChanged(subId) }
                            .onFailure { log.append("notify failed (ignored subId=$subId): ${it.message}\n") }
                    } catch (t: Throwable) {
                        failures += subId
                        log.append("subId=$subId failed (continuing other SIMs): ${t.unwrap().message}\n")
                        Log.e(TAG, "applyAction subId=$subId failed", t.unwrap())
                    }
                }
                require(failures.size < subIds.size) { "all ${subIds.size} SIMs failed (subId=${failures.joinToString(",")})" }
            }
        } catch (t: Throwable) {
            error = t.unwrap()
        }

        if (error == null) {
            result.putBoolean("result", true)
        } else {
            Log.e(TAG, "failed", error)
            log.append("FAILED ${error.javaClass.simpleName}: ${error.message}\n")
            result.putBoolean("result", false)
            result.putString("error", error.message ?: error.javaClass.simpleName)
        }
        Log.i(TAG, log.toString())
        result.putString("stream", log.toString())
        finish(if (error == null) 0 else 1, result)
    }

    private fun applyAction(
        action: String,
        subId: Int,
        mgr: CarrierConfigManager,
        ctx: Context,
        arguments: Bundle,
        log: StringBuilder,
    ) {
        when (action) {
            "set" -> {
                val bundle = PersistableBundle()
                arguments.getString("iso")?.takeIf { it.isNotBlank() }
                    ?.let { bundle.putString(KEY_ISO, it.lowercase()) }
                arguments.getString("name")?.takeIf { it.isNotBlank() }?.let {
                    bundle.putBoolean(KEY_NAME_BOOL, true)
                    bundle.putString(KEY_NAME, it)
                    bundle.putBoolean(KEY_FORCE_HOME, true)
                }
                require(!bundle.isEmpty) { "set has no valid fields" }
                overrideConfig(mgr, subId, bundle)
                log.append("SET subId=$subId OK\n")
            }

            "restore" -> {
                val realIso = MccUtil.countryFromMcc(readMccMnc(ctx, subId))
                if (realIso.isBlank()) {
                    overrideConfig(mgr, subId, null)
                    log.append("RESTORE subId=$subId OK(clear only, cannot derive real iso)\n")
                } else {
                    val bundle = PersistableBundle().apply { putString(KEY_ISO, realIso) }
                    overrideConfig(mgr, subId, bundle)
                    runCatching { notifyConfigChanged(subId) }
                    val settled = pollEffectiveIso(ctx, subId, realIso, 3000L)
                    if (settled) {
                        overrideConfig(mgr, subId, null)
                        log.append("RESTORE subId=$subId OK(clean)-> iso='$realIso'\n")
                    } else {
                        log.append("RESTORE subId=$subId OK(writeback retained, layer not yet cleared)-> iso='$realIso'\n")
                    }
                }
            }

            "clear" -> {
                overrideConfig(mgr, subId, null)
                log.append("CLEAR(all) subId=$subId OK\n")
            }

            else -> throw IllegalArgumentException("unknown action=$action")
        }
    }

    private fun withShellPermissionIdentity(block: () -> Unit) {
        val am = activityManager()
        try {
            try {
                am.javaClass.getMethod(
                    "startDelegateShellPermissionIdentity",
                    Int::class.javaPrimitiveType,
                    Array<String>::class.java,
                ).invoke(am, Process.myUid(), null)
            } catch (e: InvocationTargetException) {
                throw e.targetException ?: e
            }
            block()
        } finally {
            runCatching {
                am.javaClass.getMethod("stopDelegateShellPermissionIdentity").invoke(am)
            }
        }
    }

    private fun activityManager(): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, Context.ACTIVITY_SERVICE) as IBinder
        return Class.forName("android.app.IActivityManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))!!
    }

    private fun pollEffectiveIso(ctx: Context, subId: Int, wantLower: String, timeoutMs: Long): Boolean {
        val tm = (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .createForSubscriptionId(subId)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val cur = runCatching { tm.simCountryIso.orEmpty().lowercase() }.getOrDefault("")
            if (cur == wantLower) return true
            Thread.sleep(120)
        }
        return false
    }

    private fun overrideConfig(mgr: CarrierConfigManager, subId: Int, bundle: PersistableBundle?) {
        val m = mgr.javaClass.getMethod(
            "overrideConfig",
            Int::class.javaPrimitiveType,
            PersistableBundle::class.java,
            Boolean::class.javaPrimitiveType,
        )
        try {
            m.invoke(mgr, subId, bundle, true)
        } catch (e: InvocationTargetException) {
            val cause = e.targetException ?: e
            if (cause is SecurityException) {
                try {
                    m.invoke(mgr, subId, bundle, false)
                    return
                } catch (e2: InvocationTargetException) {
                    throw e2.targetException ?: e2
                }
            }
            throw cause
        }
    }

    private fun notifyConfigChanged(subId: Int) {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, Context.CARRIER_CONFIG_SERVICE) as IBinder
        val loader = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))
        loader.javaClass.getMethod("notifyConfigChangedForSubId", Int::class.javaPrimitiveType)
            .invoke(loader, subId)
    }

    private fun readMccMnc(ctx: Context, subId: Int): String = runCatching {
        (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .createForSubscriptionId(subId).simOperator.orEmpty()
    }.getOrDefault("")

    private fun resolveSubIds(arguments: Bundle): List<Int> {
        arguments.getString("subId")?.takeIf { it.isNotBlank() }?.let { raw ->
            val ids = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it >= 0 }
            if (ids.isNotEmpty()) return ids
        }
        val single = resolveSubId(arguments)
        return if (single >= 0) listOf(single) else emptyList()
    }

    private fun resolveSubId(arguments: Bundle): Int {
        arguments.getString("subId")?.toIntOrNull()?.let { return it }
        val slot = arguments.getString("slot")?.toIntOrNull() ?: return -1
        return runCatching {
            (Class.forName("android.telephony.SubscriptionManager")
                .getMethod("getSubId", Int::class.javaPrimitiveType)
                .invoke(null, slot) as? IntArray)?.firstOrNull() ?: -1
        }.getOrDefault(-1)
    }

    private fun waitForTargetContext(timeoutMs: Long = 8000L): Context {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            targetContext?.let { return it }
            Thread.sleep(50)
        }
        throw IllegalStateException("targetContext wait timed out")
    }

    private fun waitForShizukuBinder(timeoutMs: Long = 8000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return
            Thread.sleep(50)
        }
        throw IllegalStateException("Shizuku not ready")
    }

    private fun Throwable.unwrap(): Throwable =
        (this as? InvocationTargetException)?.targetException ?: this
}
