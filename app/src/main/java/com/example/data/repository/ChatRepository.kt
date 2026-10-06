package com.example.data.repository

import android.content.Context
import com.example.data.auth.AuthState
import com.example.data.auth.AuthUser
import com.example.data.auth.FirebaseAuthenticationService
import com.example.data.auth.GoogleSignInHelper
import com.example.data.local.AppDatabase
import com.example.data.model.CallRecord
import com.example.data.model.CallState
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.StatusStory
import com.example.data.model.User
import com.example.data.sync.FirebaseMediaStorageManager
import com.example.data.sync.FirebaseMessageSyncManager
import com.example.data.sync.FirebaseVoiceStorageManager
import com.example.data.sync.MediaStorageManager
import com.example.data.sync.MessageSyncManager
import com.example.data.sync.SyncStatus
import com.example.data.sync.VoiceStorageManager
import com.example.data.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val userDao = database.userDao()
    private val callDao = database.callDao()
    private val notificationHelper = NotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Room-to-Firestore synchronization manager
    val syncManager: MessageSyncManager = FirebaseMessageSyncManager(context, database)
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    // Firebase Authentication service
    val authService: FirebaseAuthenticationService = FirebaseAuthenticationService(context, database)
    val authState: StateFlow<AuthState> = authService.authState
    val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper(context)

    // User repository managing Firestore profiles and presence
    val userRepository: UserRepository = FirestoreUserRepository(context, database)

    // Media & Voice storage manager for Firebase Storage
    val mediaStorageManager: MediaStorageManager = FirebaseMediaStorageManager(context)
    val voiceStorageManager: VoiceStorageManager get() = mediaStorageManager

    // Current active chat (to avoid showing notifications if user is inside this chat)
    val activeChatId = MutableStateFlow<String?>(null)

    // Real-time typing indicators for chats
    private val _typingChats = MutableStateFlow<Map<String, String>>(emptyMap()) // chatId -> contactName
    val typingChats: StateFlow<Map<String, String>> = _typingChats.asStateFlow()

    // Call state for WebRTC / Call screen
    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    private var callTimerJob: Job? = null

    // In-memory status stories (WhatsApp Status / Stories feature)
    private val _stories = MutableStateFlow<List<StatusStory>>(emptyList())
    val stories: StateFlow<List<StatusStory>> = _stories.asStateFlow()

    init {
        scope.launch {
            seedInitialDataIfNecessary()
            initStories()
            syncManager.startListeningToChats()
            userRepository.startListeningToContacts()
            authService.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    userRepository.syncUserProfile(state.user)
                }
            }
        }
    }

    fun setActiveChat(chatId: String?) {
        val previous = activeChatId.value
        if (previous != null) {
            syncManager.stopSync(previous)
        }
        activeChatId.value = chatId
        if (chatId != null) {
            syncManager.startSync(chatId)
            scope.launch {
                clearUnread(chatId)
            }
        }
    }

    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()
    fun getChat(chatId: String): Flow<Chat?> = chatDao.getChatById(chatId)
    fun getMessages(chatId: String): Flow<List<Message>> = messageDao.getMessagesForChat(chatId)
    fun getAllContacts(): Flow<List<User>> = userRepository.allContacts
    fun getCurrentUser(): Flow<User?> = userRepository.currentUser
    fun getAllCalls(): Flow<List<CallRecord>> = callDao.getAllCalls()

    private suspend fun seedInitialDataIfNecessary() {
        val existing = userDao.getCurrentUserSync()
        if (existing == null) {
            val currentUser = User(
                id = "user_me",
                name = "Sajim Sultan",
                phoneNumber = "+1 555-0134",
                statusBio = "Building Sajim Chat on Gueen Chats 🚀",
                isOnline = true,
                lastSeenText = "online",
                isCurrentUser = true
            )

            val contacts = listOf(
                currentUser,
                User(
                    id = "user_sarah",
                    name = "Sarah Connor",
                    phoneNumber = "+1 555-0101",
                    statusBio = "Urgent matters only please 💻",
                    isOnline = true,
                    lastSeenText = "online"
                ),
                User(
                    id = "user_alex",
                    name = "Alex Rivera",
                    phoneNumber = "+1 555-0102",
                    statusBio = "At work • Available for chat",
                    isOnline = true,
                    lastSeenText = "online"
                ),
                User(
                    id = "user_emily",
                    name = "Emily Watson",
                    phoneNumber = "+1 555-0103",
                    statusBio = "Exploring Jetpack Compose & Real-time WebRTC 🎧",
                    isOnline = false,
                    lastSeenText = "last seen today at 09:12 AM"
                ),
                User(
                    id = "user_michael",
                    name = "Michael Chen",
                    phoneNumber = "+1 555-0104",
                    statusBio = "Design is how it works 🎨",
                    isOnline = false,
                    lastSeenText = "last seen yesterday at 11:45 PM"
                )
            )
            userDao.insertUsers(contacts)

            val now = System.currentTimeMillis()
            val initialChats = listOf(
                Chat(
                    id = "chat_sarah",
                    name = "Sarah Connor",
                    isGroup = false,
                    lastMessage = "Awesome! The real-time messaging is so fast 🚀",
                    lastMessageTime = now - (2 * 60 * 1000),
                    unreadCount = 1,
                    isOnline = true,
                    lastSeenText = "online",
                    isPinned = true
                ),
                Chat(
                    id = "chat_group_tech",
                    name = "Sajim Chat Developers 💻",
                    isGroup = true,
                    lastMessage = "Alex: Updated the Firebase sync rules and voice recording!",
                    lastMessageTime = now - (15 * 60 * 1000),
                    unreadCount = 3,
                    isOnline = true,
                    lastSeenText = "Sarah, Alex, Emily, You",
                    isPinned = true,
                    participantIdsCsv = "user_sarah,user_alex,user_emily,user_me"
                ),
                Chat(
                    id = "chat_alex",
                    name = "Alex Rivera",
                    isGroup = false,
                    lastMessage = "Check out the new dark mode theme!",
                    lastMessageTime = now - (45 * 60 * 1000),
                    unreadCount = 0,
                    isOnline = true,
                    lastSeenText = "online"
                ),
                Chat(
                    id = "chat_emily",
                    name = "Emily Watson",
                    isGroup = false,
                    lastMessage = "Voice message (0:14)",
                    lastMessageTime = now - (3 * 3600 * 1000),
                    unreadCount = 0,
                    isOnline = false,
                    lastSeenText = "last seen today at 09:12 AM"
                ),
                Chat(
                    id = "chat_michael",
                    name = "Michael Chen",
                    isGroup = false,
                    lastMessage = "Sent you the project design assets",
                    lastMessageTime = now - (24 * 3600 * 1000),
                    unreadCount = 0,
                    isOnline = false,
                    lastSeenText = "last seen yesterday at 11:45 PM"
                )
            )
            chatDao.insertChats(initialChats)

            // Seed sample call records
            val sampleCalls = listOf(
                CallRecord(
                    id = "call_1",
                    contactId = "user_sarah",
                    contactName = "Sarah Connor",
                    isVideo = true,
                    isIncoming = true,
                    isMissed = false,
                    timestamp = now - (35 * 60 * 1000),
                    durationSeconds = 184
                ),
                CallRecord(
                    id = "call_2",
                    contactId = "user_alex",
                    contactName = "Alex Rivera",
                    isVideo = false,
                    isIncoming = false,
                    isMissed = false,
                    timestamp = now - (4 * 3600 * 1000),
                    durationSeconds = 62
                ),
                CallRecord(
                    id = "call_3",
                    contactId = "user_emily",
                    contactName = "Emily Watson",
                    isVideo = false,
                    isIncoming = true,
                    isMissed = true,
                    timestamp = now - (22 * 3600 * 1000),
                    durationSeconds = 0
                )
            )
            callDao.insertCalls(sampleCalls)
        }
    }

    private fun initStories() {
        val now = System.currentTimeMillis()
        _stories.value = listOf(
            StatusStory(
                id = "story_me",
                userId = "user_me",
                userName = "My Status",
                caption = "Launched Gueen Chats v1.0! Try audio calls & voice notes 💚",
                timestamp = now - (40 * 60 * 1000),
                isSeen = true,
                colorHex = 0xFF008069
            ),
            StatusStory(
                id = "story_sarah",
                userId = "user_sarah",
                userName = "Sarah Connor",
                caption = "Coffee and code ✨ Nothing beats Kotlin Compose!",
                timestamp = now - (90 * 60 * 1000),
                isSeen = false,
                colorHex = 0xFF128C7E
            ),
            StatusStory(
                id = "story_alex",
                userId = "user_alex",
                userName = "Alex Rivera",
                caption = "Weekend hike with stunning views 🏔️",
                timestamp = now - (180 * 60 * 1000),
                isSeen = false,
                colorHex = 0xFF075E54
            )
        )
    }

    fun markStoryAsSeen(storyId: String) {
        _stories.value = _stories.value.map {
            if (it.id == storyId) it.copy(isSeen = true) else it
        }
    }

    fun addStatusStory(caption: String) {
        val newStory = StatusStory(
            id = "story_${System.currentTimeMillis()}",
            userId = "user_me",
            userName = "My Status",
            caption = caption,
            timestamp = System.currentTimeMillis(),
            isSeen = false,
            colorHex = 0xFF008069
        )
        _stories.value = listOf(newStory) + _stories.value
    }

    suspend fun clearUnread(chatId: String) {
        chatDao.clearUnread(chatId)
    }

    suspend fun getOrCreateOneToOneChat(contact: User): String {
        val currentUser = userDao.getCurrentUserSync()
        val currentUserId = currentUser?.id ?: "user_me"
        val contactUserId = contact.id

        // Check if an existing 1-on-1 chat room exists
        val allChats = chatDao.getAllChatsSync()
        val existing = allChats.firstOrNull { chat ->
            !chat.isGroup && (
                chat.id == "chat_${contactUserId.removePrefix("user_")}" ||
                chat.id == contactUserId ||
                (chat.participantIdsCsv?.contains(contactUserId) == true) ||
                chat.name.equals(contact.name, ignoreCase = true)
            )
        }

        if (existing != null) {
            return existing.id
        }

        // Create new Chat room
        val chatId = "chat_${contactUserId.removePrefix("user_")}"
        val now = System.currentTimeMillis()
        val newChat = Chat(
            id = chatId,
            name = contact.name,
            isGroup = false,
            avatarUrl = contact.avatarUrl,
            lastMessage = "Started a new conversation",
            lastMessageTime = now,
            unreadCount = 0,
            isOnline = contact.isOnline,
            lastSeenText = contact.lastSeenText,
            participantIdsCsv = "$currentUserId,$contactUserId"
        )
        chatDao.insertChat(newChat)
        return chatId
    }

    fun sendMessage(
        chatId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUri: String? = null,
        mediaDurationSec: Int = 0,
        mediaFileName: String? = null,
        mediaFileSize: String? = null
    ) {
        scope.launch {
            val messageId = "msg_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()

            val currentUser = userDao.getCurrentUserSync()
            val currentUserId = currentUser?.id ?: "user_me"
            val currentUserName = currentUser?.name ?: "Sajim Sultan"

            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                senderName = currentUserName,
                text = text,
                timestamp = now,
                status = MessageStatus.SENDING,
                type = type,
                mediaUri = mediaUri,
                mediaDurationSec = mediaDurationSec,
                mediaFileName = mediaFileName,
                mediaFileSize = mediaFileSize,
                isOutgoing = true
            )
            messageDao.insertMessage(message)

            val previewText = when (type) {
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VOICE -> "🎤 Voice message ($mediaDurationSec" + "s)"
                MessageType.DOCUMENT -> "📄 " + (mediaFileName ?: "Document")
                MessageType.VIDEO -> "🎥 Video"
                MessageType.TEXT -> text
            }
            chatDao.updateLastMessage(chatId, previewText, now)

            // Asynchronously upload media (images, videos, documents, voice notes) to Firebase Storage and push to Firestore
            scope.launch {
                var messageToPush = message
                if (mediaUri != null && !mediaUri.startsWith("http")) {
                    val parsedUri = android.net.Uri.parse(mediaUri)
                    val uploadResult = mediaStorageManager.uploadMedia(chatId, messageId, parsedUri, type)
                    val resultData = uploadResult.getOrNull()
                    if (resultData != null) {
                        messageToPush = message.copy(
                            mediaUri = resultData.downloadUrl,
                            mediaFileName = resultData.fileName.ifEmpty { message.mediaFileName },
                            mediaFileSize = resultData.fileSizeFormatted.ifEmpty { message.mediaFileSize },
                            mediaDurationSec = if (resultData.durationSec > 0) resultData.durationSec else message.mediaDurationSec
                        )
                        messageDao.insertMessage(messageToPush)
                    }
                }
                syncManager.pushMessage(messageToPush)
            }

            // Step 1: Sent (single check) after 250ms
            delay(250)
            messageDao.updateMessageStatus(messageId, MessageStatus.SENT)
            scope.launch {
                syncManager.updateMessageStatus(chatId, messageId, MessageStatus.SENT)
            }

            // Step 2: Delivered (double check) after 600ms
            delay(600)
            messageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
            scope.launch {
                syncManager.updateMessageStatus(chatId, messageId, MessageStatus.DELIVERED)
            }

            // If Firebase is not yet configured, provide local offline echo for solo testing
            if (!syncManager.isFirebaseConfigured()) {
                simulatePartnerReply(chatId, text, type)
            }
        }
    }

    private suspend fun simulatePartnerReply(chatId: String, userText: String, type: MessageType) {
        val chat = chatDao.getChatByIdSync(chatId) ?: return

        // Read receipt (blue ticks) after 1.2s
        delay(1200)
        messageDao.markAllOutgoingAs(chatId, MessageStatus.READ)

        // Remote partner starts typing
        delay(800)
        val responderName = if (chat.isGroup) "Sarah Connor" else chat.name
        _typingChats.value = _typingChats.value + (chatId to responderName)

        // Typing delay
        delay(2200)
        _typingChats.value = _typingChats.value - chatId

        // Generate realistic dynamic reply based on what was sent
        val replyText = generateReplyText(userText, type, chat.name, chat.isGroup)
        val incomingMsgId = "msg_in_${UUID.randomUUID()}"
        val replyTime = System.currentTimeMillis()

        val incomingMessage = Message(
            id = incomingMsgId,
            chatId = chatId,
            senderId = if (chat.isGroup) "user_sarah" else chat.id,
            senderName = responderName,
            text = replyText,
            timestamp = replyTime,
            status = MessageStatus.DELIVERED,
            type = MessageType.TEXT,
            isOutgoing = false
        )
        messageDao.insertMessage(incomingMessage)
        chatDao.updateLastMessage(chatId, replyText, replyTime)
        scope.launch {
            syncManager.pushMessage(incomingMessage)
        }

        // If user is currently looking at another screen or another chat, show notification
        if (activeChatId.value != chatId) {
            notificationHelper.showMessageNotification(
                senderName = if (chat.isGroup) "${chat.name} ($responderName)" else responderName,
                messageText = replyText,
                chatId = chatId
            )
        }
    }

    private fun generateReplyText(userText: String, type: MessageType, contactName: String, isGroup: Boolean): String {
        val lower = userText.lowercase()
        return when {
            type == MessageType.VOICE -> "Got your voice message! Loud and clear 👍"
            type == MessageType.IMAGE -> "Great picture! Looks really sharp 📸"
            type == MessageType.DOCUMENT -> "Downloaded the document, taking a look now."
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hey Sajim! Good to hear from you. Everything is running smoothly!"
            lower.contains("call") ->
                "Sure, feel free to tap the call or video icon at the top whenever you're free!"
            lower.contains("how are you") || lower.contains("what's up") ->
                "Doing great! Working on the Gueen Chats real-time features. How about you?"
            lower.contains("firebase") ->
                "Firebase Firestore & Storage are configured and ready for live synchronization!"
            lower.contains("status") || lower.contains("stories") ->
                "Saw the new status update in the Status tab, very cool!"
            isGroup -> "Got it team! Agree with Sajim on this."
            else -> "Thanks for the message! Let me know if you want to test a video call or send more voice notes."
        }
    }

    fun createGroupChat(groupName: String, participantIds: List<String>) {
        scope.launch {
            val groupId = "group_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()
            val newGroup = Chat(
                id = groupId,
                name = groupName,
                isGroup = true,
                lastMessage = "Group created with ${participantIds.size + 1} members",
                lastMessageTime = now,
                unreadCount = 0,
                isOnline = true,
                lastSeenText = "${participantIds.size + 1} participants",
                participantIdsCsv = participantIds.joinToString(",") + ",user_me"
            )
            chatDao.insertChat(newGroup)

            val systemMsg = Message(
                id = "sys_${UUID.randomUUID()}",
                chatId = groupId,
                senderId = "user_me",
                senderName = "Sajim Sultan",
                text = "You created group \"$groupName\"",
                timestamp = now,
                status = MessageStatus.READ,
                type = MessageType.TEXT,
                isOutgoing = true
            )
            messageDao.insertMessage(systemMsg)
        }
    }

    fun startCall(contactId: String, contactName: String, isVideo: Boolean) {
        val callRecord = CallRecord(
            id = "call_${UUID.randomUUID()}",
            contactId = contactId,
            contactName = contactName,
            isVideo = isVideo,
            isIncoming = false,
            isMissed = false,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 0
        )
        _callState.value = CallState(
            activeCallRecord = callRecord,
            isInCall = true,
            isIncoming = false,
            isVideoEnabled = isVideo,
            callDurationSec = 0
        )
        startCallTimer()
    }

    fun simulateIncomingCall(contactId: String, contactName: String, isVideo: Boolean) {
        val callRecord = CallRecord(
            id = "call_${UUID.randomUUID()}",
            contactId = contactId,
            contactName = contactName,
            isVideo = isVideo,
            isIncoming = true,
            isMissed = false,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 0
        )
        _callState.value = CallState(
            activeCallRecord = callRecord,
            isInCall = false,
            isIncoming = true,
            isVideoEnabled = isVideo,
            callDurationSec = 0
        )
    }

    fun answerCall() {
        val current = _callState.value
        if (current.isIncoming && current.activeCallRecord != null) {
            _callState.value = current.copy(
                isInCall = true,
                isIncoming = false,
                callDurationSec = 0
            )
            startCallTimer()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            while (isActive && _callState.value.isInCall) {
                delay(1000)
                _callState.value = _callState.value.copy(
                    callDurationSec = _callState.value.callDurationSec + 1
                )
            }
        }
    }

    fun toggleMute() {
        _callState.value = _callState.value.copy(isMuted = !_callState.value.isMuted)
    }

    fun toggleSpeaker() {
        _callState.value = _callState.value.copy(isSpeakerOn = !_callState.value.isSpeakerOn)
    }

    fun toggleVideo() {
        _callState.value = _callState.value.copy(isVideoEnabled = !_callState.value.isVideoEnabled)
    }

    fun endCall() {
        callTimerJob?.cancel()
        val current = _callState.value
        val record = current.activeCallRecord
        if (record != null) {
            val finalRecord = record.copy(
                durationSeconds = current.callDurationSec,
                isMissed = current.isIncoming && !current.isInCall
            )
            scope.launch {
                callDao.insertCall(finalRecord)
            }
        }
        _callState.value = CallState()
    }

    suspend fun updateUserProfile(name: String, statusBio: String, phoneNumber: String) {
        userRepository.updateUserProfile(name, statusBio, phoneNumber, null)
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
        messageDao.deleteMessagesForChat(chatId)
    }

    suspend fun clearCallHistory() {
        callDao.clearCallHistory()
    }
}
