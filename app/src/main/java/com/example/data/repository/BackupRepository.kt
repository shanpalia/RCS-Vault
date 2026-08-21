package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.BackupStats
import com.example.data.model.ConversationEntity
import com.example.data.model.MediaFileEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ReportEntity
import com.example.data.preferences.BackupPreferences
import com.example.service.MediaBackupManager
import com.example.util.BackupExporter
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

class BackupRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    val preferences = BackupPreferences(context)
    val mediaManager = MediaBackupManager(context)
    val pdfGenerator = PdfReportGenerator(context)
    val exporter = BackupExporter(context)

    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val mediaDao = database.mediaDao()
    private val eventDao = database.backupEventDao()
    private val reportDao = database.reportDao()

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allMedia: Flow<List<MediaFileEntity>> = mediaDao.getAllMedia()
    val recentEvents: Flow<List<BackupEventEntity>> = eventDao.getRecentEvents()
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    fun getMediaForConversation(conversationId: String): Flow<List<MediaFileEntity>> =
        mediaDao.getMediaForConversation(conversationId)

    fun getMediaByCategory(category: String): Flow<List<MediaFileEntity>> =
        if (category == "ALL") mediaDao.getAllMedia() else mediaDao.getMediaByCategory(category)

    fun searchAll(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.searchConversations(query)

    fun searchMedia(query: String): Flow<List<MediaFileEntity>> =
        mediaDao.searchMedia(query)

    /**
     * Combined Flow for Live Dashboard Statistics
     */
    val backupStats: Flow<BackupStats> = combine(
        messageDao.getMessageCount(),
        mediaDao.getCountByCategory("IMAGE"),
        mediaDao.getCountByCategory("VIDEO"),
        mediaDao.getCountByCategory("AUDIO"),
        mediaDao.getCountByCategory("DOCUMENT"),
        mediaDao.getTotalStorageUsed(),
        conversationDao.getAllConversations()
    ) { args: Array<Any?> ->
        val msgCount = (args[0] as? Int) ?: 0
        val imgCount = (args[1] as? Int) ?: 0
        val vidCount = (args[2] as? Int) ?: 0
        val audCount = (args[3] as? Int) ?: 0
        val docCount = (args[4] as? Int) ?: 0
        val storageBytes = (args[5] as? Long) ?: 0L
        @Suppress("UNCHECKED_CAST")
        val conversations = (args[6] as? List<ConversationEntity>) ?: emptyList()

        BackupStats(
            totalMessages = msgCount,
            totalImages = imgCount,
            totalVideos = vidCount,
            totalAudio = audCount,
            totalDocuments = docCount,
            totalStorageBytes = storageBytes,
            totalConversations = conversations.size,
            lastBackupTimestamp = preferences.lastBackupTime,
            isAutoBackupActive = preferences.isAutoBackupEnabled
        )
    }

    suspend fun toggleAutoBackup(enabled: Boolean) {
        preferences.isAutoBackupEnabled = enabled
        eventDao.insertEvent(
            BackupEventEntity(
                eventType = "STATUS_CHANGE",
                summary = if (enabled) "Auto Backup activated" else "Auto Backup paused by user",
                status = "SUCCESS"
            )
        )
    }

    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    suspend fun deleteMessage(id: Long) = messageDao.deleteMessageById(id)

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
        messageDao.deleteConversationMessages(conversationId)
    }

    suspend fun deleteMedia(media: MediaFileEntity) {
        try {
            val file = File(media.filePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaDao.deleteById(media.id)
    }

    suspend fun deleteReport(report: ReportEntity) {
        try {
            val file = File(report.filePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        reportDao.deleteReportById(report.id)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        messageDao.clearAll()
        conversationDao.clearAll()
        mediaDao.clearAll()
        reportDao.clearAll()
        eventDao.clearAll()

        // Clean directory files
        try {
            mediaManager.backupDirectory.deleteRecursively()
            File(context.filesDir, "rcs_backup").deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        preferences.lastBackupTime = 0L
        preferences.totalCapturedCount = 0

        eventDao.insertEvent(
            BackupEventEntity(
                eventType = "CLEAR_DATA",
                summary = "All local backup data and media files cleared",
                status = "WARNING"
            )
        )
    }

    /**
     * DEBUG ONLY: Test data generator for developers.
     * Never runs automatically. Only callable manually in DEBUG mode.
     */
    suspend fun generateDebugTestData() = withContext(Dispatchers.IO) {
        if (!com.example.BuildConfig.DEBUG) return@withContext

        val now = System.currentTimeMillis()
        val convId = "debug_test_conv"
        conversationDao.insertOrUpdate(
            ConversationEntity(
                conversationId = convId,
                contactName = "[TEST DATA] Dev Simulator",
                contactPhone = "+1 (555) 000-0000",
                lastMessage = "[TEST DATA] Simulated RCS diagnostic message",
                lastTimestamp = now,
                messageCount = 1,
                mediaCount = 0,
                avatarColorHex = "#38B2AC",
                appSource = "RCS Debug Tool"
            )
        )

        messageDao.insertMessage(
            MessageEntity(
                conversationId = convId,
                senderName = "[TEST DATA] Dev Simulator",
                senderPhone = "+1 (555) 000-0000",
                messageText = "[TEST DATA] This is a developer test message verifying notification listener parsing.",
                timestamp = now,
                appSource = "com.google.android.apps.messaging",
                isIncoming = true
            )
        )

        eventDao.insertEvent(
            BackupEventEntity(
                eventType = "TEST_CAPTURE",
                summary = "[TEST DATA] Developer test message recorded",
                status = "SUCCESS"
            )
        )
    }
}
