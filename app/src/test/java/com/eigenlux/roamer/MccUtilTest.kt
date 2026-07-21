package com.eigenlux.roamer

import com.eigenlux.roamer.core.MccUtil
import com.eigenlux.roamer.data.CountryPresets
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for MccUtil's static fallback table.
 */
class MccUtilTest {

    @Test
    fun `known mcc maps to iso`() {
        assertEquals("cn", MccUtil.isoFromMccTable("46001"))
        assertEquals("cn", MccUtil.isoFromMccTable("460"))
        assertEquals("us", MccUtil.isoFromMccTable("310260"))
        assertEquals("us", MccUtil.isoFromMccTable("311"))
        assertEquals("hk", MccUtil.isoFromMccTable("454"))
        assertEquals("jp", MccUtil.isoFromMccTable("44010"))
    }

    @Test
    fun `unknown or invalid mcc returns empty`() {
        assertEquals("", MccUtil.isoFromMccTable("999"))
        assertEquals("", MccUtil.isoFromMccTable(""))
        assertEquals("", MccUtil.isoFromMccTable("ab"))
    }

    @Test
    fun `fallback covers every CountryPresets primary mcc`() {
        CountryPresets.all.forEach { preset ->
            assertEquals(
                "Primary MCC ${preset.mcc} of preset ${preset.iso} is not covered by the fallback table",
                preset.iso,
                MccUtil.isoFromMccTable(preset.mcc),
            )
        }
    }
}
