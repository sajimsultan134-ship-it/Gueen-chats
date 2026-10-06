package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    DOCUMENT,
    VIDEO
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String = "+1 555-0199",
    val statusBio: String = "Hey there! I am using Gueen Chats.",
    val avatarUrl: String? = null,
    val isOnline: Boolean = true,
    val lastSeenText: String = "online",
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean = false,
    val avatarUrl: String? = null,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeenText: String = "recently",
    val isPinned: Boolean = false,
    val participantIdsCsv: String = "" // comma separated user IDs
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val mediaDurationSec: Int = 0,
    val mediaFileName: String? = null,
    val mediaFileSize: String? = null,
    val isOutgoing: Boolean = true
)

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val contactAvatarUrl: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val isMissed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

data class StatusStory(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val caption: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false,
    val colorHex: Long = 0xFF008069
)

data class CallState(
    val activeCallRecord: CallRecord? = null,
    val isInCall: Boolean = false,
    val isIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val callDurationSec: Int = 0
)
