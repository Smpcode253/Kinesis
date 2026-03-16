package com.aura.assistant.data.db.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Represents a trusted contact stored locally on the device.
 * Trust level controls what actions Aura can take on behalf of the user with this contact.
 */
@Parcelize
@Entity(tableName = "trusted_contacts")
data class TrustedContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    /** Trust level: 0 = none, 1 = low, 2 = medium, 3 = high */
    val trustLevel: Int = TrustLevel.MEDIUM,
    val createdAtMs: Long = System.currentTimeMillis()
)

object TrustLevel {
    const val NONE = 0
    const val LOW = 1
    const val MEDIUM = 2
    const val HIGH = 3

    fun fromInt(value: Int): Int = when (value) {
        NONE, LOW, MEDIUM, HIGH -> value
        else -> MEDIUM
    }

    fun toLabel(value: Int): String = when (value) {
        NONE -> "None"
        LOW -> "Low"
        MEDIUM -> "Medium"
        HIGH -> "High"
        else -> "Medium"
    }
}
