package com.eigenlux.roamer.core

import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Resolves ISO country codes from Mobile Country Codes (MCC).
 * Uses AOSP `MccTable` reflection with a static fallback map.
 */
object MccUtil {
    private const val TAG = "RoamerMcc"

    init {
        runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
    }

    /**
     * Resolves ISO country code from MCCMNC string.
     * Tries reflection first, falling back to static map if reflection fails.
     */
    fun countryFromMcc(mccMnc: String): String {
        val mccStr = mccMnc.take(3)
        if (mccStr.toIntOrNull() == null) {
            Log.w(TAG, "countryFromMcc: mccMnc='$mccMnc' has no valid MCC")
            return ""
        }
        reflectCountryCode(mccStr)?.let { return it }
        return isoFromMccTable(mccStr).also {
            if (it.isNotBlank()) Log.i(TAG, "countryFromMcc: mcc=$mccStr via fallback -> '$it'")
            else Log.w(TAG, "countryFromMcc: mcc=$mccStr missed both reflection and fallback")
        }
    }

    /**
     * Reflects `com.android.internal.telephony.MccTable.countryCodeForMcc`.
     */
    private fun reflectCountryCode(mccStr: String): String? {
        val mccInt = mccStr.toInt()
        val cls = runCatching { Class.forName("com.android.internal.telephony.MccTable") }
            .getOrElse { Log.w(TAG, "countryFromMcc: MccTable not found", it); return null }
        val candidates = cls.methods.filter { it.name == "countryCodeForMcc" }
        for (m in candidates) {
            val arg: Any = when (m.parameterTypes.getOrNull(0)) {
                Int::class.javaPrimitiveType, Integer::class.java -> mccInt
                String::class.java -> mccStr
                else -> continue
            }
            val result = runCatching { m.invoke(null, arg) as? String }
                .onFailure { Log.w(TAG, "countryFromMcc: $m invocation failed", it) }
                .getOrNull()
            if (!result.isNullOrBlank()) {
                Log.i(TAG, "countryFromMcc: mcc=$mccStr via $m -> '$result'")
                return result.lowercase()
            }
        }
        return null
    }

    /**
     * Static MCC-to-ISO fallback lookup table for unit testing and offline fallback.
     */
    fun isoFromMccTable(mccMnc: String): String = FALLBACK[mccMnc.take(3)].orEmpty()

    private val FALLBACK: Map<String, String> = buildMap {
        listOf("310", "311", "312", "313", "314", "315", "316").forEach { put(it, "us") }
        put("302", "ca")
        listOf("440", "441").forEach { put(it, "jp") }
        put("450", "kr")
        put("454", "hk")
        put("466", "tw")
        listOf("460", "461").forEach { put(it, "cn") }
        put("455", "mo")
        put("525", "sg")
        put("502", "my")
        put("520", "th")
        put("452", "vn")
        put("510", "id")
        put("515", "ph")
        listOf("404", "405", "406").forEach { put(it, "in") }
        put("724", "br")
        listOf("234", "235").forEach { put(it, "gb") }
        put("262", "de")
        put("208", "fr")
        put("222", "it")
        put("214", "es")
        put("204", "nl")
        put("228", "ch")
        put("250", "ru")
        listOf("424", "430", "431").forEach { put(it, "ae") }
        put("505", "au")
        put("530", "nz")
    }
}
