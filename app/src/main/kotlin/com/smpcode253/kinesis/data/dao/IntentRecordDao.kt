package com.smpcode253.kinesis.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smpcode253.kinesis.data.models.IntentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: IntentRecord): Long

    @Query("SELECT * FROM intent_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<IntentRecord>>

    @Query("SELECT * FROM intent_records WHERE id = :id")
    suspend fun getById(id: Long): IntentRecord?

    @Query("SELECT * FROM intent_records WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: Long): List<IntentRecord>

    @Query("SELECT * FROM intent_records WHERE intentType = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<IntentRecord>>

    @Query("DELETE FROM intent_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
