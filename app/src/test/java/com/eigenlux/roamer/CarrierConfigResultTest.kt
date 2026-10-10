package com.eigenlux.roamer

import com.eigenlux.roamer.core.CarrierConfigController.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for merging carrier config operation results.
 */
class CarrierConfigResultTest {

    @Test
    fun `plus of two successes stays successful and sums counts`() {
        val merged = Result(true, "a") + Result(true, "b", total = 2, succeeded = 2)
        assertTrue(merged.ok)
        assertFalse(merged.partial)
        assertEquals(3, merged.total)
        assertEquals(3, merged.succeeded)
        assertEquals("a\nb", merged.output)
    }

    @Test
    fun `plus of a success and a failure is partial`() {
        val merged = Result(true, "a") + Result(false, "b")
        assertFalse(merged.ok)
        assertTrue(merged.partial)
    }

    @Test
    fun `plus of two failures is a plain failure`() {
        val merged = Result(false, "a") + Result(false, "b")
        assertFalse(merged.ok)
        assertFalse(merged.partial)
    }
}
