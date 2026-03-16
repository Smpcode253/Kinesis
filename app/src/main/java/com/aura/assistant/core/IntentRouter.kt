package com.aura.assistant.core

import android.util.Log
import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.model.UserContext
import com.aura.assistant.executor.CommsExecutor
import com.aura.assistant.executor.NavExecutor
import com.aura.assistant.executor.TaskExecutor

/**
 * Routes a parsed [AuraIntent] to the correct executor and returns the result.
 * Acts as the central dispatcher in the Aura core loop.
 */
class IntentRouter(
    private val commsExecutor: CommsExecutor,
    private val navExecutor: NavExecutor,
    private val taskExecutor: TaskExecutor
) {

    /**
     * Routes an intent to the appropriate executor.
     * @return An [ExecutorResult] describing the outcome.
     */
    suspend fun route(intent: AuraIntent, context: UserContext): ExecutorResult {
        Log.d(TAG, "Routing intent: ${intent::class.simpleName} in context: ${context.label}")
        return when (intent) {
            is AuraIntent.MakeCall -> commsExecutor.makeCall(intent, context)
            is AuraIntent.SendMessage -> commsExecutor.sendMessage(intent, context)
            is AuraIntent.Navigate -> navExecutor.navigate(intent, context)
            is AuraIntent.CreateReminder -> taskExecutor.createReminder(intent)
            is AuraIntent.QueryAgenda -> taskExecutor.queryAgenda(intent)
            is AuraIntent.AddCalendarEvent -> taskExecutor.addCalendarEvent(intent)
            is AuraIntent.QueryContext -> ExecutorResult.Success(
                "Your current context is: ${context.label}."
            )
            is AuraIntent.GeneralResponse -> ExecutorResult.Success(intent.text)
            is AuraIntent.Unknown -> ExecutorResult.Failure(
                "I'm sorry, I didn't understand: '${intent.rawText}'"
            )
        }
    }

    companion object {
        private const val TAG = "IntentRouter"
    }
}
