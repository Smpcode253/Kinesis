package com.smpcode253.kinesis.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records an action that was dispatched (or proposed) for an [IntentRecord].
 *
 * [status] tracks the lifecycle of the action:
 * - `PENDING`   – awaiting user confirmation
 * - `CONFIRMED` – user approved the action
 * - `EXECUTED`  – action was sent to the relevant system API
 * - `REJECTED`  – user dismissed the action
 * - `FAILED`    – execution attempted but the underlying API returned an error
 */
@Entity(
    tableName = "action_records",
    foreignKeys = [
        ForeignKey(
            entity = IntentRecord::class,
            parentColumns = ["id"],
            childColumns = ["intentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("intentId")]
)
data class ActionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intentId: Long,
    /** Domain the action belongs to, e.g. "COMMS" | "NAV" | "TASKS". */
    val domain: String,
    /** Action verb, e.g. "CALL" | "REPLY" | "CREATE_REMINDER". */
    val action: String,
    /** Human-readable description of why this action was chosen. */
    val reason: String,
    /** What triggered this action, e.g. "AUTO" | "USER_CONFIRM". */
    val source: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
