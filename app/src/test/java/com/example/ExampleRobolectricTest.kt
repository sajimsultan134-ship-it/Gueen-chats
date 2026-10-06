package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthUser
import com.example.data.auth.FirebaseAuthenticationService
import com.example.data.local.AppDatabase
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.repository.ChatRepository
import com.example.data.repository.FirestoreUserRepository
import com.example.data.sync.FirebaseMessageSyncManager
import com.example.data.sync.FirebaseVoiceStorageManager
import com.example.data.sync.SyncState
import com.example.data.util.RecordingState
import com.example.data.util.VoiceRecordingManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gueen Chats", appName)
    }

    @Test
    fun `message creation and status flow`() {
        val message = Message(
            id = "test_1",
            chatId = "chat_test",
            senderId = "user_me",
            senderName = "Sajim Sultan",
            text = "Testing real-time messaging",
            status = MessageStatus.SENT,
            type = MessageType.TEXT,
            isOutgoing = true
        )
        assertNotNull(message)
        assertEquals(MessageStatus.SENT, message.status)
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("Sajim Sultan", message.senderName)
    }

    @Test
    fun `sync manager initializes in offline-first mode when unconfigured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val syncManager = FirebaseMessageSyncManager(context, db)
        assertNotNull(syncManager)
        val initialStatus = syncManager.syncStatus.value
        assertNotNull(initialStatus)
        assertEquals(SyncState.OFFLINE_LOCAL, initialStatus.state)
    }

    @Test
    fun `auth service handles registration and profile creation`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = AppDatabase.getInstance(context)
            val authService = FirebaseAuthenticationService(context, db)
            assertNotNull(authService)

            val regResult = authService.registerWithEmail(
                email = "sajim@test.com",
                password = "password123",
                displayName = "Sajim Tester"
            )
            assertTrue(regResult.isSuccess)
            val user = regResult.getOrNull()
            assertNotNull(user)
            assertEquals("Sajim Tester", user?.displayName)
            assertEquals("sajim@test.com", user?.email)
        }
    }

    @Test
    fun `user repository bridges auth session and persists profile to Room`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = AppDatabase.getInstance(context)
            val userRepo = FirestoreUserRepository(context, db)

            val authUser = AuthUser(
                uid = "user_sajim_test",
                email = "sajim@example.com",
                displayName = "Sajim Sultan VIP",
                phoneNumber = "+1 555-7788"
            )

            val result = userRepo.syncUserProfile(authUser)
            assertTrue(result.isSuccess)
            val syncedUser = result.getOrNull()
            assertNotNull(syncedUser)
            assertEquals("Sajim Sultan VIP", syncedUser?.name)
            assertEquals("+1 555-7788", syncedUser?.phoneNumber)

            // Verify updated profile
            userRepo.updateUserProfile(
                name = "Sajim Sultan Lead",
                statusBio = "Managing real-time messaging on Gueen Chats 🚀",
                phoneNumber = "+1 555-9999",
                avatarUrl = null
            )

            val currentLocal = userRepo.currentUser.first()
            assertNotNull(currentLocal)
            assertEquals("Sajim Sultan Lead", currentLocal?.name)
            assertEquals("+1 555-9999", currentLocal?.phoneNumber)
        }
    }

    @Test
    fun `chat list displays conversations with profile, last message preview, and unread count`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val repository = ChatRepository(context)

            val testChat = Chat(
                id = "chat_test_conversation",
                name = "Sarah Connor",
                isGroup = false,
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                lastMessage = "Hey Sajim, did you deploy the new update?",
                lastMessageTime = 1710000000000L,
                unreadCount = 3,
                isOnline = true,
                lastSeenText = "online"
            )
            val db = AppDatabase.getInstance(context)
            db.chatDao().insertChat(testChat)

            val chats = repository.getAllChats().first()
            assertTrue(chats.any { it.id == "chat_test_conversation" })
            val found = chats.first { it.id == "chat_test_conversation" }
            assertEquals("Sarah Connor", found.name)
            assertEquals("Hey Sajim, did you deploy the new update?", found.lastMessage)
            assertEquals(3, found.unreadCount)
            assertTrue(found.isOnline)
        }
    }

    @Test
    fun `voice recording manager lifecycle - start, preview, and stop`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = VoiceRecordingManager(context)

        assertEquals(RecordingState.IDLE, recorder.recordingState.value)

        // Start recording
        val started = recorder.startRecording()
        assertTrue(started)
        assertEquals(RecordingState.RECORDING, recorder.recordingState.value)
        assertTrue(recorder.isRecording.value)

        // Stop to preview
        recorder.stopToPreview()
        assertEquals(RecordingState.PREVIEW, recorder.recordingState.value)

        // Cancel and clean up
        recorder.cancelRecording()
        assertEquals(RecordingState.IDLE, recorder.recordingState.value)
    }

    @Test
    fun `voice storage manager handles fallback and upload gracefully`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val storageManager = FirebaseVoiceStorageManager(context)
            assertNotNull(storageManager)

            // Test local file fallback
            val tempFile = File(context.cacheDir, "test_voice.m4a")
            tempFile.writeText("audio-data")
            val result = storageManager.uploadVoiceNote("chat_1", "msg_1", tempFile.absolutePath)
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
            tempFile.delete()
        }
    }

    @Test
    fun `phone contacts local loading and privacy phone masking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val contactsRepo = com.example.data.repository.PhoneContactsRepository(context)

        // Test normalization
        val normalized = contactsRepo.normalizePhoneNumber("+1 (555) 0101-99")
        assertEquals("+1555010199", normalized)

        // Test privacy masking
        val masked = contactsRepo.formatMaskedPhoneNumber("+1 555-0101")
        assertTrue(masked.contains("••••"))
        assertTrue(masked.startsWith("+1 555"))
    }

    @Test
    fun `phone contacts client-side matching with registered Queen Chats users`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val contactsRepo = com.example.data.repository.PhoneContactsRepository(context)

            val deviceContacts = contactsRepo.loadDeviceContacts()
            assertTrue(deviceContacts.isNotEmpty())

            val registeredUsers = listOf(
                com.example.data.model.User(
                    id = "user_sarah",
                    name = "Sarah Connor",
                    phoneNumber = "+1 555-0101",
                    statusBio = "Urgent matters only please 💻",
                    isOnline = true
                ),
                com.example.data.model.User(
                    id = "user_alex",
                    name = "Alex Rivera",
                    phoneNumber = "+1 555-0102",
                    statusBio = "At work",
                    isOnline = true
                )
            )

            val matched = contactsRepo.matchContactsWithRegisteredUsers(deviceContacts, registeredUsers)
            val registeredFriends = matched.filter { it.isRegisteredOnQueenChats }
            val unregistered = matched.filter { !it.isRegisteredOnQueenChats }

            assertTrue(registeredFriends.any { it.name == "Sarah Connor" })
            assertTrue(registeredFriends.any { it.name == "Alex Rivera" })
            assertTrue(unregistered.any { it.name == "Jessica Alba" || it.name == "Marcus Aurelius" })
        }
    }

    @Test
    fun `getOrCreateOneToOneChat creates new chat room or reuses existing room`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val repository = ChatRepository(context)

            val testUser = com.example.data.model.User(
                id = "user_david",
                name = "David Beckham",
                phoneNumber = "+1 555-7788",
                isOnline = true
            )

            val chatId1 = repository.getOrCreateOneToOneChat(testUser)
            assertNotNull(chatId1)
            assertTrue(chatId1.isNotEmpty())

            // Calling it again reuses the existing chat room ID
            val chatId2 = repository.getOrCreateOneToOneChat(testUser)
            assertEquals(chatId1, chatId2)
        }
    }

    @Test
    fun `signInWithGoogle creates new user with Google name email and photo without duplicates`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = com.example.data.local.AppDatabase.getInstance(context)
            val authService = com.example.data.auth.FirebaseAuthenticationService(context, database)

            val testEmail = "Sajimsultan134@gmail.com"
            val testName = "Sajim Sultan"
            val testPhoto = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"

            val result1 = authService.signInWithGoogle(
                idToken = null,
                email = testEmail,
                displayName = testName,
                photoUrl = testPhoto
            )

            assertTrue(result1.isSuccess)
            val authUser1 = result1.getOrNull()
            assertNotNull(authUser1)
            assertEquals("Sajimsultan134@gmail.com", authUser1?.email)
            assertEquals("Sajim Sultan", authUser1?.displayName)
            assertEquals(testPhoto, authUser1?.photoUrl)

            val localUser1 = database.userDao().getUserById(authUser1!!.uid)
            assertNotNull(localUser1)
            assertEquals("Sajim Sultan", localUser1?.name)
            assertTrue(localUser1?.isCurrentUser == true)

            // Signing in again with the same Google account does not create duplicate account
            val result2 = authService.signInWithGoogle(
                idToken = null,
                email = testEmail,
                displayName = testName,
                photoUrl = testPhoto
            )

            assertTrue(result2.isSuccess)
            val authUser2 = result2.getOrNull()
            assertEquals(authUser1.uid, authUser2?.uid)

            val currentUsers = database.userDao().getCurrentUserSync()
            assertNotNull(currentUsers)
            assertEquals(authUser1.uid, currentUsers?.id)
        }
    }

    @Test
    fun `signInWithGoogle preserves existing user profile and history`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = com.example.data.local.AppDatabase.getInstance(context)
            val authService = com.example.data.auth.FirebaseAuthenticationService(context, database)

            val testEmail = "Sajimsultan134@gmail.com"
            val uid = "google_sajimsultan134_gmail_com"

            // Pre-seed an existing user profile with custom status
            val existing = com.example.data.model.User(
                id = uid,
                name = "Sajim Sultan",
                phoneNumber = "+1 555-9988",
                statusBio = "Custom existing status bio",
                avatarUrl = null,
                isCurrentUser = false
            )
            database.userDao().insertUser(existing)

            // Sign in with Google
            val result = authService.signInWithGoogle(
                idToken = null,
                email = testEmail,
                displayName = "Sajim Sultan Google",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
            )

            assertTrue(result.isSuccess)
            val updatedUser = database.userDao().getUserById(uid)
            assertNotNull(updatedUser)
            assertEquals("Custom existing status bio", updatedUser?.statusBio)
            assertEquals("+1 555-9988", updatedUser?.phoneNumber)
            assertEquals("Sajim Sultan Google", updatedUser?.name)
            assertTrue(updatedUser?.isCurrentUser == true)
        }
    }

    @Test
    fun `logout signs out user, clears current user in database and updates auth state to unauthenticated`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = com.example.data.local.AppDatabase.getInstance(context)
            val authService = com.example.data.auth.FirebaseAuthenticationService(context, database)

            val testEmail = "Sajimsultan134@gmail.com"
            val result = authService.signInWithGoogle(
                idToken = null,
                email = testEmail,
                displayName = "Sajim Sultan",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
            )
            assertTrue(result.isSuccess)
            assertNotNull(database.userDao().getCurrentUserSync())

            val logoutResult = authService.logout()
            assertTrue(logoutResult.isSuccess)
            val currentUserAfterLogout = database.userDao().getCurrentUserSync()
            assertEquals(null, currentUserAfterLogout)
            assertEquals(com.example.data.auth.AuthState.Unauthenticated, authService.authState.value)
        }
    }

    @Test
    fun `user profile update preserves Google email and updates display name and bio`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = com.example.data.local.AppDatabase.getInstance(context)
            val authService = com.example.data.auth.FirebaseAuthenticationService(context, database)
            val userRepo = com.example.data.repository.FirestoreUserRepository(context, database)

            val testEmail = "Sajimsultan134@gmail.com"
            val signResult = authService.signInWithGoogle(
                idToken = null,
                email = testEmail,
                displayName = "Sajim Sultan",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
            )
            assertTrue(signResult.isSuccess)
            val authUser = signResult.getOrNull()!!

            userRepo.syncUserProfile(authUser)

            // Update user profile later
            userRepo.updateUserProfile(
                name = "Sajim Sultan Pro",
                statusBio = "Active on Gueen Chats 💬",
                phoneNumber = "+1 555-4321",
                avatarUrl = null
            )

            val updatedUser = database.userDao().getCurrentUserSync()
            assertNotNull(updatedUser)
            assertEquals("Sajim Sultan Pro", updatedUser?.name)
            assertEquals("Active on Gueen Chats 💬", updatedUser?.statusBio)
            assertEquals("+1 555-4321", updatedUser?.phoneNumber)
            assertEquals(testEmail, updatedUser?.email)
        }
    }
}
