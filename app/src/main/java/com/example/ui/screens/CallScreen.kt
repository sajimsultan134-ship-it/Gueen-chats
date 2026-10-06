package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallState
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import java.util.Locale

@Composable
fun CallScreen(
    viewModel: MainViewModel,
    callState: CallState,
    modifier: Modifier = Modifier
) {
    val record = callState.activeCallRecord ?: return
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(callState.isIncoming) {
        if (callState.isIncoming) {
            pulseScale.animateTo(
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    val bgGradient = if (callState.isVideoEnabled && record.isVideo) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E3C40),
                Color(0xFF0F1E21),
                Color(0xFF060D0E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF075E54),
                Color(0xFF0F2624),
                Color(0xFF0B141A)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .testTag("call_screen_container")
    ) {
        // Video Preview Simulation (if video call and enabled)
        if (callState.isVideoEnabled && record.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1B2A2F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarView(
                        name = record.contactName,
                        avatarUrl = record.contactAvatarUrl,
                        size = 120.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "WebRTC Video Stream Connected",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }

                // Self Video Preview (Picture in picture in top corner)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 20.dp)
                        .size(width = 100.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C3E44))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarView(
                        name = "Sajim Sultan",
                        size = 46.dp
                    )
                    Text(
                        text = "You",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Top Caller Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gueen Chats ${if (record.isVideo) "Video" else "Voice"}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.contactName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusText = when {
                callState.isIncoming -> "Incoming call…"
                callState.callDurationSec == 0 -> "Calling…"
                else -> {
                    val m = callState.callDurationSec / 60
                    val s = callState.callDurationSec % 60
                    String.format(Locale.getDefault(), "%02d:%02d", m, s)
                }
            }

            Text(
                text = statusText,
                color = if (callState.isIncoming) CallGreen else Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Center Avatar for voice call
        if (!record.isVideo || !callState.isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(pulseScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing rings
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                AvatarView(
                    name = record.contactName,
                    avatarUrl = record.contactAvatarUrl,
                    size = 110.dp
                )
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
        ) {
            if (callState.isIncoming) {
                // Incoming Call Action Buttons: Decline (Red) and Accept (Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { viewModel.endCall() },
                            containerColor = CallRed,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp).testTag("decline_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = Color.White, fontSize = 13.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { viewModel.answerCall() },
                            containerColor = CallGreen,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp).testTag("accept_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", color = Color.White, fontSize = 13.sp)
                    }
                }
            } else {
                // Active Call Controls Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleSpeaker() },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (callState.isSpeakerOn) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                                contentDescription = "Speaker",
                                tint = Color.White
                            )
                        }

                        if (record.isVideo) {
                            IconButton(
                                onClick = { viewModel.toggleVideo() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (!callState.isVideoEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Video",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (callState.isMuted) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }

                        FloatingActionButton(
                            onClick = { viewModel.endCall() },
                            containerColor = CallRed,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp).testTag("end_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
