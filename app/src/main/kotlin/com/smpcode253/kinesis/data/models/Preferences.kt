package com.smpcode253.kinesis.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-configurable preferences for Kinesis.
 *
 * There is always exactly one row with [id] = 1.
 *
 * Trust level columns store a serialised [com.smpcode253.kinesis.domain.models.PolicyMode.key]
 * for each action domain.  Defaults to `SUGGEST_ONLY` so that the assistant
 * never acts autonomously until the user explicitly grants permission.
 *
 * [trustedContacts] is persisted as a JSON array by [com.smpcode253.kinesis.data.db.Converters].
 */
@Entity(tableName = "preferences")
data class Preferences(
    @PrimaryKey val id: Long = 1,
    val trustComms: String = "SUGGEST_ONLY",
    val trustNav: String = "SUGGEST_ONLY",
    val trustTasks: String = "SUGGEST_ONLY",
    /** Reserved for a future finance/payments intent domain (e.g. send money, check balance). */
    val trustFinance: String = "SUGGEST_ONLY",
    /** Contact names (or IDs) that are always treated as trusted. */
    val trustedContacts: List<String> = emptyList(),
    /** JSON object describing quiet-hours window, e.g. `{"start":"22:00","end":"07:00"}`. */
    val quietHoursJson: String = "{}",
    /** JSON object with driving-specific overrides, e.g. `{"autoAnswerCalls":true}`. */
    val drivingSettingsJson: String = "{}"
)
