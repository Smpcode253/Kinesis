package com.aura.assistant.domain

import com.aura.assistant.data.db.entities.TrustLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [TrustLevel] helper object.
 */
class TrustLevelTest {

    @Test
    fun `fromInt returns same value for valid levels`() {
        assertEquals(TrustLevel.NONE, TrustLevel.fromInt(TrustLevel.NONE))
        assertEquals(TrustLevel.LOW, TrustLevel.fromInt(TrustLevel.LOW))
        assertEquals(TrustLevel.MEDIUM, TrustLevel.fromInt(TrustLevel.MEDIUM))
        assertEquals(TrustLevel.HIGH, TrustLevel.fromInt(TrustLevel.HIGH))
    }

    @Test
    fun `fromInt returns MEDIUM for unknown values`() {
        assertEquals(TrustLevel.MEDIUM, TrustLevel.fromInt(-1))
        assertEquals(TrustLevel.MEDIUM, TrustLevel.fromInt(99))
    }

    @Test
    fun `toLabel returns correct string for each level`() {
        assertEquals("None", TrustLevel.toLabel(TrustLevel.NONE))
        assertEquals("Low", TrustLevel.toLabel(TrustLevel.LOW))
        assertEquals("Medium", TrustLevel.toLabel(TrustLevel.MEDIUM))
        assertEquals("High", TrustLevel.toLabel(TrustLevel.HIGH))
    }

    @Test
    fun `toLabel returns Medium for unknown values`() {
        assertEquals("Medium", TrustLevel.toLabel(-1))
        assertEquals("Medium", TrustLevel.toLabel(99))
    }
}
