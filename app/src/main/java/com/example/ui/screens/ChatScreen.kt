package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.util.RecordingState
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.components.MessageBubble
import com.example.ui.theme.CallRed
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    chat: Chat,
    messages: List<Message>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Hardware and gesture back navigation
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val chatColors = LocalChatColors.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMicPermissionDeniedDialog by remember { mutableStateOf(false) }

    // Live typing state
    val typingMap by viewModel.typingChats.collectAsStateWithLifecycle()
    val isPartnerTyping = typingMap.containsKey(chat.id)
    val typingAuthor = typingMap[chat.id]

    // Voice recording & preview states
    val recordingState by viewModel.voiceRecorder.recordingState.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.voiceRecorder.recordingDurationSec.collectAsStateWithLifecycle()
    val recordingAmplitudes by viewModel.voiceRecorder.recordingAmplitudes.collectAsStateWithLifecycle()
    val isPreviewPlaying by viewModel.voiceRecorder.isPreviewPlaying.collectAsStateWithLifecycle()
    val previewProgress by viewModel.voiceRecorder.previewProgress.collectAsStateWithLifecycle()

    // Voice message playback state in chat history
    val playingMessageId by viewModel.voicePlayer.currentlyPlayingId.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.voicePlayer.playbackProgress.collectAsStateWithLifecycle()

    // Pulsing animation for the recording red dot
    val pulseAlpha = remember { Animatable(1f) }
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            pulseAlpha.animateTo(
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(1f)
        }
    }

    // Permission launcher for microphone recording
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        } else {
            showMicPermissionDeniedDialog = true
        }
    }

    // Photo / Image Media Picker (Zero-permission Android Photo Picker)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendImageFromUri(chat.id, uri)
        }
    }

    // Video Media Picker (Zero-permission Android Photo Picker for Videos)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendVideoFromUri(chat.id, uri)
        }
    }

    // Document / File Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendDocumentFromUri(chat.id, uri)
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-scroll to latest message when messages list changes
    LaunchedEffect(messages.size, isPartnerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Connect real-time Firestore sync & clear unread messages on entering this conversation
    LaunchedEffect(chat.id) {
        viewModel.selectChat(chat.id)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(chatColors.chatBackground)
    ) {
        // WhatsApp Top Header for 1-to-1 Chat
        Surface(
            color = chatColors.topBarBackground,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = chatColors.topBarContent
                    )
                }

                // Partner Avatar
                AvatarView(
                    name = chat.name,
                    avatarUrl = chat.avatarUrl,
                    isGroup = chat.isGroup,
                    showOnlineBadge = true,
                    isOnline = chat.isOnline,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Partner Name & Presence Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setShowProfile(true) }
                ) {
                    Text(
                        text = chat.name,
                        color = chatColors.topBarContent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.testTag("chat_title_name")
                    )

                    Text(
                        text = when {
                            isPartnerTyping -> if (chat.isGroup) "$typingAuthor is typing…" else "typing…"
                            chat.isOnline -> "online"
                            else -> chat.lastSeenText
                        },
                        color = if (isPartnerTyping || chat.isOnline) WhatsAppGreenLight else chatColors.topBarContent.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = if (isPartnerTyping || chat.isOnline) FontWeight.Medium else FontWeight.Normal
                    )
                }

                // Audio Call Action
                IconButton(
                    onClick = {
                        viewModel.startCall(
                            contactId = chat.id,
                            contactName = chat.name,
                            isVideo = false
                        )
                    },
                    modifier = Modifier.size(42.dp).testTag("action_audio_call")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = chatColors.topBarContent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Video Call Action
                IconButton(
                    onClick = {
                        viewModel.startCall(
                            contactId = chat.id,
                            contactName = chat.name,
                            isVideo = true
                        )
                    },
                    modifier = Modifier.size(42.dp).testTag("action_video_call")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = chatColors.topBarContent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Menu Options
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(40.dp).testTag("chat_more_options")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = chatColors.topBarContent
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View contact") },
                            onClick = {
                                menuExpanded = false
                                viewModel.setShowProfile(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Media, links, and docs") },
                            onClick = { menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Search in conversation") },
                            onClick = { menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete chat", color = CallRed) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = CallRed)
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.deleteChat(chat.id)
                                onBack()
                            }
                        )
                    }
                }
            }
        }

        // Chat Message History with WhatsApp Wallpaper
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Wallpaper Background
            Image(
                painter = painterResource(id = R.drawable.chat_wallpaper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.08f)
            )

            if (messages.isEmpty()) {
                // Empty State with Encryption Notice
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chatColors.chatBackground.copy(alpha = 0.9f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Messages and calls are end-to-end encrypted. No one outside of this chat, not even Queen Chats, can read or listen to them.",
                                fontSize = 12.sp,
                                color = chatColors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Say hello to start the conversation! 👋",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = chatColors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        // Top Encryption Info Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E2A30).copy(alpha = 0.85f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD279),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "End-to-end encrypted",
                                    color = Color(0xFFFFD279),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        val isPlaying = playingMessageId == message.id
                        MessageBubble(
                            message = message,
                            isGroup = chat.isGroup,
                            isAudioPlaying = isPlaying,
                            audioProgress = if (isPlaying) playbackProgress else 0f,
                            onToggleAudio = {
                                viewModel.voicePlayer.togglePlay(
                                    messageId = message.id,
                                    audioUri = message.mediaUri,
                                    totalDurationSec = message.mediaDurationSec
                                )
                            },
                            onImageClick = { uri ->
                                viewModel.viewEnlargedImage(uri)
                            }
                        )
                    }

                    if (isPartnerTyping) {
                        item {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chatColors.bubbleIncoming)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (chat.isGroup) "$typingAuthor is typing…" else "typing…",
                                        fontSize = 13.sp,
                                        color = WhatsAppGreenLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // WhatsApp Floating Input Bar Area
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Capsule
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    when (recordingState) {
                        RecordingState.RECORDING -> {
                            // Active Voice Recording UI
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsing red dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .alpha(pulseAlpha.value)
                                        .background(CallRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                // Live timer
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%02d:%02d",
                                        recordingDuration / 60,
                                        recordingDuration % 60
                                    ),
                                    color = CallRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Live dynamic waveform amplitude bars
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val amps = if (recordingAmplitudes.isNotEmpty()) recordingAmplitudes.takeLast(20) else listOf(0.3f, 0.5f, 0.7f, 0.4f)
                                    amps.forEach { amp ->
                                        val barHeight = (amp * 24).coerceIn(4f, 26f).dp
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(WhatsAppGreenLight)
                                        )
                                    }
                                }

                                // Cancel / Trash button
                                IconButton(
                                    onClick = { viewModel.cancelVoiceRecording() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("cancel_voice_recording")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Cancel Recording",
                                        tint = CallRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Stop to Preview button
                                IconButton(
                                    onClick = { viewModel.pauseOrStopVoiceRecordingToPreview() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("stop_preview_voice_recording")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop and Preview",
                                        tint = WhatsAppGreenLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        RecordingState.PREVIEW -> {
                            // Playback Preview Before Sending UI
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Pause preview button
                                IconButton(
                                    onClick = { viewModel.toggleVoicePreviewPlayback() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(WhatsAppGreenLight.copy(alpha = 0.15f), CircleShape)
                                        .testTag("play_preview_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewPlaying) "Pause" else "Play",
                                        tint = WhatsAppGreenLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Audio progress bar
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { previewProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = WhatsAppGreenLight,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%02d:%02d",
                                            recordingDuration / 60,
                                            recordingDuration % 60
                                        ),
                                        fontSize = 10.sp,
                                        color = chatColors.textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Discard preview
                                IconButton(
                                    onClick = { viewModel.cancelVoiceRecording() },
                                    modifier = Modifier.size(36.dp).testTag("delete_preview_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Discard Voice Note",
                                        tint = CallRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        RecordingState.IDLE -> {
                            // Standard Text Input Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        inputText += " 😊 "
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mood,
                                        contentDescription = "Emojis",
                                        tint = Color.Gray
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Message",
                                            color = Color.Gray,
                                            fontSize = 15.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("message_input_field"),
                                    singleLine = false,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = WhatsAppGreenLight
                                    )
                                )

                                // Attach File Button
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.size(36.dp).testTag("button_attach")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attach file",
                                        tint = Color.Gray
                                    )
                                }

                                // Camera Quick Button
                                IconButton(
                                    onClick = {
                                        viewModel.sendImageMessage(
                                            chatId = chat.id,
                                            sampleImageResOrUri = "https://images.unsplash.com/photo-1579202673506-ca3ce28943ef"
                                        )
                                    },
                                    modifier = Modifier.size(36.dp).testTag("button_camera")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Circular WhatsApp Send or Mic Action Button
                FloatingActionButton(
                    onClick = {
                        when {
                            recordingState == RecordingState.RECORDING || recordingState == RecordingState.PREVIEW -> {
                                viewModel.sendVoiceMessage(chat.id)
                            }
                            inputText.isNotBlank() -> {
                                viewModel.sendTextMessage(chat.id, inputText.trim())
                                inputText = ""
                            }
                            else -> {
                                handleMicClick()
                            }
                        }
                    },
                    containerColor = WhatsAppGreenLight,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("button_send_or_mic")
                ) {
                    Icon(
                        imageVector = when {
                            recordingState == RecordingState.RECORDING || recordingState == RecordingState.PREVIEW -> Icons.AutoMirrored.Filled.Send
                            inputText.isNotBlank() -> Icons.AutoMirrored.Filled.Send
                            else -> Icons.Default.Mic
                        },
                        contentDescription = if (inputText.isNotBlank() || recordingState != RecordingState.IDLE) "Send" else "Record Voice Note",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Microphone Permission Denied Helper Dialog
    if (showMicPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDeniedDialog = false },
            title = {
                Text(
                    text = "Microphone Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Queen Chats needs microphone access to record and send WhatsApp-style voice messages to your contacts. Please allow microphone access.",
                    fontSize = 14.sp,
                    color = chatColors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicPermissionDeniedDialog = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDeniedDialog = false }) {
                    Text("Cancel", color = chatColors.textSecondary)
                }
            }
        )
    }

    // Media & Attachment Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Share Content",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = chatColors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        label = "Gallery",
                        color = Color(0xFFAC44CF),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendImageMessage(
                                chatId = chat.id,
                                sampleImageResOrUri = "https://images.unsplash.com/photo-1579202673506-ca3ce28943ef"
                            )
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Description,
                        label = "Document",
                        color = Color(0xFF5F66CD),
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendDocumentMessage(
                                chatId = chat.id,
                                fileName = "Project_Requirements.pdf",
                                fileSize = "1.4 MB"
                            )
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Mic,
                        label = "Audio Note",
                        color = Color(0xFFE56338),
                        onClick = {
                            showAttachmentSheet = false
                            handleMicClick()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
