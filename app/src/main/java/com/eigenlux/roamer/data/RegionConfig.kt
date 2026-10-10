package com.eigenlux.roamer.data

import android.content.Context

/**
 * A carrier name of a region.
 *
 * @property enabled False for a preset carrier the user turned off. Preset carriers cannot be
 * removed, only turned off; carriers the user added are always enabled and can be removed.
 */
data class Carrier(
    val name: String,
    val enabled: Boolean = true,
)

/**
 * A country or region the user can override a SIM to.
 *
 * @property iso Lowercase ISO 3166-1 alpha-2 code.
 * @property carriers Its carriers in display order: every preset of the region, plus the ones
 * the user added.
 * @property enabled True while the region is in the user's list. A region removed from the
 * list is kept with false, so its carriers are still there if it is added again.
 */
data class RegionConfig(
    val iso: String,
    val carriers: List<Carrier>,
    val enabled: Boolean = true,
) {
    /** Names offered on the main screen; the first one is the default. */
    val enabledCarriers: List<String> get() = carriers.filter { it.enabled }.map { it.name }
}

/**
 * A country or region defined by the user, for a code the app does not know.
 *
 * @property iso Lowercase two-letter code written to the SIM.
 * @property name Display name chosen by the user.
 */
data class CustomRegion(
    val iso: String,
    val name: String,
)

/**
 * Pure rules and text encoding for the user's region list. Free of Android dependencies.
 */
object RegionRules {

    const val MAX_QUICK_SWITCH = 5
    const val MAX_CARRIER_LENGTH = 32
    const val MAX_REGION_NAME_LENGTH = 40

    private val defaultQuickSwitch = listOf("us", "jp", "kr", "cn", "hk")

    /** The built-in presets, used until the user edits the list. */
    fun defaultRegions(): List<RegionConfig> = CountryPresets.all.map { defaultRegion(it.iso) }

    /** A region as it is first added: all of its preset carriers, enabled. */
    fun defaultRegion(iso: String): RegionConfig =
        RegionConfig(iso, CarrierPresets.forCountry(iso).map { Carrier(it) })

    fun defaultQuickSwitch(regions: List<RegionConfig>): List<String> =
        cleanQuickSwitch(defaultQuickSwitch, regions)

    fun isValidIso(iso: String): Boolean = iso.length == 2 && iso.all { it in 'a'..'z' }

    /**
     * Normalizes a carrier name typed by the user: control characters removed, trimmed, and
     * cut to [MAX_CARRIER_LENGTH]. Returns null when nothing is left.
     */
    fun cleanCarrier(raw: String): String? =
        raw.filterNot { it.isISOControl() }.trim().take(MAX_CARRIER_LENGTH).trim().ifEmpty { null }

    fun isPresetCarrier(iso: String, name: String): Boolean = name in CarrierPresets.forCountry(iso)

    /**
     * Brings a carrier list into its valid form: no repeated names, added carriers always
     * enabled, and every preset of [iso] present. A preset missing from [carriers], such as one
     * that a newer version of the app introduced, is appended.
     */
    fun normalizeCarriers(iso: String, carriers: List<Carrier>): List<Carrier> {
        val presets = CarrierPresets.forCountry(iso)
        val kept = carriers.distinctBy { it.name }.map { if (it.name in presets) it else it.copy(enabled = true) }
        val present = kept.mapTo(HashSet()) { it.name }
        return kept + presets.filterNot { it in present }.map { Carrier(it) }
    }

    /** Normalizes a region name typed by the user. Returns null when nothing is left. */
    fun cleanRegionName(raw: String): String? =
        raw.filterNot { it.isISOControl() }.trim().take(MAX_REGION_NAME_LENGTH).trim().ifEmpty { null }

    /** Brings a region list into its valid form: no repeated regions, valid carrier lists. */
    fun normalizeRegions(regions: List<RegionConfig>): List<RegionConfig> =
        regions.distinctBy { it.iso }.map { it.copy(carriers = normalizeCarriers(it.iso, it.carriers)) }

    /**
     * Moves the region at [from] to [to], both counted among the regions in the list
     * (the enabled ones). Regions kept outside the list stay where they are, after it.
     */
    fun moveInList(regions: List<RegionConfig>, from: Int, to: Int): List<RegionConfig> {
        val (listed, kept) = regions.partition { it.enabled }
        if (from !in listed.indices || to !in listed.indices) return regions
        return listed.toMutableList().apply { add(to, removeAt(from)) } + kept
    }

    /** Puts [iso] at the end of the list, bringing back its kept carriers if it was there before. */
    fun addToList(regions: List<RegionConfig>, iso: String): List<RegionConfig> {
        val existing = regions.firstOrNull { it.iso == iso }
        if (existing?.enabled == true) return regions
        val (listed, kept) = regions.filterNot { it.iso == iso }.partition { it.enabled }
        return listed + (existing ?: defaultRegion(iso)).copy(enabled = true) + kept
    }

    /** Takes [iso] out of the list but keeps its configuration. */
    fun removeFromList(regions: List<RegionConfig>, iso: String): List<RegionConfig> {
        val (listed, kept) = regions.map { if (it.iso == iso) it.copy(enabled = false) else it }.partition { it.enabled }
        return listed + kept
    }

    fun encodeCustom(customs: List<CustomRegion>): String = customs.joinToString("\n") { "${it.iso}\t${it.name}" }

    fun decodeCustom(text: String): List<CustomRegion> {
        val seen = HashSet<String>()
        return text.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            val iso = parts.first().trim().lowercase()
            val name = parts.getOrNull(1)?.let(::cleanRegionName)
            if (!isValidIso(iso) || name == null || !seen.add(iso)) null else CustomRegion(iso, name)
        }.toList()
    }

    /** Keeps only quick-switch entries that are in the list of [regions], without duplicates. */
    fun cleanQuickSwitch(quick: List<String>, regions: List<RegionConfig>): List<String> {
        val known = regions.filter { it.enabled }.mapTo(HashSet()) { it.iso }
        return quick.filter { it in known }.distinct().take(MAX_QUICK_SWITCH)
    }

    /**
     * One line per region: the ISO code, then its carriers, separated by tabs. A region or
     * carrier that is turned off is written with a leading space, which neither an ISO code nor
     * a cleaned name can start with.
     */
    fun encode(regions: List<RegionConfig>): String = regions.joinToString("\n") { region ->
        val off = { enabled: Boolean, text: String -> if (enabled) text else " $text" }
        (listOf(off(region.enabled, region.iso)) + region.carriers.map { off(it.enabled, it.name) }).joinToString("\t")
    }

    /**
     * Reverse of [encode]. Invalid or repeated regions and carriers are dropped, and the result
     * is normalized.
     */
    fun decode(text: String): List<RegionConfig> {
        val seen = HashSet<String>()
        return normalizeRegions(decodeLines(text, seen))
    }

    private fun decodeLines(text: String, seen: HashSet<String>): List<RegionConfig> =
        text.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            val iso = parts.first().trim().lowercase()
            if (!isValidIso(iso) || !seen.add(iso)) return@mapNotNull null
            val carriers = parts.drop(1).mapNotNull { field ->
                cleanCarrier(field)?.let { Carrier(it, enabled = !field.startsWith(' ')) }
            }
            RegionConfig(iso, carriers, enabled = !parts.first().startsWith(' '))
        }.toList()
}

/**
 * Persists the user's region list and quick-switch selection.
 */
object RegionStore {
    private const val PREFS = "roamer_prefs"
    private const val KEY_REGIONS = "regions_v1"
    private const val KEY_QUICK = "quick_switch_v1"
    private const val KEY_CUSTOM = "custom_regions_v1"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadRegions(ctx: Context): List<RegionConfig> =
        prefs(ctx).getString(KEY_REGIONS, null)?.let(RegionRules::decode) ?: RegionRules.defaultRegions()

    fun saveRegions(ctx: Context, regions: List<RegionConfig>) {
        prefs(ctx).edit().putString(KEY_REGIONS, RegionRules.encode(regions)).apply()
    }

    fun loadCustomRegions(ctx: Context): List<CustomRegion> =
        prefs(ctx).getString(KEY_CUSTOM, null)?.let(RegionRules::decodeCustom).orEmpty()

    fun saveCustomRegions(ctx: Context, customs: List<CustomRegion>) {
        prefs(ctx).edit().putString(KEY_CUSTOM, RegionRules.encodeCustom(customs)).apply()
    }

    /** Drops everything the user configured, so the next load returns the defaults. */
    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_REGIONS).remove(KEY_QUICK).remove(KEY_CUSTOM).apply()
    }

    fun loadQuickSwitch(ctx: Context, regions: List<RegionConfig>): List<String> {
        val stored = prefs(ctx).getString(KEY_QUICK, null) ?: return RegionRules.defaultQuickSwitch(regions)
        return RegionRules.cleanQuickSwitch(stored.split(',').map { it.trim() }, regions)
    }

    fun saveQuickSwitch(ctx: Context, quick: List<String>) {
        prefs(ctx).edit().putString(KEY_QUICK, quick.joinToString(",")).apply()
    }
}
