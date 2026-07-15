package com.eigenlux.roamer.core

import android.content.Context
import android.os.IBinder
import android.os.LocaleList
import android.os.Process
import android.util.Log
import com.eigenlux.roamer.data.AppLocaleStore
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Per-app region override: write/read a target app's per-app locale through the Shizuku binder
 * (shell identity holds CHANGE_CONFIGURATION), and keep the enrolled apps in sync with the primary
 * slot's override state.
 *
 * Uses `ILocaleManager` directly (service "locale"), mirroring [PrivilegedSubscriptionReader]. Unlike
 * the CarrierConfig `overrideConfig` path, `setApplicationLocales` is **not** guarded against the shell
 * caller on this device (verified: `cmd locale set-app-locales` works under plain shell), so no
 * instrumentation / delegation dance is needed.
 *
 * All methods block on binder calls → callers must invoke them off the main thread (as the SIM ops do,
 * via `withContext(Dispatchers.IO)`). Every path degrades gracefully to a no-op on failure.
 */
object LocaleOverrideController {

    private const val TAG = "RoamerLocale"

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
    }

    /** The `ILocaleManager` bound through Shizuku (calls run as the shell identity). */
    private fun localeManager(): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, Context.LOCALE_SERVICE) as IBinder
        return Class.forName("android.app.ILocaleManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))!!   // asInterface never returns null for a live binder
    }

    /** userId from the app's own uid (PER_USER_RANGE = 100000); scoped to the current user. */
    private fun userId(): Int = Process.myUid() / 100000

    /**
     * The ILocaleManager AIDL signature drifts across versions (API 34 added a trailing `boolean
     * fromDelegate` to setApplicationLocales), so resolve the method by name and fill arguments by
     * parameter type instead of hardcoding one signature.
     */
    private fun invokeByName(ilm: Any, name: String, pkg: String, locales: LocaleList?): Any? {
        val method = ilm.javaClass.methods.first { it.name == name }
        var firstString = true
        val args = method.parameterTypes.map { t ->
            when {
                t == String::class.java && firstString -> { firstString = false; pkg }
                t == String::class.java -> "com.android.shell"          // any trailing callingPackage
                t == Int::class.javaPrimitiveType -> userId()
                t == LocaleList::class.java -> locales ?: LocaleList.getEmptyLocaleList()
                t == Boolean::class.javaPrimitiveType -> false           // fromDelegate
                else -> null
            }
        }.toTypedArray()
        return method.invoke(ilm, *args)
    }

    /** Read [pkg]'s current per-app locale as comma-joined BCP-47 tags ("" = system-follow / none). */
    fun readAppLocale(pkg: String): String {
        if (!ShizukuManager.hasPermission()) return ""
        return runCatching {
            (invokeByName(localeManager(), "getApplicationLocales", pkg, null) as? LocaleList)
                ?.toLanguageTags().orEmpty()
        }.getOrElse {
            Log.w(TAG, "readAppLocale($pkg) failed", it)
            ""
        }
    }

    /** Set [pkg]'s per-app locale to [bcp47] (comma-separated tags; "" clears the override). */
    fun applyAppLocale(pkg: String, bcp47: String): Boolean {
        if (!ShizukuManager.hasPermission()) return false
        return runCatching {
            val locales = LocaleList.forLanguageTags(bcp47) // "" -> empty list -> clears the override
            invokeByName(localeManager(), "setApplicationLocales", pkg, locales)
            true
        }.getOrElse {
            Log.w(TAG, "applyAppLocale($pkg, '$bcp47') failed", it)
            false
        }
    }

    private fun tagsEqual(a: String, b: String): Boolean =
        LocaleList.forLanguageTags(a) == LocaleList.forLanguageTags(b)

    // All state mutators below run off-main from three independent sites (master Switch, each picker
    // checkbox, run{}/refreshAll sync). Serialize them on one monitor so the store's get→edit→put
    // sequences and enroll's capture-baseline-then-apply are atomic — otherwise a race can clobber the
    // enrolled/baseline sets or capture an already-overridden value as the baseline (a permanent, un-
    // clearable override that breaks the "device clean" invariant). The monitor is reentrant, so
    // enroll/setMaster calling sync() is fine.
    private val lock = Any()

    /**
     * Re-apply the desired locale to every enrolled app (writes only the diffs). Called on every sync
     * point (after a SIM op, on refresh). When the master switch is off or the primary slot is not
     * overridden, [RegionLogic.desiredTag] resolves to each app's baseline → this restores them.
     */
    fun sync(ctx: Context, primary: RegionLogic.PrimaryState) = synchronized(lock) {
        if (!ShizukuManager.hasPermission()) return
        val masterOn = AppLocaleStore.isMasterOn(ctx)
        for (pkg in AppLocaleStore.enrolled(ctx)) {
            val baseline = AppLocaleStore.baselineOf(ctx, pkg) ?: ""
            val desired = RegionLogic.desiredTag(masterOn, primary, baseline)
            if (!tagsEqual(readAppLocale(pkg), desired)) applyAppLocale(pkg, desired)
        }
    }

    /** Enroll [pkg]: capture its baseline (once), mark enrolled, then sync so it takes effect immediately. */
    fun enroll(ctx: Context, pkg: String, primary: RegionLogic.PrimaryState) = synchronized(lock) {
        // Never capture a baseline without Shizuku: readAppLocale would return "" on failure and a later
        // unenroll would then wipe a locale the user actually set. Bail so we don't record a wrong baseline.
        if (!ShizukuManager.hasPermission()) return
        if (AppLocaleStore.baselineOf(ctx, pkg) == null) {
            AppLocaleStore.setBaseline(ctx, pkg, readAppLocale(pkg))
        }
        AppLocaleStore.setEnrolled(ctx, pkg, true)
        sync(ctx, primary)
    }

    /** Unenroll [pkg]: restore its captured baseline, then drop the enrollment and baseline. */
    fun unenroll(ctx: Context, pkg: String) = synchronized(lock) {
        // Only drop the state once the restore actually landed. If the write fails (Shizuku died while the
        // picker was open, or the binder call threw), clearing the baseline would strand the override for
        // good: the app is no longer enrolled, so sync() never revisits it, and the value to restore is
        // gone. Keeping both lets the next sync() / unenroll finish the job — mirrors enroll's guard, and
        // matches master-off, which stays recoverable precisely because it preserves the baseline.
        if (!applyAppLocale(pkg, AppLocaleStore.baselineOf(ctx, pkg) ?: "")) return
        AppLocaleStore.setEnrolled(ctx, pkg, false)
        AppLocaleStore.clearBaseline(ctx, pkg)
    }

    /** Toggle the master switch and reconcile: off → every enrolled app restores to baseline. */
    fun setMaster(ctx: Context, on: Boolean, primary: RegionLogic.PrimaryState) = synchronized(lock) {
        AppLocaleStore.setMasterOn(ctx, on)
        sync(ctx, primary)
    }
}
