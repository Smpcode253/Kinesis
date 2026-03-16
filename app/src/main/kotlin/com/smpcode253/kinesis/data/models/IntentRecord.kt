package com.smpcode253.kinesis.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A resolved intent extracted from an [Event].
 *
 * Stores the intent type and its slots (parameters) as JSON so that the
 * schema does not need to change when new slot fields are added.
 */
@Entity(
    tableName = "intent_records",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId")]
)
data class IntentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    /** Discriminator matching an [com.smpcode253.kinesis.domain.models.AssistantIntent] subtype name. */
    val intentType: String,
    /** JSON map of slot name → slot value. */
    val slotsJson: String,
    /** Serialised [com.smpcode253.kinesis.domain.models.PolicyMode.key] that was active when this record was created. */
    val policyMode: String,
    val createdAt: Long = System.currentTimeMillis()
)
