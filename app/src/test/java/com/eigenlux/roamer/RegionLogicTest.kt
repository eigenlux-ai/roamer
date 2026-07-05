package com.eigenlux.roamer

import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.core.RegionLogic.PrimaryState
import com.eigenlux.roamer.core.SimInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-logic tests for the phase-2 region-override decisions (no Android/Shizuku involved). */
class RegionLogicTest {

    private fun sim(country: String, overridden: Boolean, isDefault: Boolean, slot: Int = 1, subId: Int = slot) =
        SimInfo(
            slot = slot, subId = subId, carrierName = "C", countryIso = country,
            mcc = "000", mnc = "00", iccId = "", realCountryIso = "cn", realCarrierName = "C",
            overridden = overridden, isDefaultSub = isDefault,
        )

    @Test
    fun `deriveLocale maps known iso and is case-insensitive`() {
        assertEquals("en-US", RegionLogic.deriveLocale("us"))
        assertEquals("en-US", RegionLogic.deriveLocale("US"))
        assertEquals("ja-JP", RegionLogic.deriveLocale("jp"))
        assertNull(RegionLogic.deriveLocale("zz"))
    }

    @Test
    fun `desiredTag applies country locale only while primary overridden and master on`() {
        val overriddenJp = PrimaryState(country = "jp", overridden = true)
        assertEquals("ja-JP", RegionLogic.desiredTag(masterOn = true, primary = overriddenJp, baseline = ""))
        // master off -> baseline
        assertEquals("fr-FR", RegionLogic.desiredTag(masterOn = false, primary = overriddenJp, baseline = "fr-FR"))
        // primary not overridden -> baseline
        val realJp = PrimaryState(country = "jp", overridden = false)
        assertEquals("", RegionLogic.desiredTag(masterOn = true, primary = realJp, baseline = ""))
        // country null -> baseline
        val none = PrimaryState(country = null, overridden = true)
        assertEquals("en-GB", RegionLogic.desiredTag(masterOn = true, primary = none, baseline = "en-GB"))
        // unknown iso not derivable -> baseline
        val unknown = PrimaryState(country = "zz", overridden = true)
        assertEquals("de-DE", RegionLogic.desiredTag(masterOn = true, primary = unknown, baseline = "de-DE"))
    }

    @Test
    fun `primaryOf single sim uses that sim`() {
        val p = RegionLogic.primaryOf(listOf(sim("us", overridden = true, isDefault = false)))
        assertEquals("us", p.country)
        assertEquals(true, p.overridden)
    }

    @Test
    fun `primaryOf dual sim picks the default subscription`() {
        val sims = listOf(
            sim("us", overridden = false, isDefault = false, slot = 1, subId = 2),
            sim("jp", overridden = true, isDefault = true, slot = 2, subId = 3),
        )
        val p = RegionLogic.primaryOf(sims)
        assertEquals("jp", p.country)
        assertEquals(true, p.overridden)
    }

    @Test
    fun `primaryOf dual sim with no default yields null country`() {
        val sims = listOf(
            sim("us", overridden = false, isDefault = false, slot = 1, subId = 2),
            sim("jp", overridden = true, isDefault = false, slot = 2, subId = 3),
        )
        assertNull(RegionLogic.primaryOf(sims).country)
    }
}
