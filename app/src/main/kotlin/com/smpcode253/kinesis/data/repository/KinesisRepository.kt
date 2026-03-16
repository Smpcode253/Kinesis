package com.smpcode253.kinesis.data.repository

import com.smpcode253.kinesis.data.dao.ActionRecordDao
import com.smpcode253.kinesis.data.dao.EventDao
import com.smpcode253.kinesis.data.dao.IntentRecordDao
import com.smpcode253.kinesis.data.dao.PreferencesDao
import com.smpcode253.kinesis.data.models.ActionRecord
import com.smpcode253.kinesis.data.models.Event
import com.smpcode253.kinesis.data.models.IntentRecord
import com.smpcode253.kinesis.data.models.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Single entry-point for all persistence operations.
 *
 * Upper layers (use-cases, ViewModels) depend on this class rather than
 * directly on DAOs, making it easy to substitute a fake in tests.
 */
class KinesisRepository(
    private val eventDao: EventDao,
    private val intentRecordDao: IntentRecordDao,
    private val actionRecordDao: ActionRecordDao,
    private val preferencesDao: PreferencesDao
) {
    // ── Events ──────────────────────────────────────────────────────────────

    suspend fun saveEvent(event: Event): Long = eventDao.insert(event)

    fun observeEvents(): Flow<List<Event>> = eventDao.observeAll()

    suspend fun getEvent(id: Long): Event? = eventDao.getById(id)

    // ── Intent records ───────────────────────────────────────────────────────

    suspend fun saveIntentRecord(record: IntentRecord): Long = intentRecordDao.insert(record)

    fun observeIntentRecords(): Flow<List<IntentRecord>> = intentRecordDao.observeAll()

    suspend fun getIntentRecord(id: Long): IntentRecord? = intentRecordDao.getById(id)

    suspend fun getIntentRecordsForEvent(eventId: Long): List<IntentRecord> =
        intentRecordDao.getByEventId(eventId)

    // ── Action records ───────────────────────────────────────────────────────

    suspend fun saveActionRecord(record: ActionRecord): Long = actionRecordDao.insert(record)

    suspend fun updateActionRecord(record: ActionRecord) = actionRecordDao.update(record)

    suspend fun updateActionStatus(id: Long, status: String) =
        actionRecordDao.updateStatus(id, status)

    fun observeActionRecords(): Flow<List<ActionRecord>> = actionRecordDao.observeAll()

    fun observePendingActions(): Flow<List<ActionRecord>> =
        actionRecordDao.observeByStatus("PENDING")

    suspend fun getActionsForIntent(intentId: Long): List<ActionRecord> =
        actionRecordDao.getByIntentId(intentId)

    // ── Preferences ──────────────────────────────────────────────────────────

    /**
     * Returns the current [Preferences].  Falls back to an in-memory default
     * if the row has not yet been seeded (e.g. before [initPreferences] runs).
     * Avoids performing a write inside a read operation, eliminating potential
     * race conditions when called concurrently.
     */
    suspend fun getPreferences(): Preferences = preferencesDao.get() ?: Preferences()

    suspend fun savePreferences(prefs: Preferences) = preferencesDao.update(prefs)

    fun observePreferences(): Flow<Preferences?> = preferencesDao.observe()

    /** Ensures the default preferences row exists.  Call once on app start. */
    suspend fun initPreferences() = preferencesDao.insertDefaults(Preferences())
}
