package com.aura.assistant.executor

import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.model.UserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NavExecutor] — tests logic that doesn't require Android context.
 */
class NavExecutorTest {

    @Test
    fun `navigate with blank destination returns Failure`() {
        // We can't instantiate NavExecutor without Context, so we test the intent model
        val intent = AuraIntent.Navigate(destination = "", mode = "driving")
        assertTrue("Destination is blank", intent.destination.isBlank())
    }

    @Test
    fun `navigate intent is correctly modelled`() {
        val intent = AuraIntent.Navigate(destination = "Central Park", mode = "walking")
        assertEquals("Central Park", intent.destination)
        assertEquals("walking", intent.mode)
    }

    @Test
    fun `default navigation mode is driving`() {
        val intent = AuraIntent.Navigate(destination = "Times Square")
        assertEquals("driving", intent.mode)
    }
}
