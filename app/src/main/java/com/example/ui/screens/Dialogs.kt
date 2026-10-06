package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.AvatarView
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    contacts: List<User>,
    onSelectContact: (User) -> Unit,
    onNewGroupClick: () -> Unit,
    onOpenContacts: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val chatColors = LocalChatColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Select contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = chatColors.textPrimary
                    )
                    Text(
                        text = "${contacts.filter { !it.isCurrentUser }.size} contacts",
                        fontSize = 12.sp,
                        color = chatColors.textSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = chatColors.textSecondary)
                }
            }

            HorizontalDivider(color = chatColors.divider, thickness = 0.5.dp)

            // Phone Contacts Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenContacts()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("action_phone_contacts"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Phone Contacts",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Phone Contacts",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = chatColors.textPrimary
                    )
                    Text(
                        text = "Find registered friends & invite contacts",
                        fontSize = 12.sp,
                        color = chatColors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = chatColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))

            // New Group Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onNewGroupClick()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(WhatsAppGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "New group",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "New group",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = chatColors.textPrimary
                )
            }

            HorizontalDivider(color = chatColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(contacts.filter { !it.isCurrentUser }, key = { it.id }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectContact(contact)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarView(
                            name = contact.name,
                            avatarUrl = contact.avatarUrl,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = chatColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = contact.statusBio,
                                fontSize = 13.sp,
                                color = chatColors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewGroupDialog(
    contacts: List<User>,
    onCreateGroup: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }
    val chatColors = LocalChatColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New Group", fontWeight = FontWeight.Bold, color = chatColors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Subject / Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Add participants (${selectedUserIds.size} selected):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = chatColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(contacts.filter { !it.isCurrentUser }, key = { it.id }) { contact ->
                        val isSelected = selectedUserIds.contains(contact.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedUserIds.remove(contact.id) else selectedUserIds.add(contact.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedUserIds.add(contact.id) else selectedUserIds.remove(contact.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = WhatsAppGreenLight)
                            )
                            AvatarView(name = contact.name, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = contact.name,
                                fontSize = 14.sp,
                                color = chatColors.textPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && selectedUserIds.isNotEmpty()) {
                        onCreateGroup(groupName, selectedUserIds.toList())
                    }
                },
                enabled = groupName.isNotBlank() && selectedUserIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = chatColors.textSecondary)
            }
        }
    )
}

@Composable
fun UserProfileDialog(
    currentUser: User?,
    onSave: (name: String, status: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentUser?.name ?: "Sajim Sultan") }
    var status by remember { mutableStateOf(currentUser?.statusBio ?: "Building Sajim Chat on Gueen Chats 🚀") }
    var phone by remember { mutableStateOf(currentUser?.phoneNumber ?: "+1 555-0134") }
    val chatColors = LocalChatColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Profile", fontWeight = FontWeight.Bold, color = chatColors.textPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarView(
                        name = name,
                        avatarUrl = currentUser?.avatarUrl,
                        size = 80.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("About / Status") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, status, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = chatColors.textSecondary)
            }
        }
    )
}

@Composable
fun AddStatusDialog(
    onAddStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val chatColors = LocalChatColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Status Story", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Share what's on your mind with your contacts (disappears after 24h):",
                    fontSize = 13.sp,
                    color = chatColors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Type a status…") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddStatus(caption) },
                enabled = caption.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
            ) {
                Text("Post Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = chatColors.textSecondary)
            }
        }
    )
}

@Composable
fun FirebaseGuideDialog(
    syncStatus: com.example.data.sync.SyncStatus,
    onConfigureFirebase: (projectId: String, appId: String, apiKey: String, (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val chatColors = LocalChatColors.current
    var projectIdInput by remember { mutableStateOf("") }
    var appIdInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var connectMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (syncStatus.isCloudConnected) WhatsAppGreenLight else Color(0xFFE5A93C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firebase Firestore Real-time Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Live Sync Status Card
                Surface(
                    color = if (syncStatus.isCloudConnected) WhatsAppGreenLight.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (syncStatus.isCloudConnected) WhatsAppGreenLight else Color(0xFFE5A93C))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (syncStatus.isCloudConnected) "STATUS: FIREBASE CONNECTED" else "STATUS: ROOM OFFLINE-FIRST MODE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (syncStatus.isCloudConnected) WhatsAppGreenLight else Color(0xFFE5A93C)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = syncStatus.statusMessage,
                            fontSize = 12.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }

                // Interactive Dynamic Connector
                Text(
                    text = "Connect Project (Dynamic Initialization):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = chatColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your Firebase credentials to connect live without rebuilding:",
                    fontSize = 11.sp,
                    color = chatColors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = projectIdInput,
                    onValueChange = { projectIdInput = it },
                    label = { Text("Firebase Project ID (e.g. my-sajim-chat)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = appIdInput,
                    onValueChange = { appIdInput = it },
                    label = { Text("Application ID (e.g. 1:12345:android:abcde)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key (e.g. AIzaSy...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (projectIdInput.isNotBlank() && appIdInput.isNotBlank() && apiKeyInput.isNotBlank()) {
                            isConnecting = true
                            connectMessage = "Connecting..."
                            onConfigureFirebase(projectIdInput, appIdInput, apiKeyInput) { success, msg ->
                                isConnecting = false
                                connectMessage = msg
                            }
                        } else {
                            connectMessage = "Please enter Project ID, App ID, and API Key"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect & Start Syncing")
                }

                if (connectMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = connectMessage!!,
                        fontSize = 11.sp,
                        color = if (syncStatus.isCloudConnected) WhatsAppGreenLight else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = chatColors.divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Manual Configuration with google-services.json
                Text(
                    text = "Manual Setup Option (google-services.json):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = WhatsAppGreenLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Go to Firebase Console > Project Settings > General.\n" +
                            "2. Add Android app with package: com.aistudio.gueenchats.sajim\n" +
                            "3. Download google-services.json and save to /app/google-services.json\n" +
                            "4. (A sample template is provided at /app/google-services.json.template)\n",
                    fontSize = 11.sp,
                    color = chatColors.textSecondary
                )

                Text(
                    text = "Cloud Firestore Security Rules:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = chatColors.textPrimary
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "rules_version = '2';\nservice cloud.firestore {\n  match /databases/{db}/documents {\n    match /chats/{chatId} {\n      allow read, write: if request.auth != null;\n      match /messages/{msgId} {\n        allow read, write: if request.auth != null;\n      }\n    }\n  }\n}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Text(
                    text = "Firebase Storage Rules (Voice Notes & Photos):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = chatColors.textPrimary
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "service firebase.storage {\n  match /b/{bucket}/o {\n    match /voice_notes/{allPaths=**} {\n      allow read, write: if request.auth != null;\n    }\n    match /media/{allPaths=**} {\n      allow read, write: if request.auth != null;\n    }\n  }\n}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
            ) {
                Text("Close")
            }
        }
    )
}
