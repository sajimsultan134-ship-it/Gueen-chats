package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun AvatarView(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    avatarUrl: String? = null,
    isGroup: Boolean = false,
    showOnlineBadge: Boolean = false,
    isOnline: Boolean = false,
    hasStory: Boolean = false,
    isStorySeen: Boolean = false
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val initialsColor = getAvatarColor(name)
    val ringColor = if (hasStory) {
        if (isStorySeen) Color.Gray.copy(alpha = 0.6f) else WhatsAppGreenLight
    } else Color.Transparent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = Modifier
            .size(if (hasStory) size - 6.dp else size)
            .let {
                if (hasStory) {
                    it.border(2.5.dp, ringColor, CircleShape)
                } else it
            }
            .clip(CircleShape)

        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$name's avatar",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } else {
            // High quality fallback avatar
            Box(
                modifier = imageModifier.background(initialsColor),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.5f)
                    )
                } else if (name == "Sajim Sultan" || name == "My Status") {
                    // Use generated app avatar or user icon
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.drawable.avatar_default_user)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size)
                    )
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.4f).sp
                    )
                }
            }
        }

        if (showOnlineBadge && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .background(WhatsAppGreenLight)
            )
        }
    }
}

fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF008069),
        Color(0xFF075E54),
        Color(0xFF128C7E),
        Color(0xFF1E88E5),
        Color(0xFF8E24AA),
        Color(0xFFD81B60),
        Color(0xFFFB8C00),
        Color(0xFF43A047),
        Color(0xFF00ACC1)
    )
    val index = (name.hashCode() and 0x7FFFFFFF) % colors.size
    return colors[index]
}
