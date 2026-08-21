package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["sha256Hash"], unique = true),
        Index(value = ["category"]),
        Index(value = ["timestamp"])
    ]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String? = null,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val category: String, // IMAGE, VIDEO, AUDIO, DOCUMENT
    val sizeBytes: Long,
    val durationMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sha256Hash: String,
    val senderName: String? = null,
    val isPreserved: Boolean = true,
    val backupLocation: String = "LOCAL_SECURE_STORAGE"
)
