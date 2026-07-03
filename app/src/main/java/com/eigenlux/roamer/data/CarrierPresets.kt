package com.eigenlux.roamer.data

/**
 * Carrier preset table (pure static data), indexed by country ISO, powering the read-only
 * country -> carrier dropdown linkage.
 * Each country lists 2-4 leading MNO display names, **in descending order of subscriber market share**
 * (cross-checked online against 2024-2025 data, including recent mergers).
 * The first item = the market leader, used as the default value when the UI selects a country.
 * Unlisted countries fall back to an empty list (the UI item is disabled).
 */
object CarrierPresets {

    private val byCountry: Map<String, List<String>> = mapOf(
        "cn" to listOf("中国移动", "中国电信", "中国联通", "中国广电"), // China Mobile ~61%, China Telecom ~26%, China Unicom ~21%, China Broadnet ~2% (2024)
        "hk" to listOf("CMHK", "csl", "3", "SmarTone"),               // CMHK~8M > csl~5M > 3~4.6M > SmarTone~2.9M
        "mo" to listOf("CTM", "China Telecom", "3"),                  // CTM ~53% > China Telecom ~35% (SmarTone exited Macau in 2024)
        "tw" to listOf("中華電信", "台灣大哥大", "遠傳電信"),          // Chunghwa 36.9%, Taiwan Mobile 33.3%, FarEasTone 29.8% (post-merger)
        "jp" to listOf("NTT docomo", "au", "SoftBank", "Rakuten Mobile"), // docomo 40.6%, au 30.4%, SoftBank 24.6%, Rakuten 4.4%
        "kr" to listOf("SK Telecom", "KT", "LG U+"),                  // SKT~37% KT~30% LG U+~21%
        "sg" to listOf("Singtel", "StarHub", "M1", "Simba"),         // Singtel~45% StarHub~23% M1~20% Simba~10%
        "my" to listOf("CelcomDigi", "Maxis", "U Mobile", "YES"),    // CelcomDigi~47% Maxis~28% U Mobile~18%
        "th" to listOf("True", "AIS", "NT"),                         // True ~52%, AIS ~46% (True/dtac merger), NT ~2%
        "vn" to listOf("Viettel", "Vinaphone", "MobiFone"),          // Viettel~56% Vinaphone~30% MobiFone~15%
        "id" to listOf("Telkomsel", "Indosat", "XL"),               // Telkomsel~45% Indosat~28% XL(XLSmart)~27%
        "ph" to listOf("Smart", "Globe", "DITO"),                    // Smart~45-50% Globe~40-50% DITO~10%
        "in" to listOf("Jio", "Airtel", "Vi", "BSNL"),              // Jio 39.3% Airtel 37.2% Vi 16.0% BSNL 7.5%
        "us" to listOf("T-Mobile", "Verizon", "AT&T"),               // T-Mobile ~35%, Verizon ~34%, AT&T ~27% (T-Mobile #1 by subscriber count)
        "ca" to listOf("Rogers", "Bell", "Telus"),                   // Rogers~31% Bell~28% Telus~27%
        "br" to listOf("Vivo", "Claro", "TIM"),                      // Vivo 38.1% Claro 33.1% TIM 22.9%
        "gb" to listOf("EE", "O2", "Vodafone", "Three"),            // EE ~30%, O2 ~26%, Vodafone ~20%, Three ~13% (Vodafone+Three merger underway)
        "de" to listOf("Telekom", "O2", "Vodafone", "1&1"),         // By connections: Telekom > O2 ~45M > Vodafone ~31M > 1&1 ~12M
        "fr" to listOf("Orange", "SFR", "Bouygues Telecom", "Free"), // Orange~30% SFR~21% Bouygues~17% Free~15.5M
        "it" to listOf("WindTre", "TIM", "Vodafone", "Iliad"),      // WindTre 23.9% TIM 23.1% Vodafone 21.2% Iliad 14.8%
        "es" to listOf("Orange", "Movistar", "Vodafone", "Yoigo"),  // Orange(MasOrange) 41% Movistar 26% Vodafone 18.5%
        "nl" to listOf("Odido", "KPN", "Vodafone"),                  // Odido~30-35% KPN~30-35% Vodafone(Ziggo)~20-25%
        "ch" to listOf("Swisscom", "Sunrise", "Salt"),               // Swisscom 54% Sunrise 26.5% Salt 18%
        "ru" to listOf("MTS", "MegaFon", "Beeline", "t2"),          // MTS~36% MegaFon~29% Beeline~24% t2(Tele2)~14%
        "ae" to listOf("Etisalat", "du"),                            // Etisalat(e&)~61% du~39%
        "au" to listOf("Telstra", "Optus", "Vodafone"),              // Telstra~44% Optus~31% Vodafone(TPG)~17%
        "nz" to listOf("Spark", "One NZ", "2degrees"),               // Spark~2.4M One NZ~2.2M 2degrees~1.6M
    )

    fun forCountry(iso: String): List<String> = byCountry[iso.lowercase()].orEmpty()
}
