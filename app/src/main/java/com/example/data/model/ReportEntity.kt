package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String, // e.g. "RCS-REP-20260821-1234"
    val generatedTimestamp: Long = System.currentTimeMillis(),
    val title: String,
    val targetType: String, // ALL_CONVERSATIONS, SINGLE_CONVERSATION
    val conversationName: String,
    val contactPhone: String? = null,
    val filePath: String,
    val fileSizeBytes: Long = 0,
    val messageCount: Int = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val documentCount: Int = 0,
    val dateRangeText: String = "All Time",
    val status: String = "GENERATED"
)
