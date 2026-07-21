package com.eigenlux.roamer.data

import android.content.Context

/**
 * Manages persistent configuration for per-app locale overrides.
 */
object AppLocaleStore {
    private const val PREFS = "roamer_prefs"
    private const val KEY_MASTER = "region_master_on"
    private const val KEY_ENROLLED = "region_enrolled"
    private const val KEY_BASELINE = "region_baseline"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isMasterOn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_MASTER, false)

    fun setMasterOn(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_MASTER, on).apply()
    }

    /**
     * Returns a copy of the enrolled package name set.
     */
    fun enrolled(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_ENROLLED, emptySet())?.toSet() ?: emptySet()

    fun isEnrolled(ctx: Context, pkg: String): Boolean = enrolled(ctx).contains(pkg)

    fun setEnrolled(ctx: Context, pkg: String, enrolled: Boolean) {
        val next = enrolled(ctx).toHashSet().apply { if (enrolled) add(pkg) else remove(pkg) }
        prefs(ctx).edit().putStringSet(KEY_ENROLLED, next).apply()
    }

    /**
     * Retrieves the stored baseline locale for [pkg], or null if unrecorded.
     */
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
