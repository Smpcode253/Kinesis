package com.smpcode253.kinesis.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smpcode253.kinesis.data.models.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): Event?

    @Query("SELECT * FROM events WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<Event>>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
