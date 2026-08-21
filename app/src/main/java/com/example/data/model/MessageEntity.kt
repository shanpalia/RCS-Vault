package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["senderPhone"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val senderName: String,
    val senderPhone: String? = null,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appSource: String = "com.google.android.apps.messaging",
    val isIncoming: Boolean = true,
    val hasAttachment: Boolean = false,
    val mediaType: String? = null, // IMAGE, VIDEO, AUDIO, DOCUMENT
    val mediaUri: String? = null,
    val mediaFileName: String? = null,
    val mediaSizeBytes: Long = 0,
    val isRecovered: Boolean = false,
    val backupStatus: String = "BACKED_UP" // BACKED_UP, ARCHIVED, PRESERVED
)
