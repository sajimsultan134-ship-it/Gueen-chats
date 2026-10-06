package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.auth.AuthUser
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirestoreUserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "+1 555-0199",
    val statusBio: String = "Hey there! I am using Gueen Chats.",
    val avatarUrl: String? = null,
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = 0L,
    val lastSeenText: String = "online"
)

interface UserRepository {
    val currentUser: Flow<User?>
    val allContacts: Flow<List<User>>
    suspend fun syncUserProfile(authUser: AuthUser): Result<User>
    suspend fun updateUserProfile(name: String, statusBio: String, phoneNumber: String, avatarUrl: String?): Result<Unit>
    suspend fun updateUserStatus(isOnline: Boolean): Result<Unit>
    fun startListeningToContacts()
    fun stopListeningToContacts()
}

class FirestoreUserRepository(
    private val context: Context,
    private val database: AppDatabase
) : UserRepository {

    private val userDao = database.userDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var contactsListener: ListenerRegistration? = null
    private var currentUserListener: ListenerRegistration? = null

    override val currentUser: Flow<User?> = userDao.getCurrentUser()
    override val allContacts: Flow<List<User>> = userDao.getAllUsers()

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun syncUserProfile(authUser: AuthUser): Result<User> {
        val firestore = getFirestore()
        val userId = authUser.uid
        val name = authUser.displayName ?: authUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Sajim User"
        val email = authUser.email ?: ""

        val initialUser = User(
            id = userId,
            name = name,
            email = email,
            phoneNumber = authUser.phoneNumber ?: "+1 555-0134",
            statusBio = "Building Sajim Chat on Gueen Chats 🚀",
            avatarUrl = authUser.photoUrl,
            isOnline = true,
            lastSeenText = "online",
            isCurrentUser = true
        )

        // Save immediately to Room and ensure this user is marked as current user
        userDao.clearOtherCurrentUsers(userId)
        userDao.insertUser(initialUser)

        if (firestore != null) {
            try {
                val docRef = firestore.collection("users").document(userId)
                val snapshot = docRef.get().await()

                if (snapshot.exists()) {
                    val dto = snapshot.toObject(FirestoreUserDto::class.java)
                    if (dto != null) {
                        val syncedUser = initialUser.copy(
                            name = dto.name.ifEmpty { name },
                            email = dto.email.ifEmpty { email },
                            phoneNumber = dto.phoneNumber,
                            statusBio = dto.statusBio,
                            avatarUrl = dto.avatarUrl ?: initialUser.avatarUrl,
                            isOnline = true,
                            lastSeenText = "online"
                        )
                        userDao.insertUser(syncedUser)
                        attachCurrentUserListener(userId)
                        return Result.success(syncedUser)
                    }
                } else {
                    // Create new user profile document in Firestore
                    val dto = FirestoreUserDto(
                        id = userId,
                        name = name,
                        email = email,
                        phoneNumber = initialUser.phoneNumber,
                        statusBio = initialUser.statusBio,
                        avatarUrl = initialUser.avatarUrl,
                        isOnline = true,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        lastSeenText = "online"
                    )
                    docRef.set(dto).await()
                    attachCurrentUserListener(userId)
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "Firestore profile sync fallback to local Room: ${e.message}")
            }
        }

        return Result.success(initialUser)
    }

    private fun attachCurrentUserListener(userId: String) {
        currentUserListener?.remove()
        val firestore = getFirestore() ?: return
        currentUserListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val dto = snapshot.toObject(FirestoreUserDto::class.java) ?: return@addSnapshotListener
                scope.launch {
                    val current = userDao.getUserById(userId)
                    val updated = (current ?: User(id = userId, name = dto.name, isCurrentUser = true)).copy(
                        name = dto.name,
                        email = dto.email.ifEmpty { current?.email ?: "" },
                        phoneNumber = dto.phoneNumber,
                        statusBio = dto.statusBio,
                        avatarUrl = dto.avatarUrl,
                        isOnline = dto.isOnline,
                        lastSeenText = dto.lastSeenText
                    )
                    userDao.insertUser(updated)
                }
            }
    }

    override suspend fun updateUserProfile(
        name: String,
        statusBio: String,
        phoneNumber: String,
        avatarUrl: String?
    ): Result<Unit> {
        val current = userDao.getCurrentUserSync() ?: return Result.failure(IllegalStateException("No current user"))
        val updated = current.copy(
            name = name,
            statusBio = statusBio,
            phoneNumber = phoneNumber,
            avatarUrl = avatarUrl ?: current.avatarUrl
        )
        userDao.insertUser(updated)

        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val updates = mapOf(
                    "name" to name,
                    "statusBio" to statusBio,
                    "phoneNumber" to phoneNumber,
                    "avatarUrl" to updated.avatarUrl
                )
                firestore.collection("users").document(current.id)
                    .set(updates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to update profile in Firestore: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateUserStatus(isOnline: Boolean): Result<Unit> {
        val current = userDao.getCurrentUserSync() ?: return Result.success(Unit)
        val now = System.currentTimeMillis()
        val lastSeenText = if (isOnline) "online" else {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "last seen today at ${sdf.format(Date(now))}"
        }

        val updated = current.copy(
            isOnline = isOnline,
            lastSeenText = lastSeenText
        )
        userDao.insertUser(updated)

        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val statusUpdates = mapOf(
                    "isOnline" to isOnline,
                    "lastSeenTimestamp" to now,
                    "lastSeenText" to lastSeenText
                )
                firestore.collection("users").document(current.id)
                    .set(statusUpdates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to update presence in Firestore: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    override fun startListeningToContacts() {
        val firestore = getFirestore() ?: return
        if (contactsListener != null) return

        contactsListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    val currentUserId = userDao.getCurrentUserSync()?.id
                    val remoteContacts = snapshot.documents.mapNotNull { doc ->
                        val dto = doc.toObject(FirestoreUserDto::class.java) ?: return@mapNotNull null
                        if (dto.id == currentUserId) return@mapNotNull null // Don't overwrite current user flag
                        User(
                            id = dto.id.ifEmpty { doc.id },
                            name = dto.name,
                            phoneNumber = dto.phoneNumber,
                            statusBio = dto.statusBio,
                            avatarUrl = dto.avatarUrl,
                            isOnline = dto.isOnline,
                            lastSeenText = dto.lastSeenText,
                            isCurrentUser = false
                        )
                    }
                    if (remoteContacts.isNotEmpty()) {
                        userDao.insertUsers(remoteContacts)
                    }
                }
            }
    }

    override fun stopListeningToContacts() {
        contactsListener?.remove()
        contactsListener = null
        currentUserListener?.remove()
        currentUserListener = null
    }
}
