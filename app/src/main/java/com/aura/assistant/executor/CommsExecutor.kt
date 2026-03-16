package com.aura.assistant.executor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.aura.assistant.data.db.entities.TrustedContact
import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.model.UserContext
import com.aura.assistant.domain.repository.ContactRepository

/**
 * Executor responsible for communications: phone calls and SMS messages.
 * Respects trust levels to prevent unauthorized communications on behalf of the user.
 */
class CommsExecutor(
    private val context: Context,
    private val contactRepository: ContactRepository
) {

    /**
     * Initiates a phone call to the resolved contact or number.
     * Requires [Manifest.permission.CALL_PHONE].
     */
    suspend fun makeCall(intent: AuraIntent.MakeCall, userContext: UserContext): ExecutorResult {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            return ExecutorResult.RequiresPermission(Manifest.permission.CALL_PHONE)
        }

        val number = resolvePhoneNumber(intent.phoneNumber, intent.contactName)
            ?: return ExecutorResult.Failure("Could not find a phone number for '${intent.contactName}'.")

        // When driving, confirm before calling
        if (userContext == UserContext.DRIVING) {
            return ExecutorResult.NeedsConfirmation(
                prompt = "Call $number while driving?",
                action = { dialNumber(number) }
            )
        }

        dialNumber(number)
        return ExecutorResult.Success("Calling $number.")
    }

    /**
     * Sends an SMS message to the resolved contact or number.
     * Requires [Manifest.permission.SEND_SMS].
     * Only sends to trusted contacts (trust level MEDIUM or higher).
     */
    suspend fun sendMessage(intent: AuraIntent.SendMessage, userContext: UserContext): ExecutorResult {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return ExecutorResult.RequiresPermission(Manifest.permission.SEND_SMS)
        }

        if (intent.message.isBlank()) {
            return ExecutorResult.Failure("Message content cannot be empty.")
        }

        val number = resolvePhoneNumber(intent.phoneNumber, intent.contactName)
            ?: return ExecutorResult.Failure("Could not find a phone number for '${intent.contactName}'.")

        // Verify trust level for the contact
        val contact = findTrustedContact(intent.contactName, number)
        if (contact != null && contact.trustLevel < com.aura.assistant.data.db.entities.TrustLevel.MEDIUM) {
            return ExecutorResult.Failure(
                "Cannot send message to ${contact.name}: trust level is too low."
            )
        }

        val sendAction = {
            try {
                @Suppress("DEPRECATION")
                val smsManager: SmsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(intent.message)
                smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS", e)
            }
        }

        // When driving, confirm before sending
        if (userContext == UserContext.DRIVING) {
            return ExecutorResult.NeedsConfirmation(
                prompt = "Send message to $number?",
                action = sendAction
            )
        }

        sendAction()
        return ExecutorResult.Success("Message sent to $number.")
    }

    private fun dialNumber(number: String) {
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(callIntent)
    }

    private suspend fun resolvePhoneNumber(phoneNumber: String?, contactName: String?): String? {
        if (!phoneNumber.isNullOrBlank()) return phoneNumber
        if (!contactName.isNullOrBlank()) {
            val matches = contactRepository.searchContacts(contactName)
            val match = matches.firstOrNull()
            if (match != null) return match.phoneNumber
        }
        return null
    }

    private suspend fun findTrustedContact(contactName: String?, phoneNumber: String): TrustedContact? {
        if (!contactName.isNullOrBlank()) {
            val matches = contactRepository.searchContacts(contactName)
            return matches.firstOrNull()
        }
        return contactRepository.searchContacts(phoneNumber).firstOrNull()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "CommsExecutor"
    }
}
