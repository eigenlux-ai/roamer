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
 * Manages per-app locale overrides using `ILocaleManager` via Shizuku.
 */
object LocaleOverrideController {

    private const val TAG = "RoamerLocale"

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
    }

    private fun localeManager(): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, Context.LOCALE_SERVICE) as IBinder
        return Class.forName("android.app.ILocaleManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))!!
    }

    private fun userId(): Int = Process.myUid() / 100000

    private fun invokeByName(ilm: Any, name: String, pkg: String, locales: LocaleList?): Any? {
        val method = ilm.javaClass.methods.first { it.name == name }
        var firstString = true
        val args = method.parameterTypes.map { t ->
            when {
                t == String::class.java && firstString -> { firstString = false; pkg }
                t == String::class.java -> "com.android.shell"
                t == Int::class.javaPrimitiveType -> userId()
                t == LocaleList::class.java -> locales ?: LocaleList.getEmptyLocaleList()
                t == Boolean::class.javaPrimitiveType -> false
                else -> null
            }
        }.toTypedArray()
        return method.invoke(ilm, *args)
    }

    /**
     * Reads the current per-app locale for [pkg]. Returns comma-separated BCP-47 tags.
     */
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

    /**
     * Sets the per-app locale for [pkg] to [bcp47]. An empty string clears the override.
     */
    fun applyAppLocale(pkg: String, bcp47: String): Boolean {
        if (!ShizukuManager.hasPermission()) return false
        return runCatching {
            val locales = LocaleList.forLanguageTags(bcp47)
            invokeByName(localeManager(), "setApplicationLocales", pkg, locales)
            true
        }.getOrElse {
            Log.w(TAG, "applyAppLocale($pkg, '$bcp47') failed", it)
            false
        }
    }

    private fun tagsEqual(a: String, b: String): Boolean =
        LocaleList.forLanguageTags(a) == LocaleList.forLanguageTags(b)

    private val lock = Any()

    /**
     * Synchronizes enrolled app locales with current primary SIM override state.
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

    /**
     * Enrolls [pkg] in region overrides, capturing baseline locale if not already stored.
     */
    fun enroll(ctx: Context, pkg: String, primary: RegionLogic.PrimaryState) = synchronized(lock) {
        if (!ShizukuManager.hasPermission()) return
        if (AppLocaleStore.baselineOf(ctx, pkg) == null) {
            AppLocaleStore.setBaseline(ctx, pkg, readAppLocale(pkg))
        }
        AppLocaleStore.setEnrolled(ctx, pkg, true)
        sync(ctx, primary)
    }

    /**
     * Unenrolls [pkg], restoring its baseline locale.
     */
    fun unenroll(ctx: Context, pkg: String) = synchronized(lock) {
        if (!applyAppLocale(pkg, AppLocaleStore.baselineOf(ctx, pkg) ?: "")) return
        AppLocaleStore.setEnrolled(ctx, pkg, false)
        AppLocaleStore.clearBaseline(ctx, pkg)
    }

    /**
     * Toggles master switch state and updates all enrolled app locales.
     */
    fun setMaster(ctx: Context, on: Boolean, primary: RegionLogic.PrimaryState) = synchronized(lock) {
        AppLocaleStore.setMasterOn(ctx, on)
        sync(ctx, primary)
    }
}
