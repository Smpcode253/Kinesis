package com.smpcode253.kinesis.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smpcode253.kinesis.data.models.ActionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActionRecord): Long

    @Update
    suspend fun update(record: ActionRecord)

    @Query("SELECT * FROM action_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActionRecord>>

    @Query("SELECT * FROM action_records WHERE id = :id")
    suspend fun getById(id: Long): ActionRecord?

    @Query("SELECT * FROM action_records WHERE intentId = :intentId")
    suspend fun getByIntentId(intentId: Long): List<ActionRecord>

    @Query("SELECT * FROM action_records WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<ActionRecord>>

    @Query("UPDATE action_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM action_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
