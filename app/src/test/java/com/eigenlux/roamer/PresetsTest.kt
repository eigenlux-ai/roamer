package com.eigenlux.roamer

import com.eigenlux.roamer.data.CarrierPresets
import com.eigenlux.roamer.data.CountryPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Consistency unit tests for country/carrier presets (pure static data) — the linked dropdown's correctness relies on these invariants. */
class PresetsTest {

    @Test
    fun `country isos are unique lowercase and two-letter`() {
        val isos = CountryPresets.all.map { it.iso }
        assertEquals("duplicate iso found", isos.size, isos.toSet().size)
        assertTrue("iso must be two lowercase letters", isos.all { it == it.lowercase() && it.length == 2 })
    }

    @Test
    fun `byIso is case-insensitive and unknown is null`() {
        assertEquals("cn", CountryPresets.byIso("CN")?.iso)
        assertEquals("us", CountryPresets.byIso("us")?.iso)
        assertNull(CountryPresets.byIso("zz"))
    }

    @Test
    fun `every preset carries a resolvable name resource and numeric mcc`() {
        // Display names live in string resources (localized); data-side we only assert each preset
        // points at a real (non-zero) @StringRes and a numeric MCC. Label text is verified on the UI side.
        CountryPresets.all.forEach { preset ->
            assertTrue("${preset.iso} nameRes must be non-zero", preset.nameRes != 0)
            assertTrue("${preset.iso} mcc must be numeric", preset.mcc.toIntOrNull() != null)
        }
        // Distinct name resources per country (no accidental copy-paste reuse across isos).
        val nameResIds = CountryPresets.all.map { it.nameRes }
        assertEquals("duplicate nameRes found", nameResIds.size, nameResIds.toSet().size)
    }

    @Test
    fun `carrier presets first item is non-blank default`() {
        CountryPresets.all.forEach { preset ->
            val carriers = CarrierPresets.forCountry(preset.iso)
            if (carriers.isNotEmpty()) {
                assertFalse("${preset.iso} default carrier is blank", carriers.first().isBlank())
            }
        }
    }

    @Test
    fun `forCountry is case-insensitive and unknown returns empty`() {
        assertTrue(CarrierPresets.forCountry("US").isNotEmpty())
        assertEquals(CarrierPresets.forCountry("us"), CarrierPresets.forCountry("US"))
        assertTrue(CarrierPresets.forCountry("zz").isEmpty())
    }
}
