package com.smpcode253.kinesis.domain.models

/**
 * Represents all possible intents that Kinesis can recognise and act upon.
 *
 * Using a sealed class hierarchy makes every `when` expression exhaustive and
 * ensures new intent types must be explicitly handled at the call-site.
 */
sealed class AssistantIntent {

    /** No intent was detected. */
    object None : AssistantIntent()

    /** Initiate an outgoing call or video call. */
    data class CommsCall(
        val contactName: String?,
        val mode: String?          // e.g. "audio" | "video"
    ) : AssistantIntent()

    /** Answer an incoming call. */
    data class CommsAnswerCall(
        val callId: String?,
        val mode: String?          // e.g. "audio" | "video"
    ) : AssistantIntent()

    /** Send a message reply to a contact. */
    data class CommsReply(
        val recipientName: String?,
        val messageText: String?
    ) : AssistantIntent()

    /** Start navigation to a destination. */
    data class NavStart(
        val destinationType: String?,     // e.g. "address" | "poi" | "contact_home"
        val destinationText: String?,
        val contactName: String?,
        val sourceMessageHint: String?    // context clue extracted from a message
    ) : AssistantIntent()

    /** Create a reminder that fires at a time or location. */
    data class TaskCreateReminder(
        val text: String?,
        val triggerType: String?,         // e.g. "time" | "location"
        val triggerTime: String?,
        val triggerLocationLabel: String?
    ) : AssistantIntent()

    /** Show the user's agenda for a given day. */
    data class TaskViewAgenda(
        val day: String?                  // e.g. "today" | "tomorrow" | ISO-8601 date
    ) : AssistantIntent()

    /** Create a calendar event. */
    data class TaskCreateEvent(
        val title: String?,
        val timeText: String?,
        val participants: List<String>?,
        val locationText: String?
    ) : AssistantIntent()
}
