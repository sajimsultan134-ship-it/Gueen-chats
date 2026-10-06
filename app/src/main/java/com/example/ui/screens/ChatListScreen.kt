package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Chat
import com.example.data.model.User
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.ThemePreference
import com.example.ui.components.AvatarView
import com.example.ui.components.formatTime
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenDark
import com.example.ui.theme.WhatsAppGreenLight

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    chats: List<Chat>,
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFirebaseSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    var isSearchActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val searchQuery = viewModel.searchQuery
    val typingMap = viewModel.typingChats
    val themePref = viewModel.themePreference
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // WhatsApp Top App Bar Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(chatColors.topBarBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gueen Chats",
                                color = chatColors.topBarContent,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("app_title")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WhatsAppGreenLight.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Sajim Chat",
                                    color = WhatsAppGreenLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Firebase Sync Indicator Button
                        IconButton(
                            onClick = onOpenFirebaseSetup,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("firebase_sync_status_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Firebase Sync Status",
                                tint = if (syncStatus.isCloudConnected) WhatsAppGreenLight else Color(0xFFE5A93C)
                            )
                        }

                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("search_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = chatColors.topBarContent
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextPref = if (themePref.value == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
                                viewModel.setThemePreference(nextPref)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (themePref.value == ThemePreference.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = chatColors.topBarContent
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = chatColors.topBarContent
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New group") },
                                    onClick = {
                                        menuExpanded = false
                                        onNewGroupClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New chat") },
                                    onClick = {
                                        menuExpanded = false
                                        onNewChatClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Phone Contacts") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.setShowContactsScreen(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Status & Stories") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabSelected(MainTab.STATUS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Firebase Config & Sync Rules") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenFirebaseSetup()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }

                // Search field bar
                AnimatedVisibility(visible = isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery.value,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search chats or registered users...", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("search_text_field"),
                        trailingIcon = {
                            if (searchQuery.value.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = WhatsAppGreenLight,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // WhatsApp Main Tabs Row (Chats, Status, Calls, Settings)
        PrimaryTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = chatColors.topBarBackground,
            contentColor = chatColors.topBarContent,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(activeTab.ordinal),
                    color = WhatsAppGreenLight,
                    height = 3.5.dp
                )
            }
        ) {
            MainTab.entries.forEach { tab ->
                val totalUnread = chats.sumOf { it.unreadCount }
                Tab(
                    selected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (tab) {
                                    MainTab.CHATS -> "CHATS"
                                    MainTab.STATUS -> "STATUS"
                                    MainTab.CALLS -> "CALLS"
                                    MainTab.SETTINGS -> "SETTINGS"
                                },
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (activeTab == tab) chatColors.topBarContent else chatColors.topBarContent.copy(alpha = 0.7f)
                            )
                            if (tab == MainTab.CHATS && totalUnread > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(WhatsAppGreenLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$totalUnread",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // Chats List Body with Registered Users Rail and Conversations
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Registered Users & Online Contacts Horizontal Rail
                if (allContacts.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REGISTERED USERS & ONLINE CONTACTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppGreenLight,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${allContacts.size} users",
                                    fontSize = 11.sp,
                                    color = chatColors.textSecondary
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // "Start New Chat" Action Item
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clickable(onClick = onNewChatClick)
                                            .testTag("rail_new_chat_button")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape)
                                                .border(1.5.dp, WhatsAppGreenLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Contact",
                                                tint = WhatsAppGreenLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "New Chat",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = chatColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // "Phone Contacts" Action Item
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clickable { viewModel.setShowContactsScreen(true) }
                                            .testTag("rail_phone_contacts_button")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape)
                                                .border(1.5.dp, WhatsAppGreenLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Contacts,
                                                contentDescription = "Phone Contacts",
                                                tint = WhatsAppGreenLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Contacts",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = chatColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Registered User Avatars
                                items(allContacts, key = { it.id }) { contact ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clickable {
                                                val targetChatId = contact.id.replace("user_", "chat_")
                                                onChatClick(targetChatId)
                                            }
                                            .testTag("rail_user_${contact.id}")
                                    ) {
                                        AvatarView(
                                            name = contact.name,
                                            avatarUrl = contact.avatarUrl,
                                            isGroup = false,
                                            showOnlineBadge = true,
                                            isOnline = contact.isOnline,
                                            size = 50.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = chatColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = chatColors.divider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Recent Conversations Header
                if (chats.isNotEmpty()) {
                    item {
                        Text(
                            text = "RECENT CONVERSATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = chatColors.textSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Empty State for Chats
                if (chats.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (searchQuery.value.isNotEmpty()) "No matching conversations" else "No recent conversations",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = chatColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.value.isNotEmpty()) "Try a different search term" else "Tap a registered user above or the message button to start chatting!",
                                fontSize = 13.sp,
                                color = chatColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNewChatClick,
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start a New Chat")
                            }
                        }
                    }
                } else {
                    // Conversations Items
                    items(chats, key = { it.id }) { chat ->
                        val isTyping = typingMap.value.containsKey(chat.id)
                        val typingAuthor = typingMap.value[chat.id]

                        ChatItemRow(
                            chat = chat,
                            isTyping = isTyping,
                            typingAuthor = typingAuthor,
                            onClick = { onChatClick(chat.id) }
                        )
                        HorizontalDivider(
                            color = chatColors.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }

            // WhatsApp Floating Action Button
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = WhatsAppGreenLight,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
                    .testTag("fab_new_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    isTyping: Boolean,
    typingAuthor: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_item_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            name = chat.name,
            avatarUrl = chat.avatarUrl,
            isGroup = chat.isGroup,
            showOnlineBadge = true,
            isOnline = chat.isOnline,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = chatColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatTime(chat.lastMessageTime),
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) WhatsAppGreenLight else chatColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTyping) {
                    Text(
                        text = if (chat.isGroup) "$typingAuthor is typing…" else "typing…",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = WhatsAppGreenLight,
                        maxLines = 1
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Double blue checkmark indicator if read or sent
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = if (chat.unreadCount == 0) Color(0xFF34B7F1) else Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = chat.lastMessage.ifEmpty { "Tap to start chatting" },
                            fontSize = 14.sp,
                            color = chatColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = chatColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(WhatsAppGreenLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${chat.unreadCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
