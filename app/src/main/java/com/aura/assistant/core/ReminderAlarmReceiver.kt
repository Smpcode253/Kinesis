package com.aura.assistant.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aura.assistant.R

/**
 * Receives alarm broadcasts to fire reminder notifications.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(nm)

        val openIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            Intent(context, com.aura.assistant.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aura_notification)
            .setContentTitle("Aura Reminder")
            .setContentText(title)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .build()

        nm.notify(reminderId.toInt() + NOTIFICATION_OFFSET, notification)
    }

    private fun createChannel(nm: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Aura Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminder notifications from Aura"
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        private const val CHANNEL_ID = "aura_reminders_channel"
        private const val NOTIFICATION_OFFSET = 5000
    }
}
