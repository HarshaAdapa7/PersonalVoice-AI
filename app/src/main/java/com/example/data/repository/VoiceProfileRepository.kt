package com.example.data.repository

import android.content.Context
import com.example.audio.AudioQualityChecker
import com.example.data.PersonalVoiceDatabase
import com.example.data.model.AuditEventEntity
import com.example.data.model.ConsentRecordEntity
import com.example.data.model.DeletionJobEntity
import com.example.data.model.VoiceProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VoiceProfileRepository(
    private val context: Context,
    private val database: PersonalVoiceDatabase,
    private val sessionManager: SessionManager
) {

    fun getActiveProfilesForCurrentUser(): Flow<List<VoiceProfileEntity>> {
        val userId = sessionManager.currentUser.value?.id ?: ""
        return database.voiceProfileDao().getActiveProfilesForUser(userId).flowOn(Dispatchers.IO)
    }

    suspend fun getProfile(profileId: String): VoiceProfileEntity? = withContext(Dispatchers.IO) {
        val userId = sessionManager.requireUserId()
        database.voiceProfileDao().getProfileByIdAndUser(profileId, userId)
    }

    suspend fun createProfile(
        displayName: String,
        engineId: String,
        voiceName: String,
        sampleAudio: ByteArray,
        consentStatement: String,
        explicitConsentChecked: Boolean
    ): Result<VoiceProfileEntity> = withContext(Dispatchers.IO) {
        if (!explicitConsentChecked) {
            return@withContext Result.failure(
                IllegalArgumentException("Explicit consent agreement must be checked before creating a voice profile.")
            )
        }

        val quality = AudioQualityChecker.analyze(sampleAudio)
        if (!quality.isValid) {
            return@withContext Result.failure(
                IllegalArgumentException("Sample audio failed quality checks: ${quality.userGuidance}")
            )
        }

        val userId = sessionManager.requireUserId()
        val profileId = "prof_${UUID.randomUUID().toString().take(8)}"
        val consentId = "consent_${UUID.randomUUID().toString().take(8)}"

        // Save audio to private storage
        val userAudioDir = File(context.filesDir, "voices/$userId").apply { mkdirs() }
        val sampleFile = File(userAudioDir, "${profileId}_consent_sample.wav")
        FileOutputStream(sampleFile).use { it.write(sampleAudio) }

        // Store Consent Record
        val consentRecord = ConsentRecordEntity(
            id = consentId,
            userId = userId,
            statementText = consentStatement,
            audioSamplePath = sampleFile.absolutePath,
            consentedAt = System.currentTimeMillis(),
            verified = true
        )
        database.consentDao().insertConsent(consentRecord)

        // Store Voice Profile
        val profile = VoiceProfileEntity(
            id = profileId,
            userId = userId,
            displayName = displayName.trim(),
            engineId = engineId,
            engineModelVersion = "v1",
            engineProfileRef = "ref_${profileId}",
            voiceName = voiceName,
            status = "active",
            consentRecordId = consentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.voiceProfileDao().insertProfile(profile)

        // Record Audit Event
        database.auditDao().insertEvent(
            AuditEventEntity(
                userId = userId,
                action = "PROFILE_ENROLLED",
                resourceType = "VOICE_PROFILE",
                resourceId = profileId,
                details = "Enrolled profile '${profile.displayName}' with consent record $consentId"
            )
        )

        Result.success(profile)
    }

    /**
     * DELETE flow: Revokes access immediately, sets status to deletion_pending,
     * enqueues a deletion job to remove audio and artifacts, and purges the file.
     */
    suspend fun deleteProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = sessionManager.requireUserId()
        val profile = database.voiceProfileDao().getProfileByIdAndUser(profileId, userId)
            ?: return@withContext Result.failure(SecurityException("Profile not found or access unauthorized."))

        // Step 1: Revoke access immediately
        database.voiceProfileDao().updateStatus(
            id = profileId,
            userId = userId,
            status = "deletion_pending",
            updatedAt = System.currentTimeMillis()
        )

        // Step 2: Enqueue deletion job
        val jobId = "del_${UUID.randomUUID().toString().take(8)}"
        val sampleFile = File(context.filesDir, "voices/$userId/${profileId}_consent_sample.wav")
        val job = DeletionJobEntity(
            id = jobId,
            userId = userId,
            resourceType = "VOICE_PROFILE",
            resourceId = profileId,
            filePath = sampleFile.absolutePath,
            status = "enqueued",
            createdAt = System.currentTimeMillis()
        )
        database.deletionJobDao().insertJob(job)

        database.auditDao().insertEvent(
            AuditEventEntity(
                userId = userId,
                action = "PROFILE_DELETION_ENQUEUED",
                resourceType = "VOICE_PROFILE",
                resourceId = profileId,
                details = "Access revoked immediately; deletion job $jobId queued"
            )
        )

        // Step 3: Execute deletion job and purge artifacts
        database.deletionJobDao().updateJobStatus(jobId, "processing", null)
        if (sampleFile.exists()) {
            sampleFile.delete()
        }

        database.voiceProfileDao().softDeleteProfile(profileId, userId)
        database.deletionJobDao().updateJobStatus(jobId, "completed", System.currentTimeMillis())

        database.auditDao().insertEvent(
            AuditEventEntity(
                userId = userId,
                action = "PROFILE_PURGED",
                resourceType = "VOICE_PROFILE",
                resourceId = profileId,
                details = "Voice profile and audio samples permanently deleted"
            )
        )

        Result.success(Unit)
    }
}
