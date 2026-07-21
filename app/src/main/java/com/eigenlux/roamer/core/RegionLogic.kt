package com.eigenlux.roamer.core

import com.eigenlux.roamer.data.CountryPresets

/**
 * Pure decision logic for per-app region overrides.
 * Free of Android dependencies to support JVM unit testing.
 */
object RegionLogic {

    /**
     * Primary SIM slot region state.
     */
    data class PrimaryState(
        val country: String?,
        val overridden: Boolean,
    )

    /**
     * Identifies the primary SIM slot from a list of active SIMs.
     */
    fun primaryOf(sims: List<SimInfo>): PrimaryState {
        val primary = if (sims.size == 1) sims.firstOrNull() else sims.firstOrNull { it.isDefaultSub }
        return PrimaryState(
            country = primary?.countryIso?.takeIf { it.isNotBlank() },
            overridden = primary?.overridden == true,
        )
    }

    /**
     * Returns the default BCP-47 locale tag associated with a country ISO.
     */
    fun deriveLocale(country: String): String? = CountryPresets.byIso(country)?.defaultLocale

    /**
     * Determines the desired locale tag for an enrolled package based on override state.
     */
    fun desiredTag(masterOn: Boolean, primary: PrimaryState, baseline: String): String {
        if (masterOn && primary.overridden && primary.country != null) {
            deriveLocale(primary.country)?.let { return it }
        }
        return baseline
    }
}
