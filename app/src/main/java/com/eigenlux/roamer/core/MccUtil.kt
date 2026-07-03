package com.eigenlux.roamer.core

import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * The **single source of truth** for deriving the ISO country code from the MCC.
 *
 * `getSimOperator` (MCC/MNC) is SELinux-protected and cannot be overridden by CarrierConfig, so the
 * country code derived from a static MCC lookup is the "tamper-proof true home country" — still
 * re-retrievable at runtime while an override is in effect. This is the cornerstone of "restore needs
 * no saved baseline": the real ISO can be re-derived at runtime from the MCC at any moment.
 *
 * The App process (enumerating SIMs / determining whether overridden) and the privileged
 * instrumentation (restore write-back) share this object, avoiding two divergent copies of the MCC
 * lookup logic.
 */
object MccUtil {
    private const val TAG = "RoamerMcc"

    init {
        // Reflecting com.android.internal.telephony.MccTable (@hide) requires a hidden-API exemption (process-level, once suffices)
        runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
    }

    /**
     * Derive a lowercase ISO from mccMnc (taking the first 3 digits as the MCC), e.g. `"46001"→"cn"`.
     * Returns `""` when there is no valid MCC.
     * Prefer AOSP `MccTable` reflection (authoritative, full table); when reflection fails, fall back to
     * the built-in [isoFromMccTable] table, avoiding degradation to `""` on some ROMs → which would
     * trigger the false-restore chain of the restore criterion "cannot get the true value counts as success".
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
     * Reflect `MccTable.countryCodeForMcc`. The signature varies between int / String across Android
     * versions, so enumerate same-named methods and match by parameter type to avoid guessing the wrong
     * overload. On a hit, return the lowercase ISO; class missing / no match / call failure all return null.
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
     * Pure static MCC→ISO fallback lookup (**no reflection, no Log, JVM-unit-testable**). Takes the
     * first 3 digits as the MCC; returns `""` when not listed. Covers all countries in
     * [com.eigenlux.roamer.data.CountryPresets] + common MCC variants. Data taken from the ITU MCC
     * allocation (public). Not an authoritative full table — the authoritative path is still `MccTable`
     * reflection; this only serves as a safety net when reflection fails, and is not used as a validation criterion.
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
