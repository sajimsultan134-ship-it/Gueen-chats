package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthState
import com.example.data.model.User
import com.example.ui.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ThemePreference
import com.example.ui.components.AvatarView
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    currentUser: User?,
    onOpenProfile: () -> Unit,
    onOpenFirebaseSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val themePref = viewModel.themePreference
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val authUser = (authState as? AuthState.Authenticated)?.user

    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // User Profile Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .testTag("user_profile_card"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(
                    name = currentUser?.name ?: authUser?.displayName ?: "Sajim Sultan",
                    avatarUrl = currentUser?.avatarUrl ?: authUser?.photoUrl,
                    size = 62.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.name ?: authUser?.displayName ?: "Sajim Sultan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = chatColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = currentUser?.statusBio ?: "Building Sajim Chat on Gueen Chats 🚀",
                        fontSize = 13.sp,
                        color = chatColors.textSecondary,
                        maxLines = 1
                    )
                    if (!authUser?.email.isNullOrBlank()) {
                        Text(
                            text = authUser!!.email!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WhatsAppGreenLight
                        )
                    } else {
                        Text(
                            text = currentUser?.phoneNumber ?: "+1 555-0134",
                            fontSize = 12.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onOpenProfile) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        tint = WhatsAppGreenLight
                    )
                }
            }

            HorizontalDivider(
                color = chatColors.divider,
                thickness = 0.5.dp
            )
        }

        // Settings items
        item {
            SettingsCategoryRow(
                icon = Icons.Default.Lock,
                title = "Privacy",
                subtitle = "Last seen, profile photo, read receipts",
                onClick = {}
            )
            SettingsCategoryRow(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Chats",
                subtitle = "Theme, wallpapers, chat history",
                onClick = {}
            )

            // Dark Mode toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = if (themePref.value == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
                        viewModel.setThemePreference(next)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Theme",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dark Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = chatColors.textPrimary
                    )
                    Text(
                        text = if (themePref.value == ThemePreference.DARK) "On (WhatsApp Dark Charcoal)" else "Off (WhatsApp Emerald Light)",
                        fontSize = 12.sp,
                        color = chatColors.textSecondary
                    )
                }

                Switch(
                    checked = themePref.value == ThemePreference.DARK,
                    onCheckedChange = { isDark ->
                        viewModel.setThemePreference(if (isDark) ThemePreference.DARK else ThemePreference.LIGHT)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreenLight
                    )
                )
            }

            // Read Receipts toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { readReceiptsEnabled = !readReceiptsEnabled }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Receipts",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Read receipts",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = chatColors.textPrimary
                    )
                    Text(
                        text = "If turned off, you won't send or receive blue checkmarks",
                        fontSize = 12.sp,
                        color = chatColors.textSecondary
                    )
                }

                Switch(
                    checked = readReceiptsEnabled,
                    onCheckedChange = { readReceiptsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreenLight
                    )
                )
            }

            // Notifications row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { notificationsEnabled = !notificationsEnabled }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = chatColors.textPrimary
                    )
                    Text(
                        text = "Message tones, vibration, priority alerts",
                        fontSize = 12.sp,
                        color = chatColors.textSecondary
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsAppGreenLight
                    )
                )
            }

            // Account & Firebase Authentication
            SettingsCategoryRow(
                icon = Icons.Default.AccountCircle,
                title = "Account & Authentication",
                subtitle = "Switch account, Register, Login with Firebase Auth",
                onClick = { viewModel.setShowAuth(true) }
            )

            // Firebase Backend Guide Row
            SettingsCategoryRow(
                icon = Icons.Default.CloudDone,
                title = "Firebase & Cloud Sync Guide",
                subtitle = "Firestore schema, Auth, Storage rules & WebRTC",
                onClick = onOpenFirebaseSetup
            )

            // Help & About
            SettingsCategoryRow(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "App Info & Version",
                subtitle = "Gueen Chats v1.0 • Sajim Chat • End-to-end encrypted",
                onClick = {}
            )

            // Log Out Button
            SettingsCategoryRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log Out",
                subtitle = "Sign out from this device",
                onClick = { viewModel.logoutUser() }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsCategoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = WhatsAppGreenLight,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = chatColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = chatColors.textSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = chatColors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
