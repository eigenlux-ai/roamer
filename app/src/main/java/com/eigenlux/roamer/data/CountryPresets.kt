package com.eigenlux.roamer.data

import androidx.annotation.StringRes
import com.eigenlux.roamer.R

/**
 * Country preset table (pure static data, touches no hidden APIs, unit-testable/previewable).
 *
 * @param iso     Two-letter country code (ISO 3166-1 alpha-2, lowercase); the override target value
 * @param mcc     Primary MCC (for display / carrier linkage only, not a legally precise value — some countries have multiple MCCs)
 * @param nameRes String resource of the localized display name (English in res/values, Chinese in res/values-zh).
 *                The display label is composed on the UI side as `<localized name> (iso)`, so the name follows the app locale.
 */
data class CountryPreset(
    val iso: String,
    val mcc: String,
    @get:StringRes val nameRes: Int,
)

/**
 * Covers mainstream test regions. Common debug regions come first (us/jp/kr/hk/tw), the rest are
 * roughly ordered by geographic region.
 * Data source: ITU MCC allocation table (public information). Used only for the override-input
 * dropdown selection, not for any validation logic.
 */
object CountryPresets {
    val all: List<CountryPreset> = listOf(
        // —— Common debug regions pinned to top ——
        CountryPreset("us", "310", R.string.country_us),
        CountryPreset("jp", "440", R.string.country_jp),
        CountryPreset("kr", "450", R.string.country_kr),
        CountryPreset("hk", "454", R.string.country_hk),
        CountryPreset("tw", "466", R.string.country_tw),
        // —— The rest ——
        CountryPreset("cn", "460", R.string.country_cn),
        CountryPreset("mo", "455", R.string.country_mo),
        CountryPreset("sg", "525", R.string.country_sg),
        CountryPreset("my", "502", R.string.country_my),
        CountryPreset("th", "520", R.string.country_th),
        CountryPreset("vn", "452", R.string.country_vn),
        CountryPreset("id", "510", R.string.country_id),
        CountryPreset("ph", "515", R.string.country_ph),
        CountryPreset("in", "404", R.string.country_in),
        CountryPreset("ca", "302", R.string.country_ca),
        CountryPreset("br", "724", R.string.country_br),
        CountryPreset("gb", "234", R.string.country_gb),
        CountryPreset("de", "262", R.string.country_de),
        CountryPreset("fr", "208", R.string.country_fr),
        CountryPreset("it", "222", R.string.country_it),
        CountryPreset("es", "214", R.string.country_es),
        CountryPreset("nl", "204", R.string.country_nl),
        CountryPreset("ch", "228", R.string.country_ch),
        CountryPreset("ru", "250", R.string.country_ru),
        CountryPreset("ae", "424", R.string.country_ae),
        CountryPreset("au", "505", R.string.country_au),
        CountryPreset("nz", "530", R.string.country_nz),
    )

    fun byIso(iso: String): CountryPreset? = all.firstOrNull { it.iso == iso.lowercase() }
}
