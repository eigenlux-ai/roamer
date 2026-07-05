package com.eigenlux.roamer.data

import android.content.Context

/**
 * Persistent state for the phase-2 per-app region override (locale-follows-primary-SIM).
 *
 * Holds three things, all in the shared "roamer_prefs" file (written from the foreground UI process,
 * so [apply][android.content.SharedPreferences.Editor.apply] is fine — the commit() caveat only
 * applies to short-lived receiver/instrumentation processes):
 *
 * - **masterOn**: the section's master switch (default off).
 * - **enrolled**: the set of package names whose region follows the primary slot.
 * - **baseline**: per enrolled app, its pre-override per-app locale (BCP-47, "" = system-follow),
 *   captured on enroll so unenroll / master-off can restore *exactly* that value instead of blindly
 *   clearing (which would wipe a language the user set manually in system settings).
 *
 * This is a thin state holder with no privilege; [com.eigenlux.roamer.core.LocaleOverrideController]
 * orchestrates the privileged reads/writes around it.
 */
object AppLocaleStore {
    private const val PREFS = "roamer_prefs"
    private const val KEY_MASTER = "region_master_on"
    private const val KEY_ENROLLED = "region_enrolled"   // StringSet of package names
    private const val KEY_BASELINE = "region_baseline"   // StringSet of "pkg=bcp47" (value may be empty)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isMasterOn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_MASTER, false)

    fun setMasterOn(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_MASTER, on).apply()
    }

    /** Package names currently enrolled (a defensive copy; never the live SharedPreferences set). */
    fun enrolled(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_ENROLLED, emptySet())?.toSet() ?: emptySet()

    fun isEnrolled(ctx: Context, pkg: String): Boolean = enrolled(ctx).contains(pkg)

    fun setEnrolled(ctx: Context, pkg: String, enrolled: Boolean) {
        val next = enrolled(ctx).toHashSet().apply { if (enrolled) add(pkg) else remove(pkg) }
        prefs(ctx).edit().putStringSet(KEY_ENROLLED, next).apply()
    }

    /** The captured pre-override locale for [pkg], or null if none recorded. */
    fun baselineOf(ctx: Context, pkg: String): String? =
        baselineMap(ctx)[pkg]

    fun setBaseline(ctx: Context, pkg: String, bcp47: String) {
        val map = baselineMap(ctx).toMutableMap().apply { put(pkg, bcp47) }
        writeBaseline(ctx, map)
    }

    fun clearBaseline(ctx: Context, pkg: String) {
        val map = baselineMap(ctx).toMutableMap().apply { remove(pkg) }
        writeBaseline(ctx, map)
    }

    private fun baselineMap(ctx: Context): Map<String, String> {
        // Entries are "pkg=bcp47"; package names and BCP-47 tags never contain '=', so splitting on the
        // first '=' is unambiguous. A trailing empty value ("pkg=") means "restore to system-follow".
        val raw = prefs(ctx).getStringSet(KEY_BASELINE, emptySet()).orEmpty()
        return raw.mapNotNull { entry ->
            val i = entry.indexOf('=')
            if (i < 0) null else entry.substring(0, i) to entry.substring(i + 1)
        }.toMap()
    }

    private fun writeBaseline(ctx: Context, map: Map<String, String>) {
        val set = map.entries.map { "${it.key}=${it.value}" }.toHashSet()
        prefs(ctx).edit().putStringSet(KEY_BASELINE, set).apply()
    }
}
