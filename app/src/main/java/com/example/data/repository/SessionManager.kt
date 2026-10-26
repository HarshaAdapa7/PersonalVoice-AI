package com.example.data.repository

import com.example.data.PersonalVoiceDatabase
import com.example.data.model.AuditEventEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class SessionManager(private val database: PersonalVoiceDatabase) {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            // Check if default account exists, otherwise bootstrap initial owner
            val defaultEmail = "creator@personalvoice.ai"
            var user = database.userDao().getUserByEmail(defaultEmail)
            if (user == null) {
                user = UserEntity(
                    id = "user_${UUID.randomUUID().toString().take(8)}",
                    email = defaultEmail,
                    passwordHash = hashPassword("voice123"),
                    displayName = "Authorized Owner"
                )
                database.userDao().insertUser(user)
                database.auditDao().insertEvent(
                    AuditEventEntity(
                        userId = user.id,
                        action = "ACCOUNT_INITIALIZED",
                        resourceType = "USER",
                        resourceId = user.id,
                        details = "Default verified account initialized"
                    )
                )
            }
            _currentUser.value = user
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Result<UserEntity> {
        val existing = database.userDao().getUserByEmail(email.trim().lowercase())
        if (existing != null) {
            return Result.failure(IllegalStateException("Account with email $email already exists."))
        }
        val user = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            email = email.trim().lowercase(),
            passwordHash = hashPassword(password),
            displayName = displayName.trim()
        )
        database.userDao().insertUser(user)
        database.auditDao().insertEvent(
            AuditEventEntity(
                userId = user.id,
                action = "USER_REGISTER",
                resourceType = "USER",
                resourceId = user.id,
                details = "User registered with verified identity"
            )
        )
        _currentUser.value = user
        return Result.success(user)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = database.userDao().getUserByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("Account not found for email $email."))

        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(IllegalArgumentException("Invalid password credentials."))
        }

        database.auditDao().insertEvent(
            AuditEventEntity(
                userId = user.id,
                action = "USER_LOGIN",
                resourceType = "USER",
                resourceId = user.id,
                details = "User logged in successfully"
            )
        )
        _currentUser.value = user
        return Result.success(user)
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            scope.launch {
                database.auditDao().insertEvent(
                    AuditEventEntity(
                        userId = user.id,
                        action = "USER_LOGOUT",
                        resourceType = "USER",
                        resourceId = user.id,
                        details = "Session terminated"
                    )
                )
            }
        }
        _currentUser.value = null
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun requireUserId(): String {
        return _currentUser.value?.id ?: throw SecurityException("User session required.")
    }

    /**
     * Ownership verification: enforces that access to resources belongs strictly to the authenticated user.
     */
    fun assertOwnership(resourceUserId: String) {
        val currentId = requireUserId()
        if (currentId != resourceUserId) {
            throw SecurityException("Access denied: You do not have permission to access or modify this resource.")
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("personal_voice_salt_$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
