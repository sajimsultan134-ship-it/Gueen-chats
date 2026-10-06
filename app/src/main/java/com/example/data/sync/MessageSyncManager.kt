package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

enum class SyncState {
    OFFLINE_LOCAL,
    CONNECTING,
    CONNECTED,
    SYNCING,
    ERROR
}

data class SyncStatus(
    val state: SyncState = SyncState.OFFLINE_LOCAL,
    val isCloudConnected: Boolean = false,
    val pendingUploadsCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val statusMessage: String = "Operating in Local-First Room Persistence mode"
)

data class FirestoreMessageDto(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val status: String = "SENT",
    val type: String = "TEXT",
    val mediaUri: String? = null,
    val mediaDurationSec: Int = 0,
    val mediaFileName: String? = null,
    val mediaFileSize: String? = null
)

interface MessageSyncManager {
    val syncStatus: StateFlow<SyncStatus>
    fun startSync(chatId: String)
    fun stopSync(chatId: String)
    fun startListeningToChats()
    fun stopListeningToChats()
    suspend fun pushMessage(message: Message): Result<Unit>
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus): Result<Unit>
    suspend fun configureFirebase(projectId: String, applicationId: String, apiKey: String): Result<Unit>
    fun isFirebaseConfigured(): Boolean
}

class FirebaseMessageSyncManager(
    private val context: Context,
    private val database: AppDatabase
) : MessageSyncManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val messageDao = database.messageDao()
    private val chatDao = database.chatDao()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val activeListeners = ConcurrentHashMap<String, ListenerRegistration>()
    private var firestoreInstance: FirebaseFirestore? = null

    private val prefs = context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)

    init {
        checkAndInitializeFirebase()
    }

    private fun checkAndInitializeFirebase() {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                firestoreInstance = FirebaseFirestore.getInstance()
                _syncStatus.value = SyncStatus(
                    state = SyncState.CONNECTED,
                    isCloudConnected = true,
                    statusMessage = "Firebase Firestore connected and active"
                )
                Log.i("MessageSyncManager", "Firebase initialized from configuration")
                return
            }

            // Check if user previously saved manual Firebase credentials
            val savedProjectId = prefs.getString("project_id", null)
            val savedAppId = prefs.getString("app_id", null)
            val savedApiKey = prefs.getString("api_key", null)

            if (!savedProjectId.isNullOrBlank() && !savedAppId.isNullOrBlank() && !savedApiKey.isNullOrBlank()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(savedProjectId)
                    .setApplicationId(savedAppId)
                    .setApiKey(savedApiKey)
                    .build()
                FirebaseApp.initializeApp(context, options)
                firestoreInstance = FirebaseFirestore.getInstance()
                _syncStatus.value = SyncStatus(
                    state = SyncState.CONNECTED,
                    isCloudConnected = true,
                    statusMessage = "Connected to Firebase project: $savedProjectId"
                )
                Log.i("MessageSyncManager", "Firebase initialized with stored credentials: $savedProjectId")
            } else {
                _syncStatus.value = SyncStatus(
                    state = SyncState.OFFLINE_LOCAL,
                    isCloudConnected = false,
                    statusMessage = "Offline-first mode (Room). Add google-services.json or enter Project credentials in Settings to sync with Firebase."
                )
            }
        } catch (e: Exception) {
            Log.w("MessageSyncManager", "Firebase auto-initialization skipped: ${e.message}")
            _syncStatus.value = SyncStatus(
                state = SyncState.OFFLINE_LOCAL,
                isCloudConnected = false,
                statusMessage = "Offline-first Room database active. Firebase awaiting configuration."
            )
        }
    }

    override fun isFirebaseConfigured(): Boolean {
        return firestoreInstance != null
    }

    override fun startSync(chatId: String) {
        val firestore = firestoreInstance ?: return
        if (activeListeners.containsKey(chatId)) return

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            statusMessage = "Listening for real-time messages on chat: $chatId"
        )

        try {
            val listener = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageSyncManager", "Firestore listener error: ${error.message}")
                        _syncStatus.value = _syncStatus.value.copy(
                            state = SyncState.ERROR,
                            statusMessage = "Sync error: ${error.localizedMessage}"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    scope.launch {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED || dc.type == DocumentChange.Type.MODIFIED) {
                                val dto = dc.document.toObject(FirestoreMessageDto::class.java)
                                val currentUser = database.userDao().getCurrentUserSync()
                                val currentUserId = currentUser?.id ?: "user_me"
                                val isOutgoing = dto.senderId == currentUserId

                                val status = try {
                                    MessageStatus.valueOf(dto.status)
                                } catch (_: Exception) {
                                    MessageStatus.DELIVERED
                                }

                                val type = try {
                                    MessageType.valueOf(dto.type)
                                } catch (_: Exception) {
                                    MessageType.TEXT
                                }

                                val entity = Message(
                                    id = dto.id.ifEmpty { dc.document.id },
                                    chatId = chatId,
                                    senderId = dto.senderId,
                                    senderName = dto.senderName,
                                    text = dto.text,
                                    timestamp = if (dto.timestamp > 0) dto.timestamp else System.currentTimeMillis(),
                                    status = status,
                                    type = type,
                                    mediaUri = dto.mediaUri,
                                    mediaDurationSec = dto.mediaDurationSec,
                                    mediaFileName = dto.mediaFileName,
                                    mediaFileSize = dto.mediaFileSize,
                                    isOutgoing = isOutgoing
                                )
                                messageDao.insertMessage(entity)

                                // Update chat metadata
                                chatDao.updateLastMessage(
                                    chatId = chatId,
                                    lastMessage = entity.text,
                                    time = entity.timestamp
                                )
                            }
                        }

                        _syncStatus.value = _syncStatus.value.copy(
                            state = SyncState.CONNECTED,
                            lastSyncTimestamp = System.currentTimeMillis(),
                            statusMessage = "Real-time sync up to date"
                        )
                    }
                }

            activeListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e("MessageSyncManager", "Failed to start sync: ${e.message}")
        }
    }

    override fun stopSync(chatId: String) {
        activeListeners.remove(chatId)?.remove()
    }

    override suspend fun pushMessage(message: Message): Result<Unit> {
        val firestore = firestoreInstance
        if (firestore == null) {
            // Offline-first: saved to Room, ready when Firebase connects
            return Result.success(Unit)
        }

        return try {
            val dto = FirestoreMessageDto(
                id = message.id,
                chatId = message.chatId,
                senderId = message.senderId,
                senderName = message.senderName,
                text = message.text,
                timestamp = message.timestamp,
                status = message.status.name,
                type = message.type.name,
                mediaUri = message.mediaUri,
                mediaDurationSec = message.mediaDurationSec,
                mediaFileName = message.mediaFileName,
                mediaFileSize = message.mediaFileSize
            )

            // Write to Firestore /chats/{chatId}/messages/{messageId}
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .document(message.id)
                .set(dto)
                .await()

            // Update parent chat metadata
            val chatUpdate = mapOf(
                "lastMessage" to message.text,
                "lastMessageTime" to message.timestamp,
                "lastSenderId" to message.senderId
            )
            firestore.collection("chats")
                .document(message.chatId)
                .set(chatUpdate, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("MessageSyncManager", "Failed to push message to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateMessageStatus(
        chatId: String,
        messageId: String,
        status: MessageStatus
    ): Result<Unit> {
        val firestore = firestoreInstance ?: return Result.success(Unit)
        return try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private var chatsListener: ListenerRegistration? = null

    override fun startListeningToChats() {
        val firestore = firestoreInstance ?: return
        if (chatsListener != null) return

        try {
            chatsListener = firestore.collection("chats")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        for (doc in snapshot.documents) {
                            val id = doc.id
                            val name = doc.getString("name") ?: "Chat"
                            val isGroup = doc.getBoolean("isGroup") ?: false
                            val avatarUrl = doc.getString("avatarUrl")
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val lastMessageTime = doc.getLong("lastMessageTime") ?: System.currentTimeMillis()
                            val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                            val isOnline = doc.getBoolean("isOnline") ?: false
                            val lastSeenText = doc.getString("lastSeenText") ?: "recently"
                            val isPinned = doc.getBoolean("isPinned") ?: false
                            val participantIdsCsv = doc.getString("participantIdsCsv") ?: ""

                            val existing = chatDao.getChatByIdSync(id)
                            val chat = Chat(
                                id = id,
                                name = if (existing != null && existing.name.isNotBlank()) existing.name else name,
                                isGroup = isGroup,
                                avatarUrl = avatarUrl ?: existing?.avatarUrl,
                                lastMessage = if (lastMessage.isNotBlank()) lastMessage else (existing?.lastMessage ?: ""),
                                lastMessageTime = if (lastMessageTime > 0) lastMessageTime else (existing?.lastMessageTime ?: System.currentTimeMillis()),
                                unreadCount = unreadCount,
                                isOnline = isOnline,
                                lastSeenText = lastSeenText,
                                isPinned = existing?.isPinned ?: isPinned,
                                participantIdsCsv = participantIdsCsv
                            )
                            chatDao.insertChat(chat)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("MessageSyncManager", "Failed to listen to chats: ${e.message}")
        }
    }

    override fun stopListeningToChats() {
        chatsListener?.remove()
        chatsListener = null
    }

    override suspend fun configureFirebase(
        projectId: String,
        applicationId: String,
        apiKey: String
    ): Result<Unit> {
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId.trim())
                .setApplicationId(applicationId.trim())
                .setApiKey(apiKey.trim())
                .build()

            // Check if existing default app exists
            val existingApps = FirebaseApp.getApps(context)
            if (existingApps.isNotEmpty()) {
                existingApps.first().delete()
            }

            FirebaseApp.initializeApp(context, options)
            firestoreInstance = FirebaseFirestore.getInstance()

            prefs.edit()
                .putString("project_id", projectId.trim())
                .putString("app_id", applicationId.trim())
                .putString("api_key", apiKey.trim())
                .apply()

            _syncStatus.value = SyncStatus(
                state = SyncState.CONNECTED,
                isCloudConnected = true,
                statusMessage = "Successfully connected to Firebase: $projectId"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus(
                state = SyncState.ERROR,
                isCloudConnected = false,
                statusMessage = "Firebase configuration failed: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }
}
