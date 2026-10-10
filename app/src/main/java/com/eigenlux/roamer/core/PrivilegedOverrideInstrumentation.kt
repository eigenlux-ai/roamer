package com.eigenlux.roamer.core

import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.os.Process
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.util.Log
import rikka.shizuku.Shizuku
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
        SystemServices.ensureHiddenApiAccess()
    }

    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        Thread { runPrivileged(arguments) }.start()
    }

    private fun runPrivileged(arguments: Bundle) {
        val result = Bundle()
        val log = StringBuilder("\n--- RoamerPriv ---\n")
        var error: Throwable? = null
        val failures = linkedMapOf<Int, String>()
        try {
            val action = arguments.getString("action")?.takeIf { it.isNotBlank() } ?: "set"
            val subIds = resolveSubIds(arguments)
            require(subIds.isNotEmpty()) { "cannot resolve subId" }

            val ctx = waitForTargetContext()
            waitForShizukuBinder()
            val mgr = ctx.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
            log.append("action=$action subIds=${subIds.joinToString(",")}\n")

            withShellPermissionIdentity {
                for (subId in subIds) {
                    try {
                        applyAction(action, subId, mgr, ctx, arguments, log)
                        runCatching { notifyConfigChanged(subId) }
                            .onFailure { log.append("notify failed (ignored subId=$subId): ${it.message}\n") }
                    } catch (t: Throwable) {
                        val cause = t.unwrap()
                        failures[subId] = cause.message ?: cause.javaClass.simpleName
                        log.append("subId=$subId failed (continuing other SIMs): ${cause.message}\n")
                        Log.e(TAG, "applyAction subId=$subId failed", cause)
                    }
                }
                require(failures.size < subIds.size) { "all ${subIds.size} SIMs failed (subId=${failures.keys.joinToString(",")})" }
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
        val outcome = InstrumentationOutcome(
            ok = error == null,
            error = error?.let { it.message ?: it.javaClass.simpleName },
            failures = failures,
        )
        // Report only after finish(), so a follow-up run never starts while this one is still registered.
        try {
            finish(if (error == null) 0 else 1, result)
        } finally {
            InstrumentationTrigger.report(arguments.getString(InstrumentationTrigger.EXTRA_TOKEN), outcome)
        }
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

            "restore", "clear" -> revert(subId, mgr, ctx, log, force = action == "clear")

            else -> throw IllegalArgumentException("unknown action=$action")
        }
    }

    /**
     * Writes the real ISO and carrier name back, then clears the override layer. Clearing alone
     * leaves the last overridden values in place. With [force] the layer is cleared even when the
     * write-back has not settled; otherwise the write-back is kept so it can still take effect.
     */
    private fun revert(subId: Int, mgr: CarrierConfigManager, ctx: Context, log: StringBuilder, force: Boolean) {
        val tag = if (force) "CLEAR" else "RESTORE"
        val realIso = MccUtil.countryFromMcc(readMccMnc(ctx, subId))
        val realName = readCarrierIdName(ctx, subId)
        if (realIso.isBlank()) {
            overrideConfig(mgr, subId, null)
            log.append("$tag subId=$subId OK(clear only, cannot derive real iso)\n")
            return
        }
        val bundle = PersistableBundle().apply { putString(KEY_ISO, realIso) }
        if (realName.isNotBlank()) {
            bundle.putBoolean(KEY_NAME_BOOL, true)
            bundle.putString(KEY_NAME, realName)
        }
        overrideConfig(mgr, subId, bundle)
        runCatching { notifyConfigChanged(subId) }
        val settled = pollEffectiveIso(ctx, subId, realIso, 3000L)
        if (settled || force) {
            overrideConfig(mgr, subId, null)
            log.append("$tag subId=$subId OK(clean, settled=$settled)-> iso='$realIso' name='$realName'\n")
        } else {
            log.append("$tag subId=$subId OK(writeback retained, layer not yet cleared)-> iso='$realIso' name='$realName'\n")
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

    private fun activityManager(): Any =
        SystemServices.shizukuInterface(Context.ACTIVITY_SERVICE, "android.app.IActivityManager")

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
        val loader = SystemServices.shizukuInterface(
            Context.CARRIER_CONFIG_SERVICE,
            "com.android.internal.telephony.ICarrierConfigLoader",
        )
        loader.javaClass.getMethod("notifyConfigChangedForSubId", Int::class.javaPrimitiveType)
            .invoke(loader, subId)
    }

    private fun readMccMnc(ctx: Context, subId: Int): String = runCatching {
        (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .createForSubscriptionId(subId).simOperator.orEmpty()
    }.getOrDefault("")

    private fun readCarrierIdName(ctx: Context, subId: Int): String = runCatching {
        (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .createForSubscriptionId(subId).simCarrierIdName?.toString().orEmpty()
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
