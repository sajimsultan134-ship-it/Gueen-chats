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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.components.formatTime
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun CallsScreen(
    viewModel: MainViewModel,
    calls: List<CallRecord>,
    onStartCall: (String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Create Call Link Item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Create call link */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(WhatsAppGreenLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Create call link",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Create call link",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = chatColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share a link for your Gueen Chats call",
                            fontSize = 13.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }
            }

            // Recent Calls Header
            item {
                Text(
                    text = "Recent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = chatColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (calls.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = chatColors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No calls yet",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }
            } else {
                items(calls, key = { it.id }) { call ->
                    CallLogItemRow(
                        call = call,
                        onCallClick = { onStartCall(call.contactId, call.contactName, call.isVideo) }
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

        // WhatsApp Floating Action Button for Calls
        FloatingActionButton(
            onClick = {
                // Quick start call with first contact
                onStartCall("user_sarah", "Sarah Connor", false)
            },
            containerColor = WhatsAppGreenLight,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .testTag("fab_new_call")
        ) {
            Icon(
                imageVector = Icons.Default.AddIcCall,
                contentDescription = "New Call",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun CallLogItemRow(
    call: CallRecord,
    onCallClick: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("call_item_${call.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            name = call.contactName,
            avatarUrl = call.contactAvatarUrl,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (call.isMissed) CallRed else chatColors.textPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (call.isMissed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                        contentDescription = "Missed",
                        tint = CallRed,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (call.isIncoming) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = "Incoming",
                        tint = CallGreen,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = "Outgoing",
                        tint = CallGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${formatTime(call.timestamp)}${if (call.durationSeconds > 0) " (${call.durationSeconds}s)" else ""}",
                    fontSize = 13.sp,
                    color = chatColors.textSecondary
                )
            }
        }

        IconButton(
            onClick = onCallClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = if (call.isVideo) "Video Call" else "Audio Call",
                tint = WhatsAppGreenLight,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
