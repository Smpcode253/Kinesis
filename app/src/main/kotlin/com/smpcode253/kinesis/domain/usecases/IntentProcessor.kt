package com.smpcode253.kinesis.domain.usecases

import com.google.gson.Gson
import com.smpcode253.kinesis.data.models.ActionRecord
import com.smpcode253.kinesis.data.models.Event
import com.smpcode253.kinesis.data.models.IntentRecord
import com.smpcode253.kinesis.data.repository.KinesisRepository
import com.smpcode253.kinesis.domain.models.AssistantIntent
import com.smpcode253.kinesis.domain.models.PolicyMode

/**
 * Orchestrates the full lifecycle of a single assistant interaction:
 *
 * 1. Persist the raw [Event].
 * 2. Determine the applicable [PolicyMode] via [PolicyEvaluator].
 * 3. Persist an [IntentRecord] with the resolved intent and policy.
 * 4. Create an [ActionRecord] whose initial status reflects the policy:
 *    - [PolicyMode.AutoExecute]  → status = `EXECUTED`  (fire-and-forget)
 *    - [PolicyMode.ConfirmVoice] → status = `PENDING`
 *    - [PolicyMode.ConfirmTap]   → status = `PENDING`
 *    - [PolicyMode.SuggestOnly]  → status = `PENDING`
 */
class IntentProcessor(
    private val repository: KinesisRepository,
    private val policyEvaluator: PolicyEvaluator = PolicyEvaluator()
) {
    private val gson = Gson()

    /**
     * Processes [intent] that was triggered by [event].
     *
     * Returns the persisted [ActionRecord] so callers can drive the
     * confirmation UI if needed.
     */
    suspend fun process(event: Event, intent: AssistantIntent): ActionRecord {
        val eventId = repository.saveEvent(event)
        val prefs = repository.getPreferences()
        val policy = policyEvaluator.evaluate(intent, prefs)

        val intentRecord = IntentRecord(
            eventId = eventId,
            intentType = intent::class.simpleName ?: "Unknown",
            slotsJson = gson.toJson(intent),
            policyMode = policy.key
        )
        val intentId = repository.saveIntentRecord(intentRecord)

        val (domain, action) = domainAndAction(intent)
        val status = if (policy is PolicyMode.AutoExecute) "EXECUTED" else "PENDING"
        val source = if (policy is PolicyMode.AutoExecute) "AUTO" else "POLICY_${policy.key}"

        val actionRecord = ActionRecord(
            intentId = intentId,
            domain = domain,
            action = action,
            reason = "Policy: ${policy.key}",
            source = source,
            status = status
        )
        val actionId = repository.saveActionRecord(actionRecord)
        return actionRecord.copy(id = actionId)
    }

    /** Confirm a pending action (e.g. after voice or tap confirmation). */
    suspend fun confirm(actionId: Long) =
        repository.updateActionStatus(actionId, "CONFIRMED")

    /** Reject a pending action (e.g. user said "no"). */
    suspend fun reject(actionId: Long) =
        repository.updateActionStatus(actionId, "REJECTED")

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun domainAndAction(intent: AssistantIntent): Pair<String, String> = when (intent) {
        is AssistantIntent.None              -> "NONE" to "NONE"
        is AssistantIntent.CommsCall         -> "COMMS" to "CALL"
        is AssistantIntent.CommsAnswerCall   -> "COMMS" to "ANSWER_CALL"
        is AssistantIntent.CommsReply        -> "COMMS" to "REPLY"
        is AssistantIntent.NavStart          -> "NAV" to "START_NAVIGATION"
        is AssistantIntent.TaskCreateReminder -> "TASKS" to "CREATE_REMINDER"
        is AssistantIntent.TaskViewAgenda    -> "TASKS" to "VIEW_AGENDA"
        is AssistantIntent.TaskCreateEvent   -> "TASKS" to "CREATE_EVENT"
    }
}
