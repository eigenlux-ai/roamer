package com.eigenlux.roamer.core

import com.eigenlux.roamer.data.CountryPresets

/**
 * Pure decision logic for the phase-2 per-app region override, deliberately free of any Android /
 * Shizuku imports so it is JVM-unit-testable (see `RegionLogicTest`). All privileged I/O lives in
 * [LocaleOverrideController], which delegates every decision here.
 */
object RegionLogic {

    /** The primary slot's region, as it drives per-app locale. */
    data class PrimaryState(
        /** The primary slot's effective (possibly overridden) country ISO, or null if unavailable. */
        val country: String?,
        /** Whether the primary slot is currently overridden (masked away from its real country). */
        val overridden: Boolean,
    )

    /**
     * Resolve the primary slot from the current SIM list. Single SIM → that SIM; dual SIM → the one
     * flagged [SimInfo.isDefaultSub] (getDefaultSubscriptionId, i.e. what apps read via no-arg
     * TelephonyManager). If dual SIM with no default (e.g. voice = "ask every time" → INVALID),
     * country is null → callers apply nothing. See memory `default-sub-is-voice-not-data`.
     */
    fun primaryOf(sims: List<SimInfo>): PrimaryState {
        val primary = if (sims.size == 1) sims.firstOrNull() else sims.firstOrNull { it.isDefaultSub }
        return PrimaryState(
            country = primary?.countryIso?.takeIf { it.isNotBlank() },
            overridden = primary?.overridden == true,
        )
    }

    /** The country's canonical per-app locale (BCP-47), or null if the ISO is not a known preset. */
    fun deriveLocale(country: String): String? = CountryPresets.byIso(country)?.defaultLocale

    /**
     * The locale tag an enrolled app should currently carry (D4): mirror the primary slot's *override*
     * state — apply the primary country's locale only while the primary slot is actually overridden;
     * otherwise (master off, primary not overridden, or ISO not derivable) fall back to the app's
     * captured [baseline] (usually "" = system-follow). This keeps the device clean when nothing is masked.
     */
    fun desiredTag(masterOn: Boolean, primary: PrimaryState, baseline: String): String {
        if (masterOn && primary.overridden && primary.country != null) {
            deriveLocale(primary.country)?.let { return it }
        }
        return baseline
    }
}
