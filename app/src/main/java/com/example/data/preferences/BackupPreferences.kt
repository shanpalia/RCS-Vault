package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class BackupPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rcs_backup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_FREQUENCY = "backup_frequency" // REALTIME, HOURLY, DAILY
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_MOBILE_DATA_ALLOWED = "mobile_data_allowed"
        private const val KEY_INCLUDE_IMAGES = "include_images"
        private const val KEY_INCLUDE_VIDEOS = "include_videos"
        private const val KEY_INCLUDE_AUDIO = "include_audio"
        private const val KEY_INCLUDE_DOCUMENTS = "include_documents"
        private const val KEY_MAX_STORAGE_MB = "max_storage_mb" // e.g. 2048 MB
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days" // 0 = never, 30, 60, 90
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_TOTAL_CAPTURED = "total_captured_count"
    }

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, value).apply()

    var backupFrequency: String
        get() = prefs.getString(KEY_BACKUP_FREQUENCY, "REALTIME") ?: "REALTIME"
        set(value) = prefs.edit().putString(KEY_BACKUP_FREQUENCY, value).apply()

    var isWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var isMobileDataAllowed: Boolean
        get() = prefs.getBoolean(KEY_MOBILE_DATA_ALLOWED, true)
        set(value) = prefs.edit().putBoolean(KEY_MOBILE_DATA_ALLOWED, value).apply()

    var includeImages: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_IMAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_IMAGES, value).apply()

    var includeVideos: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_VIDEOS, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_VIDEOS, value).apply()

    var includeAudio: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_AUDIO, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_AUDIO, value).apply()

    var includeDocuments: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_DOCUMENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_DOCUMENTS, value).apply()

    var maxStorageMb: Int
        get() = prefs.getInt(KEY_MAX_STORAGE_MB, 2048)
        set(value) = prefs.edit().putInt(KEY_MAX_STORAGE_MB, value).apply()

    var autoDeleteDays: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_DAYS, 0) // 0 = never
        set(value) = prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var appPin: String?
        get() = prefs.getString(KEY_APP_PIN, null)
        set(value) = prefs.edit().putString(KEY_APP_PIN, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    var totalCapturedCount: Int
        get() = prefs.getInt(KEY_TOTAL_CAPTURED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_CAPTURED, value).apply()

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }
}
