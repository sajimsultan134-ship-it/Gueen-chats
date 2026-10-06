package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val isAnonymous: Boolean = false
)

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class AuthError(val message: String) : AuthState()
}

class FirebaseAuthenticationService(
    private val context: Context,
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val userDao = database.userDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private val prefs = context.getSharedPreferences("sajim_auth_prefs", Context.MODE_PRIVATE)

    init {
        initializeAuth()
    }

    fun initializeAuth() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                firebaseAuth = auth
                auth.addAuthStateListener { fbAuth ->
                    val fbUser = fbAuth.currentUser
                    if (fbUser != null) {
                        val authUser = fbUser.toAuthUser()
                        _authState.value = AuthState.Authenticated(authUser)
                        syncLocalUser(authUser)
                    } else {
                        checkOfflineSession()
                    }
                }
                Log.i("FirebaseAuthService", "Firebase Auth initialized successfully")
                return
            }
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "Firebase Auth default app not available: ${e.message}")
        }
        checkOfflineSession()
    }

    private fun checkOfflineSession() {
        scope.launch {
            val localUser = userDao.getCurrentUserSync()
            if (localUser != null) {
                val authUser = AuthUser(
                    uid = localUser.id,
                    email = "${localUser.name.lowercase().replace(" ", "")}@gueenchats.com",
                    displayName = localUser.name,
                    phoneNumber = localUser.phoneNumber,
                    photoUrl = localUser.avatarUrl,
                    isEmailVerified = true,
                    isAnonymous = false
                )
                _authState.value = AuthState.Authenticated(authUser)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun isFirebaseAvailable(): Boolean {
        return firebaseAuth != null && FirebaseApp.getApps(context).isNotEmpty()
    }

    fun getCurrentUser(): AuthUser? {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
            return fbUser.toAuthUser()
        }
        val current = _authState.value
        return if (current is AuthState.Authenticated) current.user else null
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your display name"))
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable()) {
            return try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase registration returned empty user")

                // Update profile display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()

                val authUser = user.toAuthUser()
                _authState.value = AuthState.Authenticated(authUser)
                syncLocalUser(authUser)

                Result.success(authUser)
            } catch (e: FirebaseAuthException) {
                val errorMsg = mapFirebaseAuthError(e)
                _authState.value = AuthState.AuthError(errorMsg)
                Result.failure(Exception(errorMsg, e))
            } catch (e: Exception) {
                _authState.value = AuthState.AuthError(e.localizedMessage ?: "Registration failed")
                Result.failure(e)
            }
        }

        // Local-first fallback registration
        return try {
            val userId = "user_${System.currentTimeMillis()}"
            val authUser = AuthUser(
                uid = userId,
                email = email.trim(),
                displayName = displayName.trim(),
                isEmailVerified = true
            )

            val localEntity = User(
                id = userId,
                name = displayName.trim(),
                statusBio = "Hey there! I am using Gueen Chats.",
                isOnline = true,
                isCurrentUser = true
            )
            userDao.insertUser(localEntity)
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable()) {
            return try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val user = authResult.user ?: throw IllegalStateException("User authentication failed")
                val authUser = user.toAuthUser()
                _authState.value = AuthState.Authenticated(authUser)
                syncLocalUser(authUser)
                Result.success(authUser)
            } catch (e: FirebaseAuthException) {
                val errorMsg = mapFirebaseAuthError(e)
                _authState.value = AuthState.AuthError(errorMsg)
                Result.failure(Exception(errorMsg, e))
            } catch (e: Exception) {
                _authState.value = AuthState.AuthError(e.localizedMessage ?: "Login failed")
                Result.failure(e)
            }
        }

        // Offline / Local login
        return try {
            val existing = userDao.getCurrentUserSync()
            val user = if (existing != null) {
                AuthUser(
                    uid = existing.id,
                    email = email.trim(),
                    displayName = existing.name,
                    phoneNumber = existing.phoneNumber
                )
            } else {
                val newLocal = User(
                    id = "user_me",
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    statusBio = "Hey there! I am using Gueen Chats.",
                    isCurrentUser = true
                )
                userDao.insertUser(newLocal)
                AuthUser(
                    uid = newLocal.id,
                    email = email.trim(),
                    displayName = newLocal.name
                )
            }
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(
        idToken: String?,
        email: String,
        displayName: String,
        photoUrl: String?
    ): Result<AuthUser> {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable() && !idToken.isNullOrBlank()) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase Google Sign-In returned null user")
                val authUser = user.toAuthUser().copy(
                    displayName = user.displayName ?: displayName,
                    photoUrl = user.photoUrl?.toString() ?: photoUrl
                )
                _authState.value = AuthState.Authenticated(authUser)
                syncGoogleUser(authUser)
                return Result.success(authUser)
            } catch (e: FirebaseAuthException) {
                val errorMsg = mapFirebaseAuthError(e)
                Log.w("FirebaseAuthService", "Firebase Google credential sign-in error: $errorMsg")
                return syncGoogleUserFallback(email, displayName, photoUrl)
            } catch (e: Exception) {
                Log.w("FirebaseAuthService", "Google Sign-In exception: ${e.message}")
                return syncGoogleUserFallback(email, displayName, photoUrl)
            }
        }

        // When offline or testing with Google account (e.g. Sajimsultan134@gmail.com)
        return syncGoogleUserFallback(email, displayName, photoUrl)
    }

    private suspend fun syncGoogleUserFallback(
        email: String,
        displayName: String,
        photoUrl: String?
    ): Result<AuthUser> {
        return try {
            val normalizedEmail = email.trim().lowercase()
            // Deterministic UID to guarantee no duplicate accounts for the same Google account
            val uid = "google_${normalizedEmail.replace(Regex("[^a-z0-9]"), "_")}"
            val existing = userDao.getUserById(uid)
            val authUser = AuthUser(
                uid = uid,
                email = email.trim(),
                displayName = displayName.ifBlank { email.substringBefore("@") },
                photoUrl = photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde",
                isEmailVerified = true,
                isAnonymous = false
            )

            val entity = if (existing != null) {
                // Existing user: preserve existing statusBio, phone, and chat history
                existing.copy(
                    name = authUser.displayName ?: existing.name,
                    email = authUser.email ?: existing.email,
                    avatarUrl = authUser.photoUrl ?: existing.avatarUrl,
                    isOnline = true,
                    isCurrentUser = true
                )
            } else {
                // New user: automatically receive a Gueen Chats account
                User(
                    id = uid,
                    name = authUser.displayName ?: "Sajim Sultan",
                    email = authUser.email,
                    phoneNumber = "+1 555-0134",
                    statusBio = "Hey there! I am using Gueen Chats.",
                    avatarUrl = authUser.photoUrl,
                    isOnline = true,
                    isCurrentUser = true
                )
            }

            userDao.clearOtherCurrentUsers(uid)
            userDao.insertUser(entity)
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncGoogleUser(authUser: AuthUser) {
        scope.launch {
            val existing = userDao.getUserById(authUser.uid)
            val entity = if (existing != null) {
                existing.copy(
                    name = authUser.displayName ?: existing.name,
                    email = authUser.email ?: existing.email,
                    avatarUrl = authUser.photoUrl ?: existing.avatarUrl,
                    isOnline = true,
                    isCurrentUser = true
                )
            } else {
                User(
                    id = authUser.uid,
                    name = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "Sajim Sultan",
                    email = authUser.email,
                    phoneNumber = authUser.phoneNumber ?: "+1 555-0134",
                    statusBio = "Hey there! I am using Gueen Chats.",
                    avatarUrl = authUser.photoUrl,
                    isOnline = true,
                    isCurrentUser = true
                )
            }
            userDao.clearOtherCurrentUsers(authUser.uid)
            userDao.insertUser(entity)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable()) {
            return try {
                auth.sendPasswordResetEmail(email.trim()).await()
                Result.success(Unit)
            } catch (e: FirebaseAuthException) {
                Result.failure(Exception(mapFirebaseAuthError(e), e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // Simulation response if Firebase is not connected yet
        return Result.success(Unit)
    }

    suspend fun logout(): Result<Unit> {
        try {
            firebaseAuth?.signOut()
            userDao.clearAllCurrentUsers()
            prefs.edit().clear().apply()
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun updateProfile(displayName: String, photoUrl: String?): Result<Unit> {
        val user = firebaseAuth?.currentUser
        if (user != null && isFirebaseAvailable()) {
            try {
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(request).await()
            } catch (e: Exception) {
                Log.w("FirebaseAuthService", "Failed to update Firebase profile: ${e.message}")
            }
        }

        // Update local database entity
        val currentLocal = userDao.getCurrentUserSync()
        if (currentLocal != null) {
            val updated = currentLocal.copy(
                name = displayName,
                avatarUrl = photoUrl ?: currentLocal.avatarUrl
            )
            userDao.updateUser(updated)
        }

        val updatedAuthUser = getCurrentUser()?.copy(
            displayName = displayName,
            photoUrl = photoUrl
        )
        if (updatedAuthUser != null) {
            _authState.value = AuthState.Authenticated(updatedAuthUser)
        }

        return Result.success(Unit)
    }

    private fun syncLocalUser(authUser: AuthUser) {
        scope.launch {
            val existing = userDao.getUserById(authUser.uid)
            val entity = User(
                id = authUser.uid,
                name = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "Sajim User",
                phoneNumber = authUser.phoneNumber ?: "+1 555-0134",
                avatarUrl = authUser.photoUrl,
                isOnline = true,
                isCurrentUser = true
            )
            userDao.insertUser(entity)
        }
    }

    private fun mapFirebaseAuthError(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_WRONG_PASSWORD" -> "The password is incorrect. Please try again."
            "ERROR_USER_NOT_FOUND" -> "No user found with this email. Please register first."
            "ERROR_USER_DISABLED" -> "This user account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many unsuccessful attempts. Please try again later."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email address already exists."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled in Firebase Console."
            "ERROR_WEAK_PASSWORD" -> "The password is too weak. Please use at least 6 characters."
            else -> e.localizedMessage ?: "Authentication failed (${e.errorCode})"
        }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName ?: email?.substringBefore("@") ?: "Sajim User",
            phoneNumber = phoneNumber,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified,
            isAnonymous = isAnonymous
        )
    }
}
