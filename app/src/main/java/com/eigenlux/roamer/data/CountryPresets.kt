package com.eigenlux.roamer.data

import androidx.annotation.StringRes
import com.eigenlux.roamer.R

/**
 * Country preset data class.
 *
 * @property iso Lowercase ISO 3166-1 alpha-2 country code.
 * @property mcc Primary Mobile Country Code.
 * @property nameRes String resource ID for localized display name.
 * @property defaultLocale Default BCP-47 locale tag used for per-app locale syncing.
 */
data class CountryPreset(
    val iso: String,
    val mcc: String,
    @get:StringRes val nameRes: Int,
    val defaultLocale: String,
)

/**
 * Predefined list of country presets for quick selection.
 */
object CountryPresets {
    val all: List<CountryPreset> = listOf(
        CountryPreset("us", "310", R.string.country_us, "en-US"),
        CountryPreset("jp", "440", R.string.country_jp, "ja-JP"),
        CountryPreset("kr", "450", R.string.country_kr, "ko-KR"),
        CountryPreset("hk", "454", R.string.country_hk, "zh-HK"),
        CountryPreset("tw", "466", R.string.country_tw, "zh-TW"),
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
