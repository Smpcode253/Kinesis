package com.aura.assistant.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.aura.assistant.data.db.entities.Reminder

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY triggerTimeMs ASC")
    fun getAllReminders(): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerTimeMs ASC")
    fun getPendingReminders(): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun deleteCompletedReminders()
}
