package com.smpcode253.kinesis

import android.app.Application
import com.smpcode253.kinesis.data.db.KinesisDatabase
import com.smpcode253.kinesis.data.repository.KinesisRepository
import com.smpcode253.kinesis.domain.usecases.IntentProcessor
import com.smpcode253.kinesis.domain.usecases.PolicyEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry-point.
 *
 * Wires together the database, repository, and domain use-cases so that
 * other components (Activities, Services, Workers) can access them via
 * the [KinesisApplication] instance without a full DI framework.
 */
class KinesisApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { KinesisDatabase.getInstance(this) }

    val repository by lazy {
        KinesisRepository(
            eventDao = database.eventDao(),
            intentRecordDao = database.intentRecordDao(),
            actionRecordDao = database.actionRecordDao(),
            preferencesDao = database.preferencesDao()
        )
    }

    val policyEvaluator by lazy { PolicyEvaluator() }

    val intentProcessor by lazy { IntentProcessor(repository, policyEvaluator) }

    override fun onCreate() {
        super.onCreate()
        // Seed default preferences row on first launch.
        applicationScope.launch { repository.initPreferences() }
    }
}
