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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusStory
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarView
import com.example.ui.components.formatTime
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun StatusScreen(
    viewModel: MainViewModel,
    stories: List<StatusStory>,
    onStoryClick: (StatusStory) -> Unit,
    onAddStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val myStory = stories.firstOrNull { it.userId == "user_me" }
    val contactStories = stories.filter { it.userId != "user_me" }
    val recentUpdates = contactStories.filter { !it.isSeen }
    val viewedUpdates = contactStories.filter { it.isSeen }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // My Status Item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStory != null) onStoryClick(myStory) else onAddStatusClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("my_status_item"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        AvatarView(
                            name = "My Status",
                            size = 54.dp,
                            hasStory = myStory != null,
                            isStorySeen = myStory?.isSeen ?: false
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(WhatsAppGreenLight)
                                .clickable { onAddStatusClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add status",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "My status",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = chatColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (myStory != null) formatTime(myStory.timestamp) else "Tap to add status update",
                            fontSize = 13.sp,
                            color = chatColors.textSecondary
                        )
                    }
                }
            }

            // Recent updates header
            if (recentUpdates.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chatColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(recentUpdates, key = { it.id }) { story ->
                    StoryItemRow(story = story, onClick = { onStoryClick(story) })
                    HorizontalDivider(
                        color = chatColors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }

            // Viewed updates header
            if (viewedUpdates.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Viewed updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chatColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(viewedUpdates, key = { it.id }) { story ->
                    StoryItemRow(story = story, onClick = { onStoryClick(story) })
                    HorizontalDivider(
                        color = chatColors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // WhatsApp Style Dual FABs for Status (Text pencil FAB + Camera FAB)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onAddStatusClick,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = chatColors.textPrimary,
                shape = CircleShape,
                modifier = Modifier.size(44.dp).testTag("fab_text_status")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Text Status",
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = onAddStatusClick,
                containerColor = WhatsAppGreenLight,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp).testTag("fab_camera_status")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Status",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun StoryItemRow(
    story: StatusStory,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("story_item_${story.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            name = story.userName,
            avatarUrl = story.userAvatarUrl,
            hasStory = true,
            isStorySeen = story.isSeen,
            size = 54.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = story.userName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = chatColors.textPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = formatTime(story.timestamp),
                fontSize = 13.sp,
                color = chatColors.textSecondary
            )
        }
    }
}
