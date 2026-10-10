package com.eigenlux.roamer

import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.core.RegionLogic.PrimaryState
import com.eigenlux.roamer.data.Carrier
import com.eigenlux.roamer.data.CountryPresets
import com.eigenlux.roamer.data.CustomRegion
import com.eigenlux.roamer.data.RegionConfig
import com.eigenlux.roamer.data.RegionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the user's region list: defaults, validation, and text encoding.
 */
class RegionRulesTest {

    @Test
    fun `defaults mirror the presets and keep their order`() {
        val defaults = RegionRules.defaultRegions()
        assertEquals(CountryPresets.all.map { it.iso }, defaults.map { it.iso })
        val us = defaults.first { it.iso == "us" }
        assertEquals(listOf("Verizon", "T-Mobile", "AT&T"), us.enabledCarriers)
        assertTrue(us.carriers.all { it.enabled })
    }

    @Test
    fun `default quick switch only lists known regions`() {
        assertEquals(listOf("us", "jp", "kr", "cn", "hk"), RegionRules.defaultQuickSwitch(RegionRules.defaultRegions()))
        val withoutJp = RegionRules.defaultRegions().filterNot { it.iso == "jp" }
        assertEquals(listOf("us", "kr", "cn", "hk"), RegionRules.defaultQuickSwitch(withoutJp))
    }

    @Test
    fun `encode and decode round-trip the default list unchanged`() {
        val defaults = RegionRules.defaultRegions()
        assertEquals(defaults, RegionRules.decode(RegionRules.encode(defaults)))
    }

    @Test
    fun `encode and decode keep order, added entries, and entries outside the list`() {
        val regions = listOf(
            RegionConfig("zz", listOf(Carrier("Custom"))),
            RegionConfig("us", listOf(Carrier("Mint"), Carrier("AT&T"), Carrier("T-Mobile", enabled = false), Carrier("Verizon"))),
            RegionConfig("jp", RegionRules.defaultRegion("jp").carriers, enabled = false),
        )
        assertEquals(regions, RegionRules.decode(RegionRules.encode(regions)))
    }

    @Test
    fun `decode drops invalid and repeated entries and appends missing preset carriers`() {
        val decoded = RegionRules.decode("US\tVerizon\tVerizon\t \nxyz\tNope\nus\tDuplicate\n1a\n")
        val us = RegionConfig(
            "us",
            listOf(Carrier("Verizon"), Carrier("T-Mobile"), Carrier("AT&T")),
        )
        assertEquals(listOf(us), decoded)
    }

    @Test
    fun `removing a region keeps its carriers, and adding it back puts it at the end of the list`() {
        val start = listOf(
            RegionConfig("us", listOf(Carrier("Mint"))),
            RegionConfig("jp", emptyList()),
            RegionConfig("kr", emptyList()),
        )
        val removed = RegionRules.removeFromList(start, "us")
        assertEquals(listOf("jp", "kr"), removed.filter { it.enabled }.map { it.iso })
        val back = RegionRules.addToList(removed, "us")
        assertEquals(listOf("jp", "kr", "us"), back.filter { it.enabled }.map { it.iso })
        assertEquals(listOf("Mint"), back.first { it.iso == "us" }.enabledCarriers)
        assertEquals(back, RegionRules.addToList(back, "us"))
        assertEquals(listOf("Verizon", "T-Mobile", "AT&T"), RegionRules.addToList(emptyList(), "us").single().enabledCarriers)
    }

    @Test
    fun `moveInList reorders only the regions in the list`() {
        val regions = listOf(
            RegionConfig("us", emptyList()),
            RegionConfig("jp", emptyList()),
            RegionConfig("kr", emptyList()),
            RegionConfig("de", emptyList(), enabled = false),
        )
        assertEquals(listOf("jp", "kr", "us", "de"), RegionRules.moveInList(regions, 0, 2).map { it.iso })
        assertEquals(regions, RegionRules.moveInList(regions, 0, 3))
    }

    @Test
    fun `custom regions round-trip and invalid ones are dropped`() {
        val customs = listOf(CustomRegion("xk", "Kosovo"), CustomRegion("zz", "测试地区"))
        assertEquals(customs, RegionRules.decodeCustom(RegionRules.encodeCustom(customs)))
        assertEquals(listOf(CustomRegion("xk", "Kosovo")), RegionRules.decodeCustom("XK\tKosovo\nxk\tAgain\nq\tBad\nyy\t \n"))
        assertNull(RegionRules.cleanRegionName("  \t"))
    }

    @Test
    fun `normalizeCarriers keeps added carriers enabled and removes repeats`() {
        val normalized = RegionRules.normalizeCarriers(
            "kr",
            listOf(Carrier("Mine", enabled = false), Carrier("KT", enabled = false), Carrier("Mine")),
        )
        assertEquals(
            listOf(Carrier("Mine"), Carrier("KT", enabled = false), Carrier("SK Telecom"), Carrier("LG U+")),
            normalized,
        )
        assertTrue(RegionRules.isPresetCarrier("kr", "KT"))
        assertFalse(RegionRules.isPresetCarrier("kr", "Mine"))
    }

    @Test
    fun `cleanCarrier trims, strips control characters, and limits length`() {
        assertEquals("Orange", RegionRules.cleanCarrier("  Orange\t\n"))
        assertNull(RegionRules.cleanCarrier(" \t "))
        assertEquals(RegionRules.MAX_CARRIER_LENGTH, RegionRules.cleanCarrier("x".repeat(80))!!.length)
    }

    @Test
    fun `isValidIso accepts only two lowercase letters`() {
        assertTrue(RegionRules.isValidIso("eg"))
        assertFalse(RegionRules.isValidIso("EG"))
        assertFalse(RegionRules.isValidIso("egy"))
        assertFalse(RegionRules.isValidIso("e1"))
    }

    @Test
    fun `cleanQuickSwitch removes unknown, disabled, and repeated codes and caps the count`() {
        val regions = listOf("us", "jp", "kr", "cn", "hk", "sg", "th").map { RegionConfig(it, emptyList()) } +
            RegionConfig("de", emptyList(), enabled = false)
        assertEquals(listOf("jp", "us"), RegionRules.cleanQuickSwitch(listOf("jp", "zz", "de", "us", "jp"), regions))
        assertEquals(
            RegionRules.MAX_QUICK_SWITCH,
            RegionRules.cleanQuickSwitch(regions.map { it.iso }, regions).size,
        )
    }

    @Test
    fun `desiredTag uses the supplied locale lookup for regions outside the presets`() {
        val egypt = PrimaryState(country = "eg", overridden = true)
        assertEquals("", RegionLogic.desiredTag(masterOn = true, primary = egypt, baseline = ""))
        assertEquals(
            "ar-EG",
            RegionLogic.desiredTag(masterOn = true, primary = egypt, baseline = "") { "ar-EG" },
        )
    }
}
