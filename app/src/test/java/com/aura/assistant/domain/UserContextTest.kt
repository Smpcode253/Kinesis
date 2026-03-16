package com.aura.assistant.domain

import com.aura.assistant.domain.model.UserContext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [UserContext].
 */
class UserContextTest {

    @Test
    fun `fromLabel returns correct context for known labels`() {
        assertEquals(UserContext.DRIVING, UserContext.fromLabel("Driving"))
        assertEquals(UserContext.HOME, UserContext.fromLabel("Home"))
        assertEquals(UserContext.WORK, UserContext.fromLabel("Work"))
        assertEquals(UserContext.WALKING, UserContext.fromLabel("Walking"))
        assertEquals(UserContext.UNKNOWN, UserContext.fromLabel("Unknown"))
    }

    @Test
    fun `fromLabel is case-insensitive`() {
        assertEquals(UserContext.DRIVING, UserContext.fromLabel("DRIVING"))
        assertEquals(UserContext.HOME, UserContext.fromLabel("home"))
        assertEquals(UserContext.WORK, UserContext.fromLabel("Work"))
    }

    @Test
    fun `fromLabel returns UNKNOWN for unrecognized labels`() {
        assertEquals(UserContext.UNKNOWN, UserContext.fromLabel(""))
        assertEquals(UserContext.UNKNOWN, UserContext.fromLabel("flying"))
        assertEquals(UserContext.UNKNOWN, UserContext.fromLabel("sleeping"))
    }

    @Test
    fun `each context has a non-blank label`() {
        UserContext.values().forEach { context ->
            assert(context.label.isNotBlank()) { "${context.name} has blank label" }
        }
    }
}
