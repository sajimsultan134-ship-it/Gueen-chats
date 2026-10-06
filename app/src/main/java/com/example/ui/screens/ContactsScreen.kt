package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PhoneContact
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val chatColors = LocalChatColors.current

    val phoneContacts by viewModel.phoneContacts.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingContacts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.contactsSearchQuery.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedExplanation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            permissionDeniedExplanation = false
            viewModel.loadAndSyncPhoneContacts()
        } else {
            permissionDeniedExplanation = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && phoneContacts.isEmpty()) {
            viewModel.loadAndSyncPhoneContacts()
        }
    }

    // Filter contacts based on search query
    val filteredContacts = remember(phoneContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            phoneContacts
        } else {
            val q = searchQuery.trim().lowercase()
            phoneContacts.filter {
                it.name.lowercase().contains(q) || it.phoneNumber.contains(q) || it.normalizedPhoneNumber.contains(q)
            }
        }
    }

    val registeredFriends = filteredContacts.filter { it.isRegisteredOnQueenChats }
    val unregisteredContacts = filteredContacts.filter { !it.isRegisteredOnQueenChats }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Contacts Screen Top App Bar
        Surface(
            color = chatColors.topBarBackground,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("contacts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = chatColors.topBarContent
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Contact",
                            color = chatColors.topBarContent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasPermission) "${phoneContacts.size} contacts found" else "Contacts Access",
                            color = chatColors.topBarContent.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }

                    // Search Button
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("contacts_search_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Contacts",
                            tint = chatColors.topBarContent
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            if (hasPermission) {
                                viewModel.loadAndSyncPhoneContacts()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("contacts_refresh_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WhatsAppGreenLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Contacts",
                                tint = chatColors.topBarContent
                            )
                        }
                    }
                }

                // Expanding Search Bar
                AnimatedVisibility(visible = isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setContactsSearchQuery(it) },
                        placeholder = { Text("Search by contact name or number...", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("contacts_search_input"),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setContactsSearchQuery("") }) {
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

        // Privacy Guarantee Banner
        Surface(
            color = chatColors.chatBackground.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WhatsAppGreenLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Protected: Contact matching happens locally on your device. Your full phone address book is never uploaded.",
                    fontSize = 11.sp,
                    color = chatColors.textSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        HorizontalDivider(color = chatColors.divider, thickness = 0.5.dp)

        if (!hasPermission) {
            // Permission Request State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contacts Permission",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Find Your Friends on Gueen Chats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = chatColors.textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Gueen Chats needs permission to access your contacts to show which of your phone contacts are already using Gueen Chats.",
                    fontSize = 13.sp,
                    color = chatColors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("request_contacts_permission_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Allow Contacts Access")
                }

                if (permissionDeniedExplanation) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permission was denied. Please allow contacts access to view your friends list.",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Contacts List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Section 1: Friends on Queen Chats
                if (registeredFriends.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FRIENDS ON GUEEN CHATS (${registeredFriends.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenLight,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    items(registeredFriends, key = { "reg_${it.id}" }) { contact ->
                        val maskedPhone = viewModel.phoneContactsRepository.formatMaskedPhoneNumber(contact.phoneNumber)
                        val targetChatId = contact.registeredUser?.id?.replace("user_", "chat_")
                            ?: "chat_${contact.id}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openOrCreateChatForContact(contact)
                                    onStartChat(targetChatId)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("registered_contact_${contact.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                name = contact.name,
                                avatarUrl = contact.registeredUser?.avatarUrl ?: contact.photoUri,
                                isGroup = false,
                                showOnlineBadge = true,
                                isOnline = contact.registeredUser?.isOnline ?: true,
                                size = 48.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = chatColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Registered",
                                        tint = WhatsAppGreenLight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.registeredUser?.statusBio ?: "Available on Gueen Chats",
                                    fontSize = 13.sp,
                                    color = chatColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = maskedPhone,
                                    fontSize = 11.sp,
                                    color = chatColors.textSecondary.copy(alpha = 0.7f)
                                )
                            }

                            // Start Chat Button
                            Button(
                                onClick = {
                                    viewModel.openOrCreateChatForContact(contact)
                                    onStartChat(targetChatId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("chat_button_${contact.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(
                            color = chatColors.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 78.dp)
                        )
                    }
                }

                // Section 2: Invite to Gueen Chats
                if (unregisteredContacts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INVITE TO GUEEN CHATS (${unregisteredContacts.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = chatColors.textSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    items(unregisteredContacts, key = { "unreg_${it.id}" }) { contact ->
                        val maskedPhone = viewModel.phoneContactsRepository.formatMaskedPhoneNumber(contact.phoneNumber)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("unregistered_contact_${contact.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                name = contact.name,
                                avatarUrl = contact.photoUri,
                                isGroup = false,
                                showOnlineBadge = false,
                                size = 48.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = chatColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = maskedPhone,
                                    fontSize = 12.sp,
                                    color = chatColors.textSecondary
                                )
                            }

                            // Invite Button
                            OutlinedButton(
                                onClick = {
                                    sendInviteToContact(context, contact)
                                },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("invite_button_${contact.id}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Invite", modifier = Modifier.size(13.dp), tint = WhatsAppGreenLight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite", fontSize = 12.sp, color = WhatsAppGreenLight)
                            }
                        }

                        HorizontalDivider(
                            color = chatColors.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 78.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

private fun sendInviteToContact(context: Context, contact: PhoneContact) {
    val inviteMessage = "Hey ${contact.name}! Let's chat on Gueen Chats. It offers encrypted real-time messages, voice notes, and video calls. Join me here: https://ais-pre-7ue44t5rgvpt75hxncvqly-303046257720.asia-southeast1.run.app"

    try {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${contact.phoneNumber}")
            putExtra("sms_body", inviteMessage)
        }
        if (smsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(smsIntent)
            return
        }
    } catch (_: Exception) {}

    // Fallback to standard chooser
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, inviteMessage)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Invite ${contact.name} to Gueen Chats"))
}
