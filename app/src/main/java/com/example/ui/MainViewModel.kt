package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallRecord
import com.example.data.model.CallState
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.PhoneContact
import com.example.data.model.StatusStory
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.PhoneContactsRepository
import com.example.data.sync.FirebaseMediaStorageManager
import com.example.data.util.VoicePlaybackManager
import com.example.data.util.VoiceRecordingManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS,
    STATUS,
    CALLS,
    SETTINGS
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ChatRepository(application)
    val voiceRecorder = VoiceRecordingManager(application)
    val voicePlayer = VoicePlaybackManager(application)

    private val _activeTab = MutableStateFlow(MainTab.CHATS)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _themePreference = MutableStateFlow(ThemePreference.DARK)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    // Dialog & Screen sheet states
    private val _showNewChatDialog = MutableStateFlow(false)
    val showNewChatDialog: StateFlow<Boolean> = _showNewChatDialog.asStateFlow()

    private val _showNewGroupDialog = MutableStateFlow(false)
    val showNewGroupDialog: StateFlow<Boolean> = _showNewGroupDialog.asStateFlow()

    private val _activeStoryView = MutableStateFlow<StatusStory?>(null)
    val activeStoryView: StateFlow<StatusStory?> = _activeStoryView.asStateFlow()

    private val _showFirebaseDialog = MutableStateFlow(false)
    val showFirebaseDialog: StateFlow<Boolean> = _showFirebaseDialog.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    private val _showAddStatusDialog = MutableStateFlow(false)
    val showAddStatusDialog: StateFlow<Boolean> = _showAddStatusDialog.asStateFlow()

    private val _enlargedImageUri = MutableStateFlow<String?>(null)
    val enlargedImageUri: StateFlow<String?> = _enlargedImageUri.asStateFlow()

    private val _activeVideoPlayerUri = MutableStateFlow<String?>(null)
    val activeVideoPlayerUri: StateFlow<String?> = _activeVideoPlayerUri.asStateFlow()

    private val _showAuthScreen = MutableStateFlow(false)
    val showAuthScreen: StateFlow<Boolean> = _showAuthScreen.asStateFlow()

    // Phone Contacts Integration State
    val phoneContactsRepository = PhoneContactsRepository(application)

    private val _showContactsScreen = MutableStateFlow(false)
    val showContactsScreen: StateFlow<Boolean> = _showContactsScreen.asStateFlow()

    private val _rawDeviceContacts = MutableStateFlow<List<PhoneContact>>(emptyList())

    private val _contactsSearchQuery = MutableStateFlow("")
    val contactsSearchQuery: StateFlow<String> = _contactsSearchQuery.asStateFlow()

    private val _isSyncingContacts = MutableStateFlow(false)
    val isSyncingContacts: StateFlow<Boolean> = _isSyncingContacts.asStateFlow()

    val authState = repository.authState

    // Data streams
    val allContacts: StateFlow<List<User>> = repository.getAllContacts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Real-time reactive matching: when any new user registers on Firestore, phoneContacts updates automatically
    val phoneContacts: StateFlow<List<PhoneContact>> = combine(
        _rawDeviceContacts,
        allContacts
    ) { rawList, registeredUsers ->
        phoneContactsRepository.matchContactsWithRegisteredUsers(rawList, registeredUsers)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentUser: StateFlow<User?> = repository.getCurrentUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allCalls: StateFlow<List<CallRecord>> = repository.getAllCalls().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stories: StateFlow<List<StatusStory>> = repository.stories

    val callState: StateFlow<CallState> = repository.callState

    val typingChats: StateFlow<Map<String, String>> = repository.typingChats

    val syncStatus: StateFlow<com.example.data.sync.SyncStatus> = repository.syncStatus

    // Filtered chats list
    val filteredChats: StateFlow<List<Chat>> = combine(
        repository.getAllChats(),
        _searchQuery
    ) { chats, query ->
        if (query.isBlank()) {
            chats
        } else {
            chats.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChat: StateFlow<Chat?> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(null) else repository.getChat(chatId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<Message>> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(emptyList()) else repository.getMessages(chatId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        repository.setActiveChat(chatId)
    }

    fun configureFirebase(
        projectId: String,
        appId: String,
        apiKey: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.syncManager.configureFirebase(projectId, appId, apiKey)
            if (result.isSuccess) {
                onResult(true, "Connected to Firebase project $projectId")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Configuration failed")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
    }

    fun setShowNewChat(show: Boolean) {
        _showNewChatDialog.value = show
    }

    fun setShowNewGroup(show: Boolean) {
        _showNewGroupDialog.value = show
    }

    fun setShowProfile(show: Boolean) {
        _showProfileDialog.value = show
    }

    fun setShowFirebase(show: Boolean) {
        _showFirebaseDialog.value = show
    }

    fun setShowAddStatus(show: Boolean) {
        _showAddStatusDialog.value = show
    }

    fun setShowContactsScreen(show: Boolean) {
        _showContactsScreen.value = show
        if (show && _rawDeviceContacts.value.isEmpty()) {
            loadAndSyncPhoneContacts()
        }
    }

    fun setContactsSearchQuery(query: String) {
        _contactsSearchQuery.value = query
    }

    fun loadAndSyncPhoneContacts() {
        viewModelScope.launch {
            _isSyncingContacts.value = true
            try {
                val deviceContacts = phoneContactsRepository.loadDeviceContacts()
                _rawDeviceContacts.value = deviceContacts
            } catch (_: Exception) {
            } finally {
                _isSyncingContacts.value = false
            }
        }
    }

    fun openOrCreateChatForContact(contact: PhoneContact) {
        viewModelScope.launch {
            val user = contact.registeredUser ?: User(
                id = if (contact.id.startsWith("user_")) contact.id else "user_${contact.id}",
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                avatarUrl = contact.photoUri
            )
            val chatId = repository.getOrCreateOneToOneChat(user)
            _showContactsScreen.value = false
            selectChat(chatId)
        }
    }

    fun openStory(story: StatusStory?) {
        _activeStoryView.value = story
        if (story != null) {
            repository.markStoryAsSeen(story.id)
        }
    }

    fun viewEnlargedImage(uri: String?) {
        _enlargedImageUri.value = uri
    }

    fun sendTextMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        repository.sendMessage(chatId = chatId, text = text, type = MessageType.TEXT)
    }

    fun startVoiceRecording(): Boolean {
        return voiceRecorder.startRecording()
    }

    fun pauseOrStopVoiceRecordingToPreview() {
        voiceRecorder.stopToPreview()
    }

    fun toggleVoicePreviewPlayback() {
        voiceRecorder.togglePreviewPlayback()
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
    }

    fun sendVoiceMessage(chatId: String) {
        val (filePath, duration) = voiceRecorder.stopRecording()
        repository.sendMessage(
            chatId = chatId,
            text = "Voice message (${duration}s)",
            type = MessageType.VOICE,
            mediaUri = filePath,
            mediaDurationSec = duration
        )
    }

    fun sendImageMessage(chatId: String, sampleImageResOrUri: String) {
        repository.sendMessage(
            chatId = chatId,
            text = "Photo",
            type = MessageType.IMAGE,
            mediaUri = sampleImageResOrUri
        )
    }

    fun sendDocumentMessage(chatId: String, fileName: String, fileSize: String) {
        repository.sendMessage(
            chatId = chatId,
            text = fileName,
            type = MessageType.DOCUMENT,
            mediaFileName = fileName,
            mediaFileSize = fileSize
        )
    }

    fun openVideoPlayer(uri: String?) {
        _activeVideoPlayerUri.value = uri
    }

    fun sendImageFromUri(chatId: String, uri: Uri, caption: String = "") {
        repository.sendMessage(
            chatId = chatId,
            text = caption.ifBlank { "Photo" },
            type = MessageType.IMAGE,
            mediaUri = uri.toString()
        )
    }

    fun sendVideoFromUri(chatId: String, uri: Uri, caption: String = "") {
        repository.sendMessage(
            chatId = chatId,
            text = caption.ifBlank { "Video" },
            type = MessageType.VIDEO,
            mediaUri = uri.toString()
        )
    }

    fun sendDocumentFromUri(chatId: String, uri: Uri) {
        var fileName = "Document_${System.currentTimeMillis()}.pdf"
        var fileSize = "Document"
        val context = getApplication<Application>()
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) cursor.getString(nameIndex)?.let { if (it.isNotBlank()) fileName = it }
                    if (sizeIndex != -1) {
                        val bytes = cursor.getLong(sizeIndex)
                        fileSize = (repository.mediaStorageManager as? FirebaseMediaStorageManager)?.formatFileSize(bytes) ?: "$bytes B"
                    }
                }
            }
        } catch (_: Exception) {}

        repository.sendMessage(
            chatId = chatId,
            text = fileName,
            type = MessageType.DOCUMENT,
            mediaUri = uri.toString(),
            mediaFileName = fileName,
            mediaFileSize = fileSize
        )
    }

    fun createGroup(name: String, participantIds: List<String>) {
        if (name.isBlank()) return
        repository.createGroupChat(name, participantIds)
        _showNewGroupDialog.value = false
    }

    fun addStatusStory(caption: String) {
        if (caption.isBlank()) return
        repository.addStatusStory(caption)
        _showAddStatusDialog.value = false
    }

    fun startCall(contactId: String, contactName: String, isVideo: Boolean) {
        repository.startCall(contactId, contactName, isVideo)
    }

    fun testIncomingCall(contactId: String, contactName: String, isVideo: Boolean) {
        repository.simulateIncomingCall(contactId, contactName, isVideo)
    }

    fun answerCall() {
        repository.answerCall()
    }

    fun endCall() {
        repository.endCall()
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun toggleSpeaker() {
        repository.toggleSpeaker()
    }

    fun toggleVideo() {
        repository.toggleVideo()
    }

    fun updateProfile(name: String, status: String, phone: String) {
        viewModelScope.launch {
            repository.updateUserProfile(name, status, phone)
            repository.authService.updateProfile(name, null)
            _showProfileDialog.value = false
        }
    }

    fun setShowAuth(show: Boolean) {
        _showAuthScreen.value = show
    }

    fun registerUser(
        email: String,
        pass: String,
        displayName: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.authService.registerWithEmail(email, pass, displayName)
            if (result.isSuccess) {
                _showAuthScreen.value = false
                callback(true, "Registration successful! Welcome ${result.getOrNull()?.displayName}")
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun loginUser(
        email: String,
        pass: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.authService.loginWithEmail(email, pass)
            if (result.isSuccess) {
                _showAuthScreen.value = false
                callback(true, "Logged in successfully!")
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signInWithGoogle(
        activityContext: Context,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val googleResult = repository.googleSignInHelper.signInWithGoogle(activityContext)
            if (googleResult.isSuccess) {
                val account = googleResult.getOrNull()!!
                val authResult = repository.authService.signInWithGoogle(
                    idToken = account.idToken,
                    email = account.email,
                    displayName = account.displayName,
                    photoUrl = account.photoUrl
                )
                if (authResult.isSuccess) {
                    _showAuthScreen.value = false
                    callback(true, "Welcome ${authResult.getOrNull()?.displayName}!")
                } else {
                    callback(false, authResult.exceptionOrNull()?.message ?: "Google Authentication failed")
                }
            } else {
                val exception = googleResult.exceptionOrNull()
                val message = exception?.message ?: "Google Sign-In failed"
                callback(false, message)
            }
        }
    }

    fun signInWithTestGoogleAccount(
        email: String = "Sajimsultan134@gmail.com",
        displayName: String = "Sajim Sultan",
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val authResult = repository.authService.signInWithGoogle(
                idToken = null,
                email = email,
                displayName = displayName,
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
            )
            if (authResult.isSuccess) {
                _showAuthScreen.value = false
                callback(true, "Signed in as $displayName ($email)")
            } else {
                callback(false, authResult.exceptionOrNull()?.message ?: "Google Sign-In failed")
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            repository.authService.logout()
            _showAuthScreen.value = true
        }
    }

    fun resetPassword(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.authService.sendPasswordReset(email)
            if (result.isSuccess) {
                callback(true, "Password reset email sent to $email")
            } else {
                callback(false, result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_selectedChatId.value == chatId) {
                _selectedChatId.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.cancelRecording()
        voicePlayer.stopPlayback()
    }
}
