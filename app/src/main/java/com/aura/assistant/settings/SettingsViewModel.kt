package com.aura.assistant.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.aura.assistant.AuraApplication
import com.aura.assistant.data.db.entities.TrustedContact
import com.aura.assistant.data.db.entities.TrustLevel
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen, managing trusted contacts.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AuraApplication).contactRepository

    val contacts: LiveData<List<TrustedContact>> = repository.allContacts

    fun addContact(name: String, phone: String, trustLevel: Int = TrustLevel.MEDIUM) {
        viewModelScope.launch {
            repository.insertContact(
                TrustedContact(name = name, phoneNumber = phone, trustLevel = trustLevel)
            )
        }
    }

    fun updateContact(contact: TrustedContact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    fun deleteContact(contact: TrustedContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun updateTrustLevel(contact: TrustedContact, newLevel: Int) {
        viewModelScope.launch {
            repository.updateTrustLevel(contact.id, TrustLevel.fromInt(newLevel))
        }
    }
}
