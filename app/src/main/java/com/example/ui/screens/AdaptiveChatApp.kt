package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun AdaptiveChatApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val filteredChats by viewModel.filteredChats.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allCalls by viewModel.allCalls.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()

    val showNewChatDialog by viewModel.showNewChatDialog.collectAsStateWithLifecycle()
    val showNewGroupDialog by viewModel.showNewGroupDialog.collectAsStateWithLifecycle()
    val activeStoryView by viewModel.activeStoryView.collectAsStateWithLifecycle()
    val showFirebaseDialog by viewModel.showFirebaseDialog.collectAsStateWithLifecycle()
    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showAddStatusDialog by viewModel.showAddStatusDialog.collectAsStateWithLifecycle()
    val enlargedImageUri by viewModel.enlargedImageUri.collectAsStateWithLifecycle()
    val showAuthScreen by viewModel.showAuthScreen.collectAsStateWithLifecycle()
    val showContactsScreen by viewModel.showContactsScreen.collectAsStateWithLifecycle()

    val chatColors = LocalChatColors.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive Tablet / Desktop Split View Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Chats, Status, Calls, Settings (fixed 380dp)
                Box(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                ) {
                    when (activeTab) {
                        MainTab.CHATS -> ChatListScreen(
                            viewModel = viewModel,
                            chats = filteredChats,
                            activeTab = activeTab,
                            onTabSelected = { viewModel.setTab(it) },
                            onChatClick = { viewModel.selectChat(it) },
                            onNewChatClick = { viewModel.setShowNewChat(true) },
                            onNewGroupClick = { viewModel.setShowNewGroup(true) },
                            onOpenSettings = { viewModel.setTab(MainTab.SETTINGS) },
                            onOpenFirebaseSetup = { viewModel.setShowFirebase(true) }
                        )
                        MainTab.STATUS -> StatusScreen(
                            viewModel = viewModel,
                            stories = stories,
                            onStoryClick = { viewModel.openStory(it) },
                            onAddStatusClick = { viewModel.setShowAddStatus(true) }
                        )
                        MainTab.CALLS -> CallsScreen(
                            viewModel = viewModel,
                            calls = allCalls,
                            onStartCall = { contactId, contactName, isVideo ->
                                viewModel.startCall(contactId, contactName, isVideo)
                            }
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onOpenProfile = { viewModel.setShowProfile(true) },
                            onOpenFirebaseSetup = { viewModel.setShowFirebase(true) }
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(chatColors.divider)
                )

                // Right Panel: Conversation or Empty Placeholder State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (activeChat != null) {
                        ChatScreen(
                            viewModel = viewModel,
                            chat = activeChat!!,
                            messages = activeMessages,
                            onBack = { viewModel.selectChat(null) }
                        )
                    } else {
                        // WhatsApp Web style placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(chatColors.chatBackground)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(R.drawable.ic_app_icon)
                                        .build(),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Gueen Chats for Android",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = chatColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Send and receive real-time messages, voice notes, and make audio/video calls.",
                                fontSize = 14.sp,
                                color = chatColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(30.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = chatColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "End-to-end encrypted",
                                    fontSize = 12.sp,
                                    color = chatColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Phone Flow: Switch between List/Tabs and Conversation Screen
            if (selectedChatId != null && activeChat != null) {
                ChatScreen(
                    viewModel = viewModel,
                    chat = activeChat!!,
                    messages = activeMessages,
                    onBack = { viewModel.selectChat(null) }
                )
            } else {
                when (activeTab) {
                    MainTab.CHATS -> ChatListScreen(
                        viewModel = viewModel,
                        chats = filteredChats,
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setTab(it) },
                        onChatClick = { viewModel.selectChat(it) },
                        onNewChatClick = { viewModel.setShowNewChat(true) },
                        onNewGroupClick = { viewModel.setShowNewGroup(true) },
                        onOpenSettings = { viewModel.setTab(MainTab.SETTINGS) },
                        onOpenFirebaseSetup = { viewModel.setShowFirebase(true) }
                    )
                    MainTab.STATUS -> StatusScreen(
                        viewModel = viewModel,
                        stories = stories,
                        onStoryClick = { viewModel.openStory(it) },
                        onAddStatusClick = { viewModel.setShowAddStatus(true) }
                    )
                    MainTab.CALLS -> CallsScreen(
                        viewModel = viewModel,
                        calls = allCalls,
                        onStartCall = { contactId, contactName, isVideo ->
                            viewModel.startCall(contactId, contactName, isVideo)
                        }
                    )
                    MainTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onOpenProfile = { viewModel.setShowProfile(true) },
                        onOpenFirebaseSetup = { viewModel.setShowFirebase(true) }
                    )
                }
            }
        }

        // Full Screen Calling Overlay
        if (callState.activeCallRecord != null) {
            CallScreen(
                viewModel = viewModel,
                callState = callState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Story Viewer Full Screen Dialog
        if (activeStoryView != null) {
            StoryViewerDialog(
                story = activeStoryView!!,
                onDismiss = { viewModel.openStory(null) }
            )
        }

        // Enlarged Image Viewer
        if (enlargedImageUri != null) {
            Dialog(onDismissRequest = { viewModel.viewEnlargedImage(null) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { viewModel.viewEnlargedImage(null) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (enlargedImageUri?.startsWith("drawable://") == true || enlargedImageUri == null) {
                                    R.drawable.chat_wallpaper
                                } else {
                                    enlargedImageUri
                                }
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { viewModel.viewEnlargedImage(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // New Chat Bottom Sheet
        if (showNewChatDialog) {
            NewChatBottomSheet(
                contacts = allContacts,
                onSelectContact = { contact ->
                    viewModel.setShowNewChat(false)
                    viewModel.selectChat(contact.id.replace("user_", "chat_"))
                },
                onNewGroupClick = {
                    viewModel.setShowNewChat(false)
                    viewModel.setShowNewGroup(true)
                },
                onOpenContacts = {
                    viewModel.setShowNewChat(false)
                    viewModel.setShowContactsScreen(true)
                },
                onDismiss = { viewModel.setShowNewChat(false) }
            )
        }

        // Dedicated Phone Contacts Screen
        if (showContactsScreen) {
            ContactsScreen(
                viewModel = viewModel,
                onBack = { viewModel.setShowContactsScreen(false) },
                onStartChat = { chatId ->
                    viewModel.setShowContactsScreen(false)
                    viewModel.selectChat(chatId)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // New Group Dialog
        if (showNewGroupDialog) {
            NewGroupDialog(
                contacts = allContacts,
                onCreateGroup = { name, members ->
                    viewModel.createGroup(name, members)
                },
                onDismiss = { viewModel.setShowNewGroup(false) }
            )
        }

        // User Profile Dialog
        if (showProfileDialog) {
            UserProfileDialog(
                currentUser = currentUser,
                onSave = { name, status, phone ->
                    viewModel.updateProfile(name, status, phone)
                },
                onDismiss = { viewModel.setShowProfile(false) }
            )
        }

        // Add Status Story Dialog
        if (showAddStatusDialog) {
            AddStatusDialog(
                onAddStatus = { caption ->
                    viewModel.addStatusStory(caption)
                },
                onDismiss = { viewModel.setShowAddStatus(false) }
            )
        }

        // Firebase Configuration & Architecture Guide
        if (showFirebaseDialog) {
            val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
            FirebaseGuideDialog(
                syncStatus = syncStatus,
                onConfigureFirebase = { projId, appId, apiKey, callback ->
                    viewModel.configureFirebase(projId, appId, apiKey, callback)
                },
                onDismiss = { viewModel.setShowFirebase(false) }
            )
        }

        // Firebase Authentication Screen
        if (showAuthScreen) {
            AuthScreen(
                onLogin = { email, pass, callback ->
                    viewModel.loginUser(email, pass, callback)
                },
                onRegister = { email, pass, name, callback ->
                    viewModel.registerUser(email, pass, name, callback)
                },
                onPasswordReset = { email, callback ->
                    viewModel.resetPassword(email, callback)
                },
                onGoogleSignIn = { context, callback ->
                    viewModel.signInWithGoogle(context, callback)
                },
                onQuickGoogleSignIn = { email, displayName, callback ->
                    viewModel.signInWithTestGoogleAccount(email, displayName, callback)
                },
                onContinueAsGuest = {
                    viewModel.setShowAuth(false)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
