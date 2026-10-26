package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditEventEntity
import com.example.data.model.ConsentRecordEntity
import com.example.data.model.DeletionJobEntity
import com.example.data.model.UserEntity
import com.example.data.model.VoiceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles WHERE userId = :userId AND status != 'deleted' ORDER BY createdAt DESC")
    fun getActiveProfilesForUser(userId: String): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getProfileByIdAndUser(id: String, userId: String): VoiceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfileEntity)

    @Update
    suspend fun updateProfile(profile: VoiceProfileEntity)

    @Query("UPDATE voice_profiles SET status = :status, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun updateStatus(id: String, userId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE voice_profiles SET status = 'deleted', deletedAt = :deletedAt WHERE id = :id AND userId = :userId")
    suspend fun softDeleteProfile(id: String, userId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun purgeProfile(id: String)
}

@Dao
interface ConsentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsent(consent: ConsentRecordEntity)

    @Query("SELECT * FROM consent_records WHERE id = :id LIMIT 1")
    suspend fun getConsentById(id: String): ConsentRecordEntity?

    @Query("SELECT * FROM consent_records WHERE userId = :userId ORDER BY consentedAt DESC")
    fun getConsentsForUser(userId: String): Flow<List<ConsentRecordEntity>>

    @Query("DELETE FROM consent_records WHERE id = :id")
    suspend fun purgeConsent(id: String)
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAuditEventsForUser(userId: String): Flow<List<AuditEventEntity>>
}

@Dao
interface DeletionJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DeletionJobEntity)

    @Query("SELECT * FROM deletion_jobs WHERE status = 'enqueued' ORDER BY createdAt ASC")
    suspend fun getPendingJobs(): List<DeletionJobEntity>

    @Query("SELECT * FROM deletion_jobs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getJobsForUser(userId: String): Flow<List<DeletionJobEntity>>

    @Query("UPDATE deletion_jobs SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateJobStatus(id: String, status: String, completedAt: Long? = System.currentTimeMillis())
}
