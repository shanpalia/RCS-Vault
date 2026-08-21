package com.example.service

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.data.db.AppDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.MediaFileEntity
import com.example.data.preferences.BackupPreferences
import com.example.util.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MediaBackupManager(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val mediaDao = database.mediaDao()
    private val eventDao = database.backupEventDao()
    private val preferences = BackupPreferences(context)

    val backupDirectory: File
        get() {
            val dir = File(context.filesDir, "rcs_backup/media")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Preserves a media file from an InputStream (or Uri), deduplicating by SHA-256.
     */
    suspend fun preserveMedia(
        inputStream: InputStream,
        originalFileName: String,
        mimeType: String,
        conversationId: String? = null,
        senderName: String? = null,
        durationMs: Long? = null
    ): MediaFileEntity? = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) return@withContext null

            val hash = SecurityManager.sha256(bytes)

            // Check if already backed up
            val existing = mediaDao.findByHash(hash)
            if (existing != null) {
                return@withContext existing
            }

            // Determine category
            val category = getCategoryFromMime(mimeType, originalFileName)

            // Check preferences
            val isAllowed = when (category) {
                "IMAGE" -> preferences.includeImages
                "VIDEO" -> preferences.includeVideos
                "AUDIO" -> preferences.includeAudio
                "DOCUMENT" -> preferences.includeDocuments
                else -> true
            }
            if (!isAllowed) return@withContext null

            // Subfolder by date and category
            val dateFolder = SimpleDateFormat("yyyy_MM", Locale.US).format(Date())
            val targetDir = File(backupDirectory, "${category.lowercase()}/$dateFolder")
            if (!targetDir.exists()) targetDir.mkdirs()

            val sanitizedName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(targetDir, "${System.currentTimeMillis()}_$sanitizedName")

            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }

            val entity = MediaFileEntity(
                conversationId = conversationId,
                fileName = sanitizedName,
                filePath = targetFile.absolutePath,
                mimeType = mimeType,
                category = category,
                sizeBytes = bytes.size.toLong(),
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                sha256Hash = hash,
                senderName = senderName,
                isPreserved = true,
                backupLocation = "INTERNAL_SECURE_STORAGE"
            )

            val id = mediaDao.insertMedia(entity)
            eventDao.insertEvent(
                BackupEventEntity(
                    eventType = "MEDIA_PRESERVED",
                    summary = "Preserved $category: $sanitizedName (${formatBytes(bytes.size.toLong())})",
                    itemCount = 1,
                    status = "SUCCESS"
                )
            )

            return@withContext entity.copy(id = id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scan accessible media from MediaStore (Images, Audio, Video, Documents)
     */
    suspend fun scanAndPreserveAccessibleMedia(maxItems: Int = 25): Int = withContext(Dispatchers.IO) {
        var preservedCount = 0

        // 1. Scan Images
        if (preferences.includeImages) {
            preservedCount += scanMediaStore(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "IMAGE",
                maxItems
            )
        }

        // 2. Scan Audio
        if (preferences.includeAudio) {
            preservedCount += scanMediaStore(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "AUDIO",
                maxItems
            )
        }

        // 3. Scan Video
        if (preferences.includeVideos) {
            preservedCount += scanMediaStore(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                "VIDEO",
                maxItems
            )
        }

        if (preservedCount > 0) {
            eventDao.insertEvent(
                BackupEventEntity(
                    eventType = "AUTO_SYNC",
                    summary = "Scanned and preserved $preservedCount accessible media files",
                    itemCount = preservedCount,
                    status = "SUCCESS"
                )
            )
        }

        preservedCount
    }

    private suspend fun scanMediaStore(contentUri: Uri, category: String, limit: Int): Int {
        var count = 0
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

        try {
            context.contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "file_$id"
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val uri = ContentUris.withAppendedId(contentUri, id)

                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val result = preserveMedia(
                                inputStream = stream,
                                originalFileName = name,
                                mimeType = mime,
                                conversationId = null,
                                senderName = "MediaStore"
                            )
                            if (result != null) count++
                        }
                    } catch (e: Exception) {
                        // Skip unreadable files
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    /**
     * Exports selected or all media to a ZIP archive in cache directory.
     */
    suspend fun exportToZip(mediaList: List<MediaFileEntity>): File = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "rcs_media_export_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (media in mediaList) {
                val file = File(media.filePath)
                if (file.exists() && file.canRead()) {
                    val entryName = "${media.category.lowercase()}/${media.fileName}"
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        zipFile
    }

    companion object {
        fun getCategoryFromMime(mimeType: String, fileName: String): String {
            val lowerMime = mimeType.lowercase()
            val lowerName = fileName.lowercase()
            return when {
                lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".jpeg") -> "IMAGE"
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") -> "VIDEO"
                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".opus") || lowerName.endsWith(".ogg") -> "AUDIO"
                else -> "DOCUMENT"
            }
        }

        fun formatBytes(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
