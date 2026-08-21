package com.example.data.model

data class BackupStats(
    val totalMessages: Int = 0,
    val totalImages: Int = 0,
    val totalVideos: Int = 0,
    val totalAudio: Int = 0,
    val totalDocuments: Int = 0,
    val totalStorageBytes: Long = 0L,
    val totalConversations: Int = 0,
    val lastBackupTimestamp: Long = 0L,
    val isAutoBackupActive: Boolean = false
) {
    val formattedStorage: String
        get() {
            val kb = totalStorageBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$totalStorageBytes B"
            }
        }
}
