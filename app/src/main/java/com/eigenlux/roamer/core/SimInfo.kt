package com.eigenlux.roamer.core

/**
 * Display model for a single SIM slot's telephony state.
 *
 * @property slot Physical SIM slot index (1-based).
 * @property subId Target subscription ID.
 * @property carrierName Currently active carrier display name.
 * @property countryIso Currently active ISO country code.
 * @property mcc Immutable Mobile Country Code.
 * @property mnc Immutable Mobile Network Code.
 * @property iccId SIM hardware identification number.
 * @property realCountryIso Unmodified country ISO derived from MCC.
 * @property realCarrierName Real carrier name resolved from the system carrier database.
 * @property overridden True if the current country ISO differs from the hardware real ISO.
 * @property isDefaultSub True if this SIM is designated as the system default voice subscription.
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
