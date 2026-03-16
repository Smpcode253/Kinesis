package com.aura.assistant.data.db.dao

import androidx.room.*
import com.aura.assistant.data.db.entities.ConversationEntry

@Dao
interface ConversationHistoryDao {

    @Query("SELECT * FROM conversation_history ORDER BY timestampMs ASC")
    suspend fun getFullHistory(): List<ConversationEntry>

    @Query("SELECT * FROM conversation_history ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<ConversationEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ConversationEntry): Long

    @Query("DELETE FROM conversation_history")
    suspend fun clearHistory()

    @Query("DELETE FROM conversation_history WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
