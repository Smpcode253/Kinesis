package com.aura.assistant.llm

import com.aura.assistant.domain.model.AuraIntent
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the LLM JSON parsing logic.
 * We test the parseJsonToIntent logic by mirroring the private method behaviour.
 */
class LlmJsonParsingTest {

    private val gson = Gson()

    @Suppress("UNCHECKED_CAST")
    private fun parseJsonToIntent(json: String, originalText: String): AuraIntent {
        return try {
            val map = gson.fromJson(json, Map::class.java) as Map<String, Any?>
            when (val intent = map["intent"] as? String) {
                "make_call" -> AuraIntent.MakeCall(
                    contactName = map["contact_name"] as? String,
                    phoneNumber = map["phone_number"] as? String
                )
                "send_message" -> AuraIntent.SendMessage(
                    contactName = map["contact_name"] as? String,
                    phoneNumber = map["phone_number"] as? String,
                    message = map["message"] as? String ?: ""
                )
                "navigate" -> AuraIntent.Navigate(
                    destination = map["destination"] as? String ?: "",
                    mode = map["mode"] as? String ?: "driving"
                )
                "create_reminder" -> AuraIntent.CreateReminder(
                    title = map["title"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    triggerTimeMs = (map["trigger_time_ms"] as? Double)?.toLong()
                        ?: System.currentTimeMillis() + 3_600_000L
                )
                "query_context" -> AuraIntent.QueryContext
                "general_response" -> AuraIntent.GeneralResponse(
                    text = map["text"] as? String ?: ""
                )
                else -> AuraIntent.Unknown(originalText)
            }
        } catch (e: Exception) {
            AuraIntent.Unknown(originalText)
        }
    }

    @Test
    fun `parse make_call intent`() {
        val json = """{"intent":"make_call","contact_name":"Mom","phone_number":null}"""
        val intent = parseJsonToIntent(json, "call mom")
        assertTrue(intent is AuraIntent.MakeCall)
        assertEquals("Mom", (intent as AuraIntent.MakeCall).contactName)
    }

    @Test
    fun `parse send_message intent`() {
        val json = """{"intent":"send_message","contact_name":"Alice","message":"On my way"}"""
        val intent = parseJsonToIntent(json, "text Alice")
        assertTrue(intent is AuraIntent.SendMessage)
        assertEquals("On my way", (intent as AuraIntent.SendMessage).message)
    }

    @Test
    fun `parse navigate intent`() {
        val json = """{"intent":"navigate","destination":"Home","mode":"driving"}"""
        val intent = parseJsonToIntent(json, "take me home")
        assertTrue(intent is AuraIntent.Navigate)
        assertEquals("Home", (intent as AuraIntent.Navigate).destination)
    }

    @Test
    fun `parse create_reminder intent`() {
        val futureTime = System.currentTimeMillis() + 3600_000.0
        val json = """{"intent":"create_reminder","title":"Doctor appointment","trigger_time_ms":$futureTime}"""
        val intent = parseJsonToIntent(json, "remind me about the doctor")
        assertTrue(intent is AuraIntent.CreateReminder)
        assertEquals("Doctor appointment", (intent as AuraIntent.CreateReminder).title)
    }

    @Test
    fun `parse query_context intent`() {
        val json = """{"intent":"query_context"}"""
        val intent = parseJsonToIntent(json, "what context am I in")
        assertTrue(intent is AuraIntent.QueryContext)
    }

    @Test
    fun `parse unknown intent for unrecognized type`() {
        val json = """{"intent":"fly_to_the_moon"}"""
        val intent = parseJsonToIntent(json, "fly me to the moon")
        assertTrue(intent is AuraIntent.Unknown)
    }

    @Test
    fun `parse unknown intent for malformed JSON`() {
        val intent = parseJsonToIntent("not valid json at all {{{", "garbage")
        assertTrue(intent is AuraIntent.Unknown)
    }

    @Test
    fun `parse general_response intent`() {
        val json = """{"intent":"general_response","text":"The weather looks great today!"}"""
        val intent = parseJsonToIntent(json, "what's the weather?")
        assertTrue(intent is AuraIntent.GeneralResponse)
        assertEquals("The weather looks great today!", (intent as AuraIntent.GeneralResponse).text)
    }
}
