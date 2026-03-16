package com.aura.assistant

import android.app.Application
import android.util.Log
import com.aura.assistant.core.ContextManager
import com.aura.assistant.data.db.AuraDatabase
import com.aura.assistant.domain.repository.ContactRepository
import com.aura.assistant.domain.repository.ReminderRepository
import com.aura.assistant.executor.CommsExecutor
import com.aura.assistant.executor.NavExecutor
import com.aura.assistant.executor.TaskExecutor
import com.aura.assistant.llm.LlmClient
import com.aura.assistant.speech.SpeechManager

/**
 * Application class that holds singletons used across the app.
 * All user data is stored locally on the device (privacy-first).
 */
class AuraApplication : Application() {

    // ─── Database ─────────────────────────────────────────────────────────────

    val database: AuraDatabase by lazy {
        AuraDatabase.getInstance(this)
    }

    // ─── Repositories ─────────────────────────────────────────────────────────

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository(database.trustedContactDao())
    }

    // ─── Core Components ──────────────────────────────────────────────────────

    val contextManager: ContextManager by lazy {
        ContextManager(this)
    }

    val speechManager: SpeechManager by lazy {
        SpeechManager(this)
    }

    val llmClient: LlmClient by lazy {
        LlmClient(conversationHistoryDao = database.conversationHistoryDao())
    }

    // ─── Executors ────────────────────────────────────────────────────────────

    val commsExecutor: CommsExecutor by lazy {
        CommsExecutor(this, contactRepository)
    }

    val navExecutor: NavExecutor by lazy {
        NavExecutor(this)
    }

    val taskExecutor: TaskExecutor by lazy {
        TaskExecutor(this, reminderRepository)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AuraApplication created")
    }

    companion object {
        private const val TAG = "AuraApplication"
    }
}
