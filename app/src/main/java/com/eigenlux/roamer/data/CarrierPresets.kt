package com.eigenlux.roamer.data

/**
 * Static carrier presets indexed by ISO country code.
 * The first item in each list is the default selection for that region.
 */
object CarrierPresets {

    private val byCountry: Map<String, List<String>> = mapOf(
        "cn" to listOf("中国移动", "中国电信", "中国联通", "中国广电"),
        "hk" to listOf("CMHK", "csl", "3", "SmarTone"),
        "mo" to listOf("CTM", "China Telecom", "3"),
        "tw" to listOf("中華電信", "台灣大哥大", "遠傳電信"),
        "jp" to listOf("NTT docomo", "au", "SoftBank", "Rakuten Mobile"),
        "kr" to listOf("SK Telecom", "KT", "LG U+"),
        "sg" to listOf("Singtel", "StarHub", "M1", "Simba"),
        "my" to listOf("CelcomDigi", "Maxis", "U Mobile", "YES"),
        "th" to listOf("True", "AIS", "NT"),
        "vn" to listOf("Viettel", "Vinaphone", "MobiFone"),
        "id" to listOf("Telkomsel", "Indosat", "XL"),
        "ph" to listOf("Smart", "Globe", "DITO"),
        "in" to listOf("Jio", "Airtel", "Vi", "BSNL"),
        "us" to listOf("T-Mobile", "Verizon", "AT&T"),
        "ca" to listOf("Rogers", "Bell", "Telus"),
        "br" to listOf("Vivo", "Claro", "TIM"),
        "gb" to listOf("EE", "O2", "Vodafone", "Three"),
        "de" to listOf("Telekom", "O2", "Vodafone", "1&1"),
        "fr" to listOf("Orange", "SFR", "Bouygues Telecom", "Free"),
        "it" to listOf("WindTre", "TIM", "Vodafone", "Iliad"),
        "es" to listOf("Orange", "Movistar", "Vodafone", "Yoigo"),
        "nl" to listOf("Odido", "KPN", "Vodafone"),
        "ch" to listOf("Swisscom", "Sunrise", "Salt"),
        "ru" to listOf("MTS", "MegaFon", "Beeline", "t2"),
        "ae" to listOf("Etisalat", "du"),
        "au" to listOf("Telstra", "Optus", "Vodafone"),
        "nz" to listOf("Spark", "One NZ", "2degrees"),
    )

    fun forCountry(iso: String): List<String> = byCountry[iso.lowercase()].orEmpty()
}
