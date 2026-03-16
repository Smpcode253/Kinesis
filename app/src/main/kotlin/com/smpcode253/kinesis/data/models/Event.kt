package com.smpcode253.kinesis.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw input event received by the assistant (e.g. a voice utterance, an incoming
 * message notification, or a sensor trigger).
 *
 * The [payloadJson] column stores a JSON representation of the event-specific
 * payload so that the schema can remain stable as new event types are introduced.
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Discriminator string, e.g. "VOICE_UTTERANCE" | "NOTIFICATION" | "SENSOR". */
    val type: String,
    /** JSON payload whose structure is determined by [type]. */
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
