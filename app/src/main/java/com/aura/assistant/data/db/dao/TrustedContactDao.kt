package com.aura.assistant.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.aura.assistant.data.db.entities.TrustedContact

@Dao
interface TrustedContactDao {

    @Query("SELECT * FROM trusted_contacts ORDER BY name ASC")
    fun getAllContacts(): LiveData<List<TrustedContact>>

    @Query("SELECT * FROM trusted_contacts ORDER BY name ASC")
    suspend fun getAllContactsSync(): List<TrustedContact>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): TrustedContact?

    @Query("SELECT * FROM trusted_contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    suspend fun searchContacts(query: String): List<TrustedContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContact): Long

    @Update
    suspend fun updateContact(contact: TrustedContact)

    @Delete
    suspend fun deleteContact(contact: TrustedContact)

    @Query("UPDATE trusted_contacts SET trustLevel = :level WHERE id = :id")
    suspend fun updateTrustLevel(id: Long, level: Int)
}
