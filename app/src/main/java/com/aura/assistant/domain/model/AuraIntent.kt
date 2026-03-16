package com.aura.assistant.domain.model

/**
 * Represents a parsed intent from natural language input.
 * The LLM parses user speech into one of these structured intents
 * which are then routed to the appropriate executor.
 */
sealed class AuraIntent {

    /** Make a phone call to a contact or number. */
    data class MakeCall(
        val contactName: String? = null,
        val phoneNumber: String? = null
    ) : AuraIntent()

    /** Send a text message. */
    data class SendMessage(
        val contactName: String? = null,
        val phoneNumber: String? = null,
        val message: String
    ) : AuraIntent()

    /** Start navigation to a destination. */
    data class Navigate(
        val destination: String,
        val mode: String = "driving"
    ) : AuraIntent()

    /** Create a reminder or task. */
    data class CreateReminder(
        val title: String,
        val description: String = "",
        val triggerTimeMs: Long
    ) : AuraIntent()

    /** Query the user's agenda or schedule. */
    data class QueryAgenda(
        val date: String? = null
    ) : AuraIntent()

    /** Add a calendar event. */
    data class AddCalendarEvent(
        val title: String,
        val description: String = "",
        val startTimeMs: Long,
        val endTimeMs: Long,
        val location: String = ""
    ) : AuraIntent()

    /** A general conversational response or query. */
    data class GeneralResponse(
        val text: String
    ) : AuraIntent()

    /** Query about the current context or status. */
    object QueryContext : AuraIntent()

    /** Unknown or unparseable intent. */
    data class Unknown(
        val rawText: String
    ) : AuraIntent()
}
