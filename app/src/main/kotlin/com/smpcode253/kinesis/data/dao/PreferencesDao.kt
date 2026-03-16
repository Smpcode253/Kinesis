package com.smpcode253.kinesis.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smpcode253.kinesis.data.models.Preferences
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {

    /**
     * Inserts the default [Preferences] row on first launch.
     * [OnConflictStrategy.IGNORE] ensures subsequent calls are no-ops.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(prefs: Preferences)

    @Update
    suspend fun update(prefs: Preferences)

    /** Always returns the single preferences row (id = 1). */
    @Query("SELECT * FROM preferences WHERE id = 1")
    suspend fun get(): Preferences?

    /** Observe preferences changes reactively. */
    @Query("SELECT * FROM preferences WHERE id = 1")
    fun observe(): Flow<Preferences?>
}
