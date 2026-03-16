package com.aura.assistant.domain

import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.UserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for domain model classes.
 */
class AuraIntentTest {

    @Test
    fun `MakeCall intent with phone number is correctly modelled`() {
        val intent = AuraIntent.MakeCall(phoneNumber = "+15550100")
        assertEquals("+15550100", intent.phoneNumber)
        assertNull(intent.contactName)
    }

    @Test
    fun `MakeCall intent with contact name is correctly modelled`() {
        val intent = AuraIntent.MakeCall(contactName = "Mom")
        assertEquals("Mom", intent.contactName)
        assertNull(intent.phoneNumber)
    }

    @Test
    fun `SendMessage intent requires message content`() {
        val intent = AuraIntent.SendMessage(contactName = "Alice", message = "Hello!")
        assertEquals("Hello!", intent.message)
        assertTrue(intent.message.isNotBlank())
    }

    @Test
    fun `CreateReminder intent stores title and trigger time`() {
        val triggerTime = System.currentTimeMillis() + 3600_000L
        val intent = AuraIntent.CreateReminder(title = "Buy groceries", triggerTimeMs = triggerTime)
        assertEquals("Buy groceries", intent.title)
        assertEquals(triggerTime, intent.triggerTimeMs)
    }

    @Test
    fun `Navigate intent defaults to driving mode`() {
        val intent = AuraIntent.Navigate(destination = "Airport")
        assertEquals("driving", intent.mode)
    }

    @Test
    fun `Unknown intent stores raw text`() {
        val intent = AuraIntent.Unknown("xyzzy")
        assertEquals("xyzzy", intent.rawText)
    }
}
