package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_events")
data class BackupEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // AUTO_SYNC, NOTIFICATION_CAPTURED, MEDIA_PRESERVED, REPORT_GENERATED, MANUAL_BACKUP
    val summary: String,
    val itemCount: Int = 1,
    val status: String = "SUCCESS" // SUCCESS, WARNING, ERROR
)
