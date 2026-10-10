package com.eigenlux.roamer.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The user's configuration as it is exported and imported.
 */
data class RoamerConfig(
    val regions: List<RegionConfig>,
    val customRegions: List<CustomRegion>,
    val quickSwitch: List<String>,
)

/**
 * JSON encoding of [RoamerConfig] for export and import.
 */
object ConfigCodec {

    private const val FORMAT = "roamer-config"
    private const val VERSION = 1

    /** Upper bounds that keep a malformed or hostile file from bloating the stored config. */
    private const val MAX_REGIONS = 400
    private const val MAX_CARRIERS = 50

    fun default(): RoamerConfig {
        val regions = RegionRules.defaultRegions()
        return RoamerConfig(regions, emptyList(), RegionRules.defaultQuickSwitch(regions))
    }

    fun encode(config: RoamerConfig): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("regions", JSONArray().apply {
            config.regions.forEach { region ->
                put(JSONObject().apply {
                    put("iso", region.iso)
                    put("enabled", region.enabled)
                    put("carriers", JSONArray().apply {
                        region.carriers.forEach { carrier ->
                            put(JSONObject().apply {
                                put("name", carrier.name)
                                put("enabled", carrier.enabled)
                            })
                        }
                    })
                })
            }
        })
        put("customRegions", JSONArray().apply {
            config.customRegions.forEach { custom ->
                put(JSONObject().apply {
                    put("iso", custom.iso)
                    put("name", custom.name)
                })
            }
        })
        put("quickSwitch", JSONArray(config.quickSwitch))
    }.toString(2)

    /**
     * Parses and validates an exported file. Returns null when [text] is not a Roamer config of
     * a known version. Invalid entries inside a valid file are dropped, and the result is
     * normalized the same way stored data is.
     */
    fun decode(text: String): RoamerConfig? = runCatching {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT || root.optInt("version", -1) != VERSION) return null

        val regions = root.getJSONArray("regions").objects().take(MAX_REGIONS).mapNotNull { item ->
            val iso = item.optString("iso").trim().lowercase()
            if (!RegionRules.isValidIso(iso)) return@mapNotNull null
            val carriers = item.optJSONArray("carriers")?.objects().orEmpty().take(MAX_CARRIERS).mapNotNull { c ->
                RegionRules.cleanCarrier(c.optString("name"))?.let { Carrier(it, c.optBoolean("enabled", true)) }
            }
            RegionConfig(iso, carriers, item.optBoolean("enabled", true))
        }
        val customs = root.optJSONArray("customRegions")?.objects().orEmpty().take(MAX_REGIONS).mapNotNull { item ->
            val iso = item.optString("iso").trim().lowercase()
            val name = RegionRules.cleanRegionName(item.optString("name"))
            if (RegionRules.isValidIso(iso) && name != null) CustomRegion(iso, name) else null
        }.distinctBy { it.iso }
        val quick = root.optJSONArray("quickSwitch")?.let { array ->
            (0 until array.length()).map { array.optString(it).trim().lowercase() }
        }.orEmpty()

        val normalized = RegionRules.normalizeRegions(regions)
        RoamerConfig(normalized, customs, RegionRules.cleanQuickSwitch(quick, normalized))
    }.getOrNull()

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
}
