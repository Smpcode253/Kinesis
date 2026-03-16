package com.aura.assistant.domain.repository

import androidx.lifecycle.LiveData
import com.aura.assistant.data.db.dao.ReminderDao
import com.aura.assistant.data.db.entities.Reminder

/**
 * Repository for managing reminders. Acts as a single source of truth
 * between the database and the rest of the application.
 */
class ReminderRepository(private val dao: ReminderDao) {

    val allReminders: LiveData<List<Reminder>> = dao.getAllReminders()
    val pendingReminders: LiveData<List<Reminder>> = dao.getPendingReminders()

    suspend fun getReminderById(id: Long): Reminder? = dao.getReminderById(id)

    suspend fun insertReminder(reminder: Reminder): Long = dao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = dao.deleteReminder(reminder)

    suspend fun markCompleted(id: Long) = dao.markCompleted(id)

    suspend fun deleteCompletedReminders() = dao.deleteCompletedReminders()
}
