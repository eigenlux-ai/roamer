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
 * Privileged proxy: runs inside the App process (targetContext = App) and performs
 * capture-if-absent plus override/restore.
 *
 * Two entry points share this class:
 *  - **In-App (recommended)**: [InstrumentationTrigger] invokes `startInstrumentation` directly
 *    via Shizuku with `INSTR_FLAG_NO_RESTART` → no force-stop, no UI crash.
 *  - **Headless**: `am instrument -w -e action set -e subId N -e iso us -e name A5TEST <pkg>/<this class>`
 *    (adb/Termux; force-stops this package, suitable only for UI-less scenarios).
 *
 * Privilege source: via Shizuku (shell identity), call `startDelegateShellPermissionIdentity(myUid, null)`
 * to **delegate the shell permissions to the App's own uid**. Then inside `overrideConfig`,
 * `getCallingUid()` = App uid (passing Samsung's reject-shell guard), while carrying shell's
 * `MODIFY_PHONE_STATE` (passing the permission check).
 * Reference: public AOSP/Shizuku-based implementations.
 */
class PrivilegedOverrideInstrumentation : Instrumentation() {

    private companion object {
        const val TAG = "RoamerPriv"
        val KEY_ISO = CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING
        val KEY_NAME_BOOL = CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL
        val KEY_NAME = CarrierConfigManager.KEY_CARRIER_NAME_STRING
        // Consistent with public AOSP-based implementations: force home-network display to raise
        // the SPN name override success rate (especially for the secondary SIM)
        val KEY_FORCE_HOME = CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL
    }

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")
    }

    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        // Privileged calls must block-wait (binder/delegation); cannot run on the onCreate main
        // thread → use a dedicated thread.
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

            // 🔴 All subIds are processed sequentially within a **single** shell-identity delegation.
            // stopDelegateShellPermissionIdentity is process-level: if each SIM triggers its own
            // instrumentation, under concurrency the first one to finish drops the whole process's
            // shell identity, so the next SIM's overrideConfig immediately loses privilege → clear
            // fails (manifests as the secondary SIM's carrier failing to restore during "restore all").
            val failures = mutableListOf<Int>()
            withShellPermissionIdentity {
                for (subId in subIds) {
                    // Per-SIM independent try: one SIM throwing must not interrupt the others,
                    // otherwise we get an inconsistent intermediate state (partly restored, partly stale).
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
                // Only mark the whole run as failed when every SIM failed; partial failures are
                // handled by the App-side per-SIM polling that produces the N/M semantics.
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

    /** Apply an action to a single subId (called within an already-delegated shell-identity context). Multiple SIMs share one delegation via the outer loop. */
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
                // No need to save original values: on restore, ISO is re-derived at runtime from the
                // immutable MCC and the carrier name reverts automatically when the override layer is cleared.
                // See memory carrier-name-reverts-baseline-not-needed.
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
                // Fully re-derived at runtime, relying on no saved values:
                //  - real ISO is derived at runtime from the MCC (getSimOperator is immune to override,
                //    so the true value is retrievable at any time)
                //  - the carrier name needs no handling — clearing the layer triggers pull-on-read that
                //    automatically reverts it to the production value (verified on real devices)
                val realIso = MccUtil.countryFromMcc(readMccMnc(ctx, subId))
                if (realIso.isBlank()) {
                    // Cannot obtain the real MCC (no SIM / no service) → can only do a blind clear
                    // (name still reverts; if ISO was tainted this cannot recover it, but that is very rare)
                    overrideConfig(mgr, subId, null)
                    log.append("RESTORE subId=$subId OK(clear only, cannot derive real iso)\n")
                } else {
                    // Two-step restore: ① write back the real ISO (non-empty triggers the subscription
                    //   database to update to the true value)
                    //   ② clear the override layer (clearing does not re-derive → the true ISO is retained;
                    //   the carrier name reverts automatically at the same time)
                    // Result: correct ISO + name back to production value + clean override layer.
                    // If ① has not taken effect in time, ① is retained (benign residue, still correct).
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
                overrideConfig(mgr, subId, null) // Unconditional clear of everything (name reverts automatically; ISO may persist, so the proper restore path uses restore)
                log.append("CLEAR(all) subId=$subId OK\n")
            }

            else -> throw IllegalArgumentException("unknown action=$action")
        }
    }

    /** Via Shizuku (shell), delegate the shell permissions to this process's uid; inside the block we hold MODIFY_PHONE_STATE etc. */
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

    /** Poll whether the real simCountryIso has returned to the expected value (lowercase); used to determine "has it taken effect" for the two-step restore. */
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
            m.invoke(mgr, subId, bundle, true) // Prefer persistent (still effective after reboot)
        } catch (e: InvocationTargetException) {
            val cause = e.targetException ?: e
            // Non-system apps are refused persistent=true on some devices → fall back to non-persistent
            // (lost after reboot, compensated by replaying at boot)
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

    /** Trigger the system to re-read carrier config; invoked via ICarrierConfigLoader (shell identity), best-effort. */
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

    /** Resolve the target subId list: supports comma-separated ("3,2", for batch single-delegation); falls back to single default resolution (legacy behavior). */
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
