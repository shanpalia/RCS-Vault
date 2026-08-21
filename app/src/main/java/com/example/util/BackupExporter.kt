package com.example.util

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MediaFileEntity
import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupExporter(private val context: Context) {
    private val database = AppDatabase.getInstance(context)

    /**
     * Exports all database messages and conversations to a JSON-packed ZIP archive.
     */
    suspend fun exportFullBackup(): File = withContext(Dispatchers.IO) {
        val exportTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportFile = File(context.cacheDir, "rcs_full_backup_$exportTimestamp.rcsbak")

        val messages = database.messageDao().getAllMessagesSync()
        val conversations = database.conversationDao().getAllConversationsSync()
        val mediaList = database.mediaDao().getAllMediaSync()

        // 1. Build JSON metadata
        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("app", "RCS Backup & Recovery")
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("totalMessages", messages.size)
        rootJson.put("totalConversations", conversations.size)
        rootJson.put("totalMedia", mediaList.size)

        val convArray = JSONArray()
        for (c in conversations) {
            val obj = JSONObject()
            obj.put("conversationId", c.conversationId)
            obj.put("contactName", c.contactName)
            obj.put("contactPhone", c.contactPhone ?: "")
            obj.put("lastMessage", c.lastMessage)
            obj.put("lastTimestamp", c.lastTimestamp)
            obj.put("messageCount", c.messageCount)
            obj.put("mediaCount", c.mediaCount)
            convArray.put(obj)
        }
        rootJson.put("conversations", convArray)

        val msgArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject()
            obj.put("conversationId", m.conversationId)
            obj.put("senderName", m.senderName)
            obj.put("senderPhone", m.senderPhone ?: "")
            obj.put("messageText", m.messageText)
            obj.put("timestamp", m.timestamp)
            obj.put("appSource", m.appSource)
            obj.put("isIncoming", m.isIncoming)
            obj.put("hasAttachment", m.hasAttachment)
            obj.put("mediaType", m.mediaType ?: "")
            msgArray.put(obj)
        }
        rootJson.put("messages", msgArray)

        // 2. Write to ZIP
        ZipOutputStream(FileOutputStream(exportFile)).use { zos ->
            // Add backup_manifest.json
            zos.putNextEntry(ZipEntry("backup_manifest.json"))
            zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Add media files
            for (media in mediaList) {
                val f = File(media.filePath)
                if (f.exists() && f.canRead()) {
                    zos.putNextEntry(ZipEntry("media/${media.category.lowercase()}/${media.fileName}"))
                    FileInputStream(f).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        database.backupEventDao().insertEvent(
            BackupEventEntity(
                eventType = "EXPORT_BACKUP",
                summary = "Exported full backup archive with ${messages.size} msgs and ${mediaList.size} files",
                itemCount = messages.size + mediaList.size,
                status = "SUCCESS"
            )
        )

        exportFile
    }

    /**
     * Imports messages and conversations from a JSON/RCSBAK file.
     */
    suspend fun importBackup(file: File): Int = withContext(Dispatchers.IO) {
        var importedMessagesCount = 0
        try {
            var manifestJsonStr: String? = null

            if (file.name.endsWith(".rcsbak") || file.name.endsWith(".zip")) {
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "backup_manifest.json") {
                            manifestJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            } else {
                manifestJsonStr = file.readText(Charsets.UTF_8)
            }

            if (manifestJsonStr != null) {
                val json = JSONObject(manifestJsonStr!!)
                val msgArray = json.optJSONArray("messages") ?: JSONArray()
                val convArray = json.optJSONArray("conversations") ?: JSONArray()

                val convDao = database.conversationDao()
                for (i in 0 until convArray.length()) {
                    val obj = convArray.getJSONObject(i)
                    convDao.insertOrUpdate(
                        ConversationEntity(
                            conversationId = obj.getString("conversationId"),
                            contactName = obj.getString("contactName"),
                            contactPhone = obj.optString("contactPhone").takeIf { it.isNotBlank() },
                            lastMessage = obj.optString("lastMessage"),
                            lastTimestamp = obj.optLong("lastTimestamp", System.currentTimeMillis()),
                            messageCount = obj.optInt("messageCount", 1),
                            mediaCount = obj.optInt("mediaCount", 0)
                        )
                    )
                }

                val msgDao = database.messageDao()
                val newMessages = mutableListOf<MessageEntity>()
                for (i in 0 until msgArray.length()) {
                    val obj = msgArray.getJSONObject(i)
                    newMessages.add(
                        MessageEntity(
                            conversationId = obj.getString("conversationId"),
                            senderName = obj.getString("senderName"),
                            senderPhone = obj.optString("senderPhone").takeIf { it.isNotBlank() },
                            messageText = obj.getString("messageText"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            appSource = obj.optString("appSource", "Google Messages"),
                            isIncoming = obj.optBoolean("isIncoming", true),
                            hasAttachment = obj.optBoolean("hasAttachment", false),
                            mediaType = obj.optString("mediaType").takeIf { it.isNotBlank() }
                        )
                    )
                }
                msgDao.insertMessages(newMessages)
                importedMessagesCount = newMessages.size

                database.backupEventDao().insertEvent(
                    BackupEventEntity(
                        eventType = "IMPORT_BACKUP",
                        summary = "Imported $importedMessagesCount messages and ${convArray.length()} conversations",
                        itemCount = importedMessagesCount,
                        status = "SUCCESS"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        importedMessagesCount
    }
}
