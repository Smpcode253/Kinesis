package com.smpcode253.kinesis.domain.usecases

import com.smpcode253.kinesis.data.models.Preferences
import com.smpcode253.kinesis.domain.models.AssistantIntent
import com.smpcode253.kinesis.domain.models.PolicyMode

/**
 * Determines the [PolicyMode] that should govern execution of a given [AssistantIntent].
 *
 * The evaluation order is:
 * 1. Look up the trust setting for the intent's domain from [Preferences].
 * 2. Return the corresponding [PolicyMode].
 *
 * Future versions can extend this to factor in contextual signals such as
 * driving mode, quiet hours, or whether the contact is trusted.
 * The [com.smpcode253.kinesis.data.models.Preferences.trustFinance] field is
 * reserved for a future finance intent domain and is not yet mapped here.
 */
class PolicyEvaluator {

    /**
     * Returns the [PolicyMode] applicable to [intent] given the current [prefs].
     */
    fun evaluate(intent: AssistantIntent, prefs: Preferences): PolicyMode {
        val key = when (intent) {
            is AssistantIntent.None -> return PolicyMode.SuggestOnly

            is AssistantIntent.CommsCall,
            is AssistantIntent.CommsAnswerCall,
            is AssistantIntent.CommsReply -> prefs.trustComms

            is AssistantIntent.NavStart -> prefs.trustNav

            is AssistantIntent.TaskCreateReminder,
            is AssistantIntent.TaskViewAgenda,
            is AssistantIntent.TaskCreateEvent -> prefs.trustTasks
        }
        return PolicyMode.fromKey(key)
    }
}
