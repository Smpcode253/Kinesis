package com.aura.assistant.domain.model

/**
 * Represents the user's current context.
 * Aura adapts its behavior based on the active context.
 */
enum class UserContext(val label: String) {
    DRIVING("Driving"),
    HOME("Home"),
    WORK("Work"),
    WALKING("Walking"),
    UNKNOWN("Unknown");

    companion object {
        fun fromLabel(label: String): UserContext =
            values().firstOrNull { it.label.equals(label, ignoreCase = true) } ?: UNKNOWN
    }
}

/**
 * Represents the result of an executor action.
 */
sealed class ExecutorResult {
    data class Success(val message: String) : ExecutorResult()
    data class Failure(val error: String) : ExecutorResult()
    data class RequiresPermission(val permission: String) : ExecutorResult()
    data class NeedsConfirmation(val prompt: String, val action: () -> Unit) : ExecutorResult()
}
