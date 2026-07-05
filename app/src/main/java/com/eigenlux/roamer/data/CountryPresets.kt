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
 * @param defaultLocale Canonical BCP-47 locale (language + region) for this country, written to a selected app's
 *                per-app locale when its region follows this country (phase-2 region override; e.g. "en-US", "ja-JP").
 *                Multi-language regions use the dominant UI language (e.g. hk → zh-HK). The region subtag always equals [iso].
 */
data class CountryPreset(
    val iso: String,
    val mcc: String,
    @get:StringRes val nameRes: Int,
    val defaultLocale: String,
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
        CountryPreset("us", "310", R.string.country_us, "en-US"),
        CountryPreset("jp", "440", R.string.country_jp, "ja-JP"),
        CountryPreset("kr", "450", R.string.country_kr, "ko-KR"),
        CountryPreset("hk", "454", R.string.country_hk, "zh-HK"),
        CountryPreset("tw", "466", R.string.country_tw, "zh-TW"),
        // —— The rest ——
        CountryPreset("cn", "460", R.string.country_cn, "zh-CN"),
        CountryPreset("mo", "455", R.string.country_mo, "zh-MO"),
        CountryPreset("sg", "525", R.string.country_sg, "en-SG"),
        CountryPreset("my", "502", R.string.country_my, "ms-MY"),
        CountryPreset("th", "520", R.string.country_th, "th-TH"),
        CountryPreset("vn", "452", R.string.country_vn, "vi-VN"),
        CountryPreset("id", "510", R.string.country_id, "id-ID"),
        CountryPreset("ph", "515", R.string.country_ph, "en-PH"),
        CountryPreset("in", "404", R.string.country_in, "en-IN"),
        CountryPreset("ca", "302", R.string.country_ca, "en-CA"),
        CountryPreset("br", "724", R.string.country_br, "pt-BR"),
        CountryPreset("gb", "234", R.string.country_gb, "en-GB"),
        CountryPreset("de", "262", R.string.country_de, "de-DE"),
        CountryPreset("fr", "208", R.string.country_fr, "fr-FR"),
        CountryPreset("it", "222", R.string.country_it, "it-IT"),
        CountryPreset("es", "214", R.string.country_es, "es-ES"),
        CountryPreset("nl", "204", R.string.country_nl, "nl-NL"),
        CountryPreset("ch", "228", R.string.country_ch, "de-CH"),
        CountryPreset("ru", "250", R.string.country_ru, "ru-RU"),
        CountryPreset("ae", "424", R.string.country_ae, "ar-AE"),
        CountryPreset("au", "505", R.string.country_au, "en-AU"),
        CountryPreset("nz", "530", R.string.country_nz, "en-NZ"),
    )

    fun byIso(iso: String): CountryPreset? = all.firstOrNull { it.iso == iso.lowercase(java.util.Locale.ROOT) }
}
