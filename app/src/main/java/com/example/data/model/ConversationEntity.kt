package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val contactName: String,
    val contactPhone: String? = null,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val mediaCount: Int = 0,
    val avatarColorHex: String = "#00897B",
    val appSource: String = "Google Messages"
)
