package com.example.service

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.preferences.BackupPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RcsNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    private lateinit var preferences: BackupPreferences

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        preferences = BackupPreferences(applicationContext)
        Log.d(TAG, "RcsNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "Auto backup is disabled; skipping notification.")
            return
        }

        serviceScope.launch {
            processStatusBarNotification(sbn, database, preferences)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handled if needed
    }

    companion object {
        private const val TAG = "RcsNotifyListener"

        private val KNOWN_MESSAGING_PACKAGES = setOf(
            "com.google.android.apps.messaging", // Google Messages (RCS/SMS)
            "com.samsung.android.messaging",     // Samsung Messages
            "com.android.mms",                   // Default AOSP Messaging
            "com.verizon.messaging.vzmsgs",      // Verizon Messages
            "com.motorola.messaging"             // Motorola Messaging
        )

        suspend fun processStatusBarNotification(
            sbn: StatusBarNotification,
            db: AppDatabase,
            prefs: BackupPreferences
        ) {
            val packageName = sbn.packageName ?: ""
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Filter for messaging apps or CATEGORY_MESSAGE
            val isKnownPkg = KNOWN_MESSAGING_PACKAGES.contains(packageName) ||
                    packageName.contains("messaging") ||
                    packageName.contains("mms") ||
                    packageName.contains("sms") ||
                    packageName.contains("rcs")
            val isMessageCategory = notification.category == Notification.CATEGORY_MESSAGE
            val hasMessagingStyle = extras.containsKey(NotificationCompat.EXTRA_MESSAGING_STYLE_USER) ||
                    extras.containsKey(Notification.EXTRA_MESSAGES)

            if (!isKnownPkg && !isMessageCategory && !hasMessagingStyle) {
                return
            }

            // Extract contact name / title
            var senderName = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
                ?: extras.getString(Notification.EXTRA_TITLE)
                ?: extras.getString(Notification.EXTRA_TITLE_BIG)
                ?: "Unknown Contact"

            // Extract message text
            var messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""

            if (messageText.isBlank()) {
                val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (!lines.isNullOrEmpty()) {
                    messageText = lines.last().toString()
                }
            }

            if (messageText.isBlank()) {
                return // No actionable message content in this notification
            }

            val timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

            // Detect phone number if formatted in sender title or text
            val phoneRegex = Regex("""(\+?[0-9]{1,3}[-.\s]?)?(\(?[0-9]{3}\)?[-.\s]?)?[0-9]{3}[-.\s]?[0-9]{4}""")
            val phoneMatch = phoneRegex.find(senderName)?.value ?: phoneRegex.find(messageText)?.value

            // Detect media attachment mentions in RCS notifications
            val lowerText = messageText.lowercase()
            var hasAttachment = false
            var mediaType: String? = null

            when {
                lowerText.contains("photo") || lowerText.contains("picture") || lowerText.contains("image") || lowerText.contains("📷") -> {
                    hasAttachment = true
                    mediaType = "IMAGE"
                }
                lowerText.contains("video") || lowerText.contains("movie") || lowerText.contains("🎥") -> {
                    hasAttachment = true
                    mediaType = "VIDEO"
                }
                lowerText.contains("audio") || lowerText.contains("voice message") || lowerText.contains("voice note") || lowerText.contains("🎤") -> {
                    hasAttachment = true
                    mediaType = "AUDIO"
                }
                lowerText.contains("document") || lowerText.contains("pdf") || lowerText.contains("file") || lowerText.contains("📎") -> {
                    hasAttachment = true
                    mediaType = "DOCUMENT"
                }
            }

            // Consistent Conversation ID
            val conversationId = senderName.trim().lowercase().replace("[^a-z0-9]".toRegex(), "_")

            val messageEntity = MessageEntity(
                conversationId = conversationId,
                senderName = senderName.trim(),
                senderPhone = phoneMatch,
                messageText = messageText.trim(),
                timestamp = timestamp,
                appSource = packageName,
                isIncoming = true,
                hasAttachment = hasAttachment,
                mediaType = mediaType,
                backupStatus = "BACKED_UP"
            )

            // Save Message
            val messageDao = db.messageDao()
            messageDao.insertMessage(messageEntity)

            // Save / Update Conversation
            val convDao = db.conversationDao()
            val existingConv = convDao.getConversationByIdSync(conversationId)
            val updatedMsgCount = (existingConv?.messageCount ?: 0) + 1
            val updatedMediaCount = (existingConv?.mediaCount ?: 0) + (if (hasAttachment) 1 else 0)

            val convEntity = ConversationEntity(
                conversationId = conversationId,
                contactName = senderName.trim(),
                contactPhone = phoneMatch ?: existingConv?.contactPhone,
                lastMessage = messageText.trim(),
                lastTimestamp = timestamp,
                messageCount = updatedMsgCount,
                mediaCount = updatedMediaCount,
                avatarColorHex = existingConv?.avatarColorHex ?: getRandomAvatarColor(conversationId),
                appSource = if (packageName.contains("google")) "Google Messages (RCS)" else "SMS / RCS"
            )
            convDao.insertOrUpdate(convEntity)

            // Log Backup Event
            val eventDao = db.backupEventDao()
            eventDao.insertEvent(
                BackupEventEntity(
                    eventType = "NOTIFICATION_CAPTURED",
                    summary = "Backed up message from $senderName: \"${messageText.take(30)}\"",
                    itemCount = 1,
                    status = "SUCCESS"
                )
            )

            prefs.lastBackupTime = timestamp
            prefs.totalCapturedCount = prefs.totalCapturedCount + 1
        }

        private fun getRandomAvatarColor(seed: String): String {
            val colors = listOf(
                "#00897B", "#26A69A", "#00BFA5", "#0288D1",
                "#0097A7", "#43A047", "#3949AB", "#689F38"
            )
            val index = Math.abs(seed.hashCode()) % colors.size
            return colors[index]
        }
    }
}
