package com.aura.assistant.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a reminder or task stored locally on the device.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTimeMs: Long,
    val isCompleted: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)
