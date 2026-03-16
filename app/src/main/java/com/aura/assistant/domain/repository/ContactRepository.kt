package com.aura.assistant.domain.repository

import androidx.lifecycle.LiveData
import com.aura.assistant.data.db.dao.TrustedContactDao
import com.aura.assistant.data.db.entities.TrustedContact

/**
 * Repository for managing trusted contacts. Acts as a single source of truth
 * between the database and the rest of the application.
 */
class ContactRepository(private val dao: TrustedContactDao) {

    val allContacts: LiveData<List<TrustedContact>> = dao.getAllContacts()

    suspend fun getAllContactsSync(): List<TrustedContact> = dao.getAllContactsSync()

    suspend fun getContactById(id: Long): TrustedContact? = dao.getContactById(id)

    suspend fun searchContacts(query: String): List<TrustedContact> = dao.searchContacts(query)

    suspend fun insertContact(contact: TrustedContact): Long = dao.insertContact(contact)

    suspend fun updateContact(contact: TrustedContact) = dao.updateContact(contact)

    suspend fun deleteContact(contact: TrustedContact) = dao.deleteContact(contact)

    suspend fun updateTrustLevel(id: Long, level: Int) = dao.updateTrustLevel(id, level)
}
