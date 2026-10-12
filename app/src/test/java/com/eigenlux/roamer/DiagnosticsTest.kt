package com.eigenlux.roamer

import com.eigenlux.roamer.core.Diagnostics
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the environment line shown with a failure in the log.
 */
class DiagnosticsTest {

    @Test
    fun `format lists version, Android release, brand, user, and Shizuku identity`() {
        assertEquals(
            "Roamer 1.3.1 · Android 14 (API 34) · samsung · user 10 · Shizuku uid 2000",
            Diagnostics.format("1.3.1", "14", 34, "samsung", 10, 2000),
        )
    }

    @Test
    fun `format marks missing values and a stopped Shizuku service`() {
        assertEquals(
            "Roamer ? · Android ? (API 34) · ? · user 0 · Shizuku not running",
            Diagnostics.format("", "", 34, "", 0, null),
        )
    }
}
