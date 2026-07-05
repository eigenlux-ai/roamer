package com.eigenlux.roamer.core

/**
 * The state of a single SIM (for display). The card information is split into three sections; for the
 * field mapping see MainActivity: current values (carrierName/countryIso) · read-only values
 * (mcc/mnc/iccId, untouchable by any override) · action area (override/restore buttons, UI layer).
 *
 * No longer saves a pre-override snapshot: on restore, ISO is re-derived at runtime from the immutable
 * MCC and the carrier name reverts automatically when the override layer is cleared, so no saved
 * values are needed.
 *
 * @param slot        physical SIM slot (1/2), display-only
 * @param subId       subscription ID, the override target
 * @param carrierName currently displayed carrier name (may already be overridden)
 * @param countryIso  currently effective country code ISO (may already be overridden)
 * @param mcc            real MCC (from getSimOperator, not overridable, read-only)
 * @param mnc            real MNC (read-only)
 * @param iccId          SIM hardware serial number (read-only; a non-privileged read may be redacted to empty by the system)
 * @param realCountryIso real country code (derived at runtime from the immutable MCC, immune to override; used for the "original value" display while an override is in effect)
 * @param realCarrierName real carrier name (derived from the Carrier-ID database via getSimCarrierIdName, immune to the carrier_name override;
 *                        still the real name while an override is in effect, used for the "original value" display. When unavailable, in the non-overridden state falls back to the current name)
 * @param overridden     whether the country code has been overridden (derived at runtime: realCountryIso ≠ currently effective countryIso)
 * @param isDefaultSub   whether this SIM is the system default subscription (SubscriptionManager.getDefaultSubscriptionId) —
 *                       the sub that an app's no-arg TelephonyManager reads (getSimCountryIso/getSimOperatorName) bind to
 *                       (on a voice-capable phone this resolves to the default voice SIM, not necessarily the data SIM).
 *                       Only set when there are >= 2 active SIMs, where it actually disambiguates which card apps read.
 */
data class SimInfo(
    val slot: Int,
    val subId: Int,
    val carrierName: String,
    val countryIso: String,
    val mcc: String,
    val mnc: String,
    val iccId: String,
    val realCountryIso: String,
    val realCarrierName: String,
    val overridden: Boolean,
    val isDefaultSub: Boolean = false,
)
