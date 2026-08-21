package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.BackupStats
import com.example.data.model.ConversationEntity
import com.example.data.model.MediaFileEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ReportEntity
import com.example.data.repository.BackupRepository
import com.example.data.update.UpdateManager
import com.example.data.update.UpdateStatus
import com.example.data.update.VersionInfo
import com.example.util.PermissionHelper
import com.example.util.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class NavigationTab {
    HOME, MESSAGES, MEDIA, REPORTS, SETTINGS
}

sealed class PdfGenerationState {
    object Idle : PdfGenerationState()
    object Generating : PdfGenerationState()
    data class Success(val file: File, val reportEntity: ReportEntity?) : PdfGenerationState()
    data class Error(val message: String) : PdfGenerationState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BackupRepository(application)

    // Current Tab
    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Active Conversation Detail
    private val _selectedConversation = MutableStateFlow<ConversationEntity?>(null)
    val selectedConversation: StateFlow<ConversationEntity?> = _selectedConversation.asStateFlow()

    // Global Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Media Filter Tab
    private val _selectedMediaCategory = MutableStateFlow("ALL")
    val selectedMediaCategory: StateFlow<String> = _selectedMediaCategory.asStateFlow()

    // App Security Lock State
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Notification Listener Status
    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    // PDF Generation State
    private val _pdfState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Idle)
    val pdfState: StateFlow<PdfGenerationState> = _pdfState.asStateFlow()

    // User Message/Notice
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Database Flows
    val stats: StateFlow<BackupStats> = repository.backupStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupStats())

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaFiles: StateFlow<List<MediaFileEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEvents: StateFlow<List<BackupEventEntity>> = repository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update Management
    val updateManager = UpdateManager(application)
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    init {
        checkPermissions()
        checkAppLock()
        checkForUpdates(isManualCheck = false)
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openConversation(conversation: ConversationEntity) {
        _selectedConversation.value = conversation
    }

    fun closeConversation() {
        _selectedConversation.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMediaCategory(category: String) {
        _selectedMediaCategory.value = category
    }

    fun checkPermissions() {
        _isNotificationAccessGranted.value =
            PermissionHelper.isNotificationListenerEnabled(getApplication())
    }

    private fun checkAppLock() {
        val lockEnabled = repository.preferences.isAppLockEnabled
        val pin = repository.preferences.appPin
        _isAppLocked.value = lockEnabled && !pin.isNullOrEmpty()
    }

    fun unlockApp(pin: String): Boolean {
        val storedHash = repository.preferences.appPin
        val success = SecurityManager.verifyPin(pin, storedHash)
        if (success) {
            _isAppLocked.value = false
        }
        return success
    }

    fun setAppPin(pin: String) {
        repository.preferences.appPin = SecurityManager.sha256(pin)
        repository.preferences.isAppLockEnabled = true
        _snackbarMessage.value = "PIN security protection enabled"
    }

    fun disableAppLock() {
        repository.preferences.isAppLockEnabled = false
        repository.preferences.appPin = null
        _isAppLocked.value = false
        _snackbarMessage.value = "PIN protection disabled"
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAutoBackup(enabled)
            _snackbarMessage.value = if (enabled) "Auto Backup activated" else "Auto Backup paused"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun resetPdfState() {
        _pdfState.value = PdfGenerationState.Idle
    }

    /**
     * Generate PDF Report with selected filters
     */
    fun generatePdfReport(
        title: String,
        targetConversationId: String?,
        conversationName: String,
        contactPhone: String?,
        dateRangeText: String,
        includeMessages: Boolean = true,
        includeMedia: Boolean = true
    ) {
        viewModelScope.launch {
            _pdfState.value = PdfGenerationState.Generating
            try {
                val allMsgs = databaseInstance().messageDao().getAllMessagesSync()
                val filteredMsgs = if (targetConversationId != null) {
                    allMsgs.filter { it.conversationId == targetConversationId }
                } else {
                    allMsgs
                }

                val allMed = databaseInstance().mediaDao().getAllMediaSync()
                val filteredMed = if (targetConversationId != null) {
                    allMed.filter { it.conversationId == targetConversationId }
                } else {
                    allMed
                }

                val file = repository.pdfGenerator.generateReport(
                    title = title,
                    targetConversationId = targetConversationId,
                    conversationName = conversationName,
                    contactPhone = contactPhone,
                    dateRangeText = dateRangeText,
                    messages = filteredMsgs,
                    mediaFiles = filteredMed,
                    includeMessages = includeMessages,
                    includeMedia = includeMedia
                )

                _pdfState.value = PdfGenerationState.Success(file, null)
                _snackbarMessage.value = "PDF Report generated successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _pdfState.value = PdfGenerationState.Error(e.localizedMessage ?: "Failed to generate PDF")
            }
        }
    }

    /**
     * Simulate an incoming test message (Debug only) to verify notification parsing pipelines
     */
    fun simulateTestIncomingMessage(contactName: String, text: String, hasAttachment: Boolean = false, mediaType: String? = null) {
        if (!com.example.BuildConfig.DEBUG) return
        viewModelScope.launch {
            val displayName = if (contactName.startsWith("[TEST DATA]")) contactName else "[TEST DATA] $contactName"
            val displayMessage = if (text.startsWith("[TEST DATA]")) text else "[TEST DATA] $text"
            val convId = displayName.trim().lowercase().replace("[^a-z0-9]".toRegex(), "_")
            val message = MessageEntity(
                conversationId = convId,
                senderName = displayName,
                senderPhone = null,
                messageText = displayMessage,
                timestamp = System.currentTimeMillis(),
                appSource = "com.google.android.apps.messaging",
                isIncoming = true,
                hasAttachment = hasAttachment,
                mediaType = mediaType,
                backupStatus = "BACKED_UP"
            )
            repository.insertMessage(message)

            val existing = databaseInstance().conversationDao().getConversationByIdSync(convId)
            databaseInstance().conversationDao().insertOrUpdate(
                ConversationEntity(
                    conversationId = convId,
                    contactName = displayName,
                    contactPhone = null,
                    lastMessage = displayMessage,
                    lastTimestamp = System.currentTimeMillis(),
                    messageCount = (existing?.messageCount ?: 0) + 1,
                    mediaCount = (existing?.mediaCount ?: 0) + (if (hasAttachment) 1 else 0),
                    avatarColorHex = existing?.avatarColorHex ?: "#38B2AC",
                    appSource = "RCS Debug Tool"
                )
            )

            databaseInstance().backupEventDao().insertEvent(
                BackupEventEntity(
                    eventType = "TEST_CAPTURE",
                    summary = "Captured test diagnostic message for $displayName",
                    status = "SUCCESS"
                )
            )

            repository.preferences.lastBackupTime = System.currentTimeMillis()
            _snackbarMessage.value = "Test diagnostic message captured"
        }
    }

    /**
     * Scan accessible media files
     */
    fun scanAccessibleMedia() {
        viewModelScope.launch {
            val count = repository.mediaManager.scanAndPreserveAccessibleMedia(20)
            _snackbarMessage.value = if (count > 0) "Preserved $count new media files" else "Media scan complete. All files up to date."
        }
    }

    /**
     * Export Full Backup archive
     */
    fun exportBackup(onExportReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val file = repository.exporter.exportFullBackup()
                _snackbarMessage.value = "Full backup archive created (${file.name})"
                onExportReady(file)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Failed to export backup: ${e.message}"
            }
        }
    }

    /**
     * Clear all backup data
     */
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedConversation.value = null
            _snackbarMessage.value = "All backup data has been deleted."
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            _selectedConversation.value = null
            _snackbarMessage.value = "Conversation deleted from backup"
        }
    }

    fun deleteMedia(media: MediaFileEntity) {
        viewModelScope.launch {
            repository.deleteMedia(media)
            _snackbarMessage.value = "Media file removed from backup"
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.deleteReport(report)
            _snackbarMessage.value = "Report deleted"
        }
    }

    /**
     * Check for app updates from official server
     */
    fun checkForUpdates(isManualCheck: Boolean = false) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val status = updateManager.checkForUpdates(isManualCheck)
            _updateStatus.value = status
            if (isManualCheck) {
                when (status) {
                    is UpdateStatus.UpToDate -> {
                        _snackbarMessage.value = "RCS Vault is up to date (${status.currentVersion})"
                    }
                    is UpdateStatus.Error -> {
                        _snackbarMessage.value = status.message
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Download and install the update APK
     */
    fun startUpdateDownload(info: VersionInfo) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Downloading(0f, 0L, 0L)
            val result = updateManager.downloadApk(info) { progress, downloaded, total ->
                _updateStatus.value = UpdateStatus.Downloading(progress, downloaded, total)
            }

            result.onSuccess { apkFile ->
                _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile, info)
                updateManager.installApk(apkFile)
            }.onFailure { error ->
                _updateStatus.value = UpdateStatus.Error("Failed to download update: ${error.localizedMessage ?: "Unknown error"}")
                _snackbarMessage.value = "Update download failed. Please try again."
            }
        }
    }

    fun installDownloadedApk(apkFile: File) {
        updateManager.installApk(apkFile)
    }

    fun dismissUpdateDialog() {
        _updateStatus.value = UpdateStatus.Idle
    }

    /**
     * Diagnostic Test Data Populator (Guarded by DEBUG)
     */
    fun generateDebugTestData() {
        if (!com.example.BuildConfig.DEBUG) return
        viewModelScope.launch {
            repository.generateDebugTestData()
            _snackbarMessage.value = "Diagnostic test dataset populated with [TEST DATA] tags"
        }
    }

    private fun databaseInstance(): AppDatabase = AppDatabase.getInstance(getApplication())
}
