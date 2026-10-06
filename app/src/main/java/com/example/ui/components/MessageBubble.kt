package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppCheckBlue
import com.example.ui.theme.WhatsAppGreenLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isGroup: Boolean,
    isAudioPlaying: Boolean,
    audioProgress: Float,
    onToggleAudio: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit = {},
    onDocumentClick: (Message) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val isOut = message.isOutgoing

    val bubbleColor = if (isOut) chatColors.bubbleOutgoing else chatColors.bubbleIncoming
    val bubbleShape = if (isOut) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val alignment = if (isOut) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 320.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .testTag("message_bubble_${message.id}")
        ) {
            Column {
                // Sender name in group chat
                if (isGroup && !isOut) {
                    Text(
                        text = message.senderName,
                        color = getAvatarColor(message.senderName),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Message content depending on type
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = chatColors.textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }

                    MessageType.VOICE -> {
                        VoiceNoteContent(
                            durationSec = message.mediaDurationSec,
                            isPlaying = isAudioPlaying,
                            progress = audioProgress,
                            onTogglePlay = onToggleAudio,
                            isOutgoing = isOut
                        )
                    }

                    MessageType.IMAGE -> {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .clickable {
                                        onImageClick(message.mediaUri ?: "")
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(
                                            if (message.mediaUri?.startsWith("drawable://") == true || message.mediaUri == null) {
                                                R.drawable.chat_wallpaper
                                            } else {
                                                message.mediaUri
                                            }
                                        )
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Shared image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (message.text.isNotBlank() && message.text != "Photo") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = chatColors.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    MessageType.DOCUMENT -> {
                        val extension = message.mediaFileName?.substringAfterLast(".", "")?.uppercase() ?: "FILE"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { onDocumentClick(message) }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        when (extension) {
                                            "PDF" -> Color(0xFFE53935).copy(alpha = 0.15f)
                                            "DOC", "DOCX" -> Color(0xFF1E88E5).copy(alpha = 0.15f)
                                            "XLS", "XLSX" -> Color(0xFF43A047).copy(alpha = 0.15f)
                                            else -> WhatsAppGreenLight.copy(alpha = 0.2f)
                                        },
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Document",
                                        tint = when (extension) {
                                            "PDF" -> Color(0xFFE53935)
                                            "DOC", "DOCX" -> Color(0xFF1E88E5)
                                            "XLS", "XLSX" -> Color(0xFF43A047)
                                            else -> WhatsAppGreenLight
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = extension.take(4),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chatColors.textPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.mediaFileName ?: "Document",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = chatColors.textPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = message.mediaFileSize ?: "Document",
                                    fontSize = 11.sp,
                                    color = chatColors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { onDocumentClick(message) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Open Document",
                                    tint = WhatsAppGreenLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    MessageType.VIDEO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onVideoClick(message.mediaUri ?: "")
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFF1F2C34)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (message.mediaDurationSec > 0) {
                                                String.format(Locale.US, "%02d:%02d", message.mediaDurationSec / 60, message.mediaDurationSec % 60)
                                            } else "Video",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (message.text.isNotBlank() && message.text != "Video") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = chatColors.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Time and Status ticks row
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 11.sp,
                        color = chatColors.textSecondary
                    )
                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusIndicator(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNoteContent(
    durationSec: Int,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    isOutgoing: Boolean
) {
    val chatColors = LocalChatColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isOutgoing) WhatsAppGreenLight else WhatsAppGreenLight.copy(alpha = 0.85f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Waveform simulation bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(14, 22, 10, 26, 18, 12, 28, 20, 16, 24, 8, 20, 14, 22, 10, 18)
                heights.forEachIndexed { index, h ->
                    val barProgress = index.toFloat() / heights.size.toFloat()
                    val isPast = progress >= barProgress
                    val barColor = if (isPast) WhatsAppGreenLight else chatColors.textSecondary.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WhatsAppGreenLight,
                trackColor = chatColors.textSecondary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val elapsed = (progress * durationSec).toInt()
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", elapsed / 60, elapsed % 60),
                    fontSize = 11.sp,
                    color = chatColors.textSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice message",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60),
                        fontSize = 11.sp,
                        color = chatColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Sending",
                tint = Color.Gray,
                modifier = Modifier.size(13.dp)
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Sent",
                tint = Color.Gray,
                modifier = Modifier.size(15.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = Color.Gray,
                modifier = Modifier.size(15.dp)
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = WhatsAppCheckBlue,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
