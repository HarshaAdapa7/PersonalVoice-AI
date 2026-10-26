package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "consent_records")
data class ConsentRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val statementText: String,
    val audioSamplePath: String? = null,
    val consentedAt: Long = System.currentTimeMillis(),
    val verified: Boolean = true
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val displayName: String,
    val engineId: String,
    val engineModelVersion: String = "v1",
    val engineProfileRef: String? = null,
    val voiceName: String = "Puck",
    val status: String = "active", // "pending", "processing", "active", "failed", "deletion_pending", "deleted"
    val consentRecordId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val action: String, // "USER_REGISTER", "USER_LOGIN", "PROFILE_ENROLL", "PROFILE_DELETE_ENQUEUED", "AUDIO_PURGED", etc.
    val resourceType: String,
    val resourceId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "deletion_jobs")
data class DeletionJobEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val resourceType: String, // "VOICE_PROFILE", "AUDIO_ARTIFACT", "CONSENT_RECORD"
    val resourceId: String,
    val filePath: String? = null,
    val status: String = "enqueued", // "enqueued", "processing", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
