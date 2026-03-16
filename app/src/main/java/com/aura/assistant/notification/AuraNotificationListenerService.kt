package com.aura.assistant.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aura.assistant.AuraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * A [NotificationListenerService] that monitors incoming notifications and
 * surfaces them to Aura for proactive suggestions.
 *
 * To activate this service, the user must grant notification access via:
 * Settings → Apps & notifications → Special app access → Notification access.
 */
class AuraNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val app: AuraApplication get() = application as AuraApplication

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip our own notifications to avoid feedback loops
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        Log.d(TAG, "Notification posted from ${sbn.packageName}: $title — $text")

        val event = NotificationEvent(
            packageName = sbn.packageName,
            title = title,
            text = text,
            postedAtMs = System.currentTimeMillis()
        )

        serviceScope.launch {
            _notificationEvents.emit(event)
        }

        // Proactive suggestion: announce important-looking notifications while driving
        val currentContext = app.contextManager.currentContext
        if (currentContext == com.aura.assistant.domain.model.UserContext.DRIVING) {
            val isImportant = isImportantNotification(sbn.packageName, title, text)
            if (isImportant) {
                serviceScope.launch(Dispatchers.Main) {
                    app.speechManager.speak("Notification from $title: $text")
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "Notification removed from ${sbn?.packageName}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Determines if a notification is important enough to announce while driving.
     * Currently flags messages from known communication apps.
     */
    private fun isImportantNotification(packageName: String, title: String, text: String): Boolean {
        val communicationApps = setOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.whatsapp",
            "com.facebook.orca",
            "org.telegram.messenger",
            "com.microsoft.teams",
            "com.slack"
        )
        return packageName in communicationApps && text.isNotBlank()
    }

    companion object {
        private const val TAG = "AuraNotificationListener"

        private val _notificationEvents = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 64)
        val notificationEvents: SharedFlow<NotificationEvent> = _notificationEvents.asSharedFlow()
    }
}

data class NotificationEvent(
    val packageName: String,
    val title: String,
    val text: String,
    val postedAtMs: Long
)
