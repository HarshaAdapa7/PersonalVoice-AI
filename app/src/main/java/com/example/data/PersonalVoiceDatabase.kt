package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AuditDao
import com.example.data.dao.ConsentDao
import com.example.data.dao.DeletionJobDao
import com.example.data.dao.UserDao
import com.example.data.dao.VoiceProfileDao
import com.example.data.model.AuditEventEntity
import com.example.data.model.ConsentRecordEntity
import com.example.data.model.DeletionJobEntity
import com.example.data.model.UserEntity
import com.example.data.model.VoiceProfileEntity

@Database(
    entities = [
        UserEntity::class,
        ConsentRecordEntity::class,
        VoiceProfileEntity::class,
        AuditEventEntity::class,
        DeletionJobEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PersonalVoiceDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun voiceProfileDao(): VoiceProfileDao
    abstract fun consentDao(): ConsentDao
    abstract fun auditDao(): AuditDao
    abstract fun deletionJobDao(): DeletionJobDao

    companion object {
        @Volatile
        private var INSTANCE: PersonalVoiceDatabase? = null

        fun getInstance(context: Context): PersonalVoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PersonalVoiceDatabase::class.java,
                    "personal_voice.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
