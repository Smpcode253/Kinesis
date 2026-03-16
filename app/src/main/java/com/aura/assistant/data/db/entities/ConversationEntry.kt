package com.aura.assistant.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the conversation history between the user and Aura for context.
 * All data is stored locally on the device.
 */
@Entity(tableName = "conversation_history")
data class ConversationEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** "user" or "assistant" */
    val role: String,
    val content: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val contextTag: String = ""
)
