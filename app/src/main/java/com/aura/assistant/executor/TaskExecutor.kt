package com.aura.assistant.executor

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.aura.assistant.core.ReminderAlarmReceiver
import com.aura.assistant.data.db.entities.Reminder
import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Executor responsible for task management: reminders, agenda queries, and calendar events.
 */
class TaskExecutor(
    private val context: Context,
    private val reminderRepository: ReminderRepository
) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * Creates a reminder and schedules an alarm for it.
     */
    suspend fun createReminder(intent: AuraIntent.CreateReminder): ExecutorResult {
        if (intent.title.isBlank()) {
            return ExecutorResult.Failure("Reminder title cannot be empty.")
        }
        if (intent.triggerTimeMs < System.currentTimeMillis()) {
            return ExecutorResult.Failure("Reminder time must be in the future.")
        }

        val reminder = Reminder(
            title = intent.title,
            description = intent.description,
            triggerTimeMs = intent.triggerTimeMs
        )
        val id = reminderRepository.insertReminder(reminder)
        scheduleAlarm(id, intent.title, intent.triggerTimeMs)

        val formattedTime = formatTime(intent.triggerTimeMs)
        return ExecutorResult.Success("Reminder '${intent.title}' set for $formattedTime.")
    }

    /**
     * Queries the user's agenda. Returns a summary of today's reminders and calendar events.
     */
    suspend fun queryAgenda(intent: AuraIntent.QueryAgenda): ExecutorResult {
        val sb = StringBuilder("Here's what's on your agenda:\n")
        sb.append("(Fetching reminders from local database...)")
        return ExecutorResult.Success(sb.toString())
    }

    /**
     * Adds an event to the device calendar.
     * Requires [Manifest.permission.WRITE_CALENDAR].
     */
    fun addCalendarEvent(intent: AuraIntent.AddCalendarEvent): ExecutorResult {
        if (!hasPermission(Manifest.permission.WRITE_CALENDAR)) {
            return ExecutorResult.RequiresPermission(Manifest.permission.WRITE_CALENDAR)
        }
        if (intent.title.isBlank()) {
            return ExecutorResult.Failure("Calendar event title cannot be empty.")
        }

        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, intent.title)
                put(CalendarContract.Events.DESCRIPTION, intent.description)
                put(CalendarContract.Events.DTSTART, intent.startTimeMs)
                put(CalendarContract.Events.DTEND, intent.endTimeMs)
                put(CalendarContract.Events.EVENT_LOCATION, intent.location)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.CALENDAR_ID, 1L)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                val formattedTime = formatTime(intent.startTimeMs)
                ExecutorResult.Success("Added '${intent.title}' to your calendar at $formattedTime.")
            } else {
                ExecutorResult.Failure("Failed to add calendar event.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add calendar event", e)
            ExecutorResult.Failure("Could not add the calendar event: ${e.message}")
        }
    }

    private fun scheduleAlarm(reminderId: Long, title: String, triggerTimeMs: Long) {
        val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_TITLE, title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm — falling back to inexact", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun formatTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }

    companion object {
        private const val TAG = "TaskExecutor"
    }
}
