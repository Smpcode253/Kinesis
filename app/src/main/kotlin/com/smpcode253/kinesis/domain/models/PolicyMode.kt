package com.smpcode253.kinesis.domain.models

/**
 * Defines how Kinesis executes a resolved intent for a given trust domain.
 *
 * Ordered from most permissive ([AutoExecute]) to most restrictive ([SuggestOnly]).
 */
sealed class PolicyMode(val key: String) {

    /** Execute the action immediately with no confirmation. */
    object AutoExecute : PolicyMode("AUTO_EXECUTE")

    /** Read back the proposed action and wait for a spoken "yes/no" reply. */
    object ConfirmVoice : PolicyMode("CONFIRM_VOICE")

    /** Show a confirmation card that the user must tap to approve. */
    object ConfirmTap : PolicyMode("CONFIRM_TAP")

    /** Only surface a suggestion; never act without an explicit user tap. */
    object SuggestOnly : PolicyMode("SUGGEST_ONLY")

    companion object {
        /** Deserialise a persisted [key] back to the corresponding [PolicyMode]. */
        fun fromKey(key: String): PolicyMode = when (key) {
            AutoExecute.key  -> AutoExecute
            ConfirmVoice.key -> ConfirmVoice
            ConfirmTap.key   -> ConfirmTap
            else             -> SuggestOnly
        }
    }
}
