package com.aura.assistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aura.assistant.data.db.dao.ConversationHistoryDao
import com.aura.assistant.data.db.dao.ReminderDao
import com.aura.assistant.data.db.dao.TrustedContactDao
import com.aura.assistant.data.db.entities.ConversationEntry
import com.aura.assistant.data.db.entities.Reminder
import com.aura.assistant.data.db.entities.TrustedContact

/**
 * The main Room database for Aura. All data is stored locally on the device.
 */
@Database(
    entities = [
        Reminder::class,
        TrustedContact::class,
        ConversationEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun conversationHistoryDao(): ConversationHistoryDao

    companion object {
        private const val DATABASE_NAME = "aura_local_db"

        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
