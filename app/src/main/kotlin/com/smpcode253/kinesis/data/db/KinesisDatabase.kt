package com.smpcode253.kinesis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smpcode253.kinesis.data.dao.ActionRecordDao
import com.smpcode253.kinesis.data.dao.EventDao
import com.smpcode253.kinesis.data.dao.IntentRecordDao
import com.smpcode253.kinesis.data.dao.PreferencesDao
import com.smpcode253.kinesis.data.models.ActionRecord
import com.smpcode253.kinesis.data.models.Event
import com.smpcode253.kinesis.data.models.IntentRecord
import com.smpcode253.kinesis.data.models.Preferences

@Database(
    entities = [
        Event::class,
        IntentRecord::class,
        ActionRecord::class,
        Preferences::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KinesisDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun intentRecordDao(): IntentRecordDao
    abstract fun actionRecordDao(): ActionRecordDao
    abstract fun preferencesDao(): PreferencesDao

    companion object {
        private const val DATABASE_NAME = "kinesis.db"

        @Volatile
        private var instance: KinesisDatabase? = null

        fun getInstance(context: Context): KinesisDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, KinesisDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
