package com.eigenlux.roamer.core

import com.eigenlux.roamer.data.CountryPresets

/**
 * Pure decision logic for SIM region overrides and per-app locale syncing.
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
     * Subscriptions to restore and to override when switching every SIM to one country.
     */
    data class MaskPlan(
        val restore: List<Int>,
        val override: List<Int>,
    )

    /**
     * Plans a switch of all [sims] to [iso]. A SIM whose real country is [iso] cannot be
     * overridden to it, so it is restored when currently overridden and left alone otherwise.
     */
    fun maskPlan(sims: List<SimInfo>, iso: String): MaskPlan {
        val (home, foreign) = sims.partition { it.realCountryIso.equals(iso, ignoreCase = true) }
        return MaskPlan(
            restore = home.filter { it.overridden }.map { it.subId },
            override = foreign.map { it.subId },
        )
    }

    /**
     * Returns the default BCP-47 locale tag associated with a country ISO.
     */
    fun deriveLocale(country: String): String? = CountryPresets.byIso(country)?.defaultLocale

    /**
     * Determines the desired locale tag for an enrolled package based on override state.
     */
    fun desiredTag(
        masterOn: Boolean,
        primary: PrimaryState,
        baseline: String,
        localeOf: (String) -> String? = ::deriveLocale,
    ): String {
        if (masterOn && primary.overridden && primary.country != null) {
            localeOf(primary.country)?.let { return it }
        }
        return baseline
    }
}
