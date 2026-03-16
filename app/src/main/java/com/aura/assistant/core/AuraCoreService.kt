package com.aura.assistant.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aura.assistant.AuraApplication
import com.aura.assistant.MainActivity
import com.aura.assistant.R
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.model.UserContext
import com.aura.assistant.speech.SpeechManager
import com.aura.assistant.speech.SttEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * The core foreground service that drives the Aura assistant loop.
 *
 * It manages:
 * - Speech recognition (VTT) to capture user commands
 * - Context awareness via [ContextManager]
 * - LLM intent parsing and routing via [IntentRouter]
 * - Text-to-Speech (TTS) responses
 */
class AuraCoreService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val app: AuraApplication get() = application as AuraApplication

    private lateinit var speechManager: SpeechManager
    private lateinit var contextManager: ContextManager
    private lateinit var intentRouter: IntentRouter

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AuraCoreService created")

        speechManager = SpeechManager(this)
        contextManager = ContextManager(this)
        intentRouter = IntentRouter(
            commsExecutor = app.commsExecutor,
            navExecutor = app.navExecutor,
            taskExecutor = app.taskExecutor
        )

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Aura is ready."))

        speechManager.initTts()
        speechManager.initStt()

        // Observe STT events
        speechManager.sttEvents
            .onEach { event -> handleSttEvent(event) }
            .launchIn(serviceScope)

        // Start context monitoring
        contextManager.startMonitoring { newContext ->
            Log.d(TAG, "Context changed: $newContext")
            updateNotification("Context: ${newContext.label}")
        }

        speechManager.speak("Aura is ready. How can I help you?")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> speechManager.startListening()
            ACTION_STOP_LISTENING -> speechManager.stopListening()
            ACTION_STOP_SERVICE -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AuraCoreService destroyed")
        contextManager.stopMonitoring()
        speechManager.destroy()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleSttEvent(event: SttEvent) {
        when (event) {
            is SttEvent.ReadyForSpeech -> updateNotification("Listening…")
            is SttEvent.SpeechBegun -> updateNotification("Hearing you…")
            is SttEvent.SpeechEnded -> updateNotification("Processing…")
            is SttEvent.Result -> processUserInput(event.text)
            is SttEvent.PartialResult -> Log.d(TAG, "Partial: ${event.text}")
            is SttEvent.Error -> {
                Log.e(TAG, "STT error: ${event.message}")
                if (event.code != android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                    speechManager.speak("Sorry, I had trouble hearing that. Please try again.")
                }
                updateNotification("Aura is ready.")
            }
        }
    }

    private fun processUserInput(userText: String) {
        Log.d(TAG, "Processing user input: $userText")
        updateNotification("Thinking…")
        serviceScope.launch(Dispatchers.IO) {
            val currentContext = contextManager.currentContext
            val intent = app.llmClient.parseIntent(userText, currentContext)
            val result = intentRouter.route(intent, currentContext)
            handleExecutorResult(result)
        }
    }

    private fun handleExecutorResult(result: ExecutorResult) {
        when (result) {
            is ExecutorResult.Success -> {
                speechManager.speak(result.message)
                updateNotification("Aura is ready.")
            }
            is ExecutorResult.Failure -> {
                speechManager.speak(result.error)
                updateNotification("Aura is ready.")
            }
            is ExecutorResult.RequiresPermission -> {
                speechManager.speak("I need permission to do that. Please grant the required permission in Settings.")
                updateNotification("Needs permission.")
            }
            is ExecutorResult.NeedsConfirmation -> {
                speechManager.speak(result.prompt)
                // In a real implementation this would wait for voice confirmation
                updateNotification("Awaiting confirmation.")
            }
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Aura Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Aura voice assistant status"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(status: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_aura_notification)
        .setContentTitle("Aura")
        .setContentText(status)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_mic,
            "Listen",
            PendingIntent.getService(
                this, 1,
                Intent(this, AuraCoreService::class.java).apply { action = ACTION_START_LISTENING },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_stop,
            "Stop",
            PendingIntent.getService(
                this, 2,
                Intent(this, AuraCoreService::class.java).apply { action = ACTION_STOP_SERVICE },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun updateNotification(status: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status))
    }

    companion object {
        private const val TAG = "AuraCoreService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "aura_core_channel"

        const val ACTION_START_LISTENING = "com.aura.assistant.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.aura.assistant.ACTION_STOP_LISTENING"
        const val ACTION_STOP_SERVICE = "com.aura.assistant.ACTION_STOP_SERVICE"
    }
}
