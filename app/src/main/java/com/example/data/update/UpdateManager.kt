package com.example.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: List<String>,
    val forceUpdate: Boolean = false
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(val info: VersionInfo, val isManualCheck: Boolean = false) : UpdateStatus()
    data class UpToDate(val currentVersion: String, val isManualCheck: Boolean = false) : UpdateStatus()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val info: VersionInfo) : UpdateStatus()
    data class Error(val message: String, val isManualCheck: Boolean = false) : UpdateStatus()
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        const val UPDATE_SERVER_BASE_URL = "https://shanpalia.github.io/WebsitePaliaAPK_V.2/"
        // Keep version.json at the website root. The legacy /rcs-vault/ path is
        // also tried for compatibility with older deployments.
        const val VERSION_JSON_URL = "https://shanpalia.github.io/WebsitePaliaAPK_V.2/version.json"
        private const val LEGACY_VERSION_JSON_URL = "https://shanpalia.github.io/WebsitePaliaAPK_V.2/rcs-vault/version.json"
        private const val RAW_GITHUB_VERSION_JSON_URL = "https://raw.githubusercontent.com/shanpalia/WebsitePaliaAPK_V.2/main/version.json"
        private const val RAW_GITHUB_TEMPLATE_VERSION_JSON_URL = "https://raw.githubusercontent.com/shanpalia/WebsitePaliaAPK_V.2/main/update-server-template/version.json"
        private const val RAW_GITHUB_MASTER_VERSION_JSON_URL = "https://raw.githubusercontent.com/shanpalia/WebsitePaliaAPK_V.2/master/version.json"
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }

    val currentVersionCode: Long
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }

    /**
     * Checks version.json from the official update server
     */
    suspend fun checkForUpdates(isManualCheck: Boolean = false): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val candidateUrls = listOf(
                VERSION_JSON_URL,
                LEGACY_VERSION_JSON_URL,
                RAW_GITHUB_VERSION_JSON_URL,
                RAW_GITHUB_TEMPLATE_VERSION_JSON_URL,
                RAW_GITHUB_MASTER_VERSION_JSON_URL
            ).distinct()
            var bodyString: String? = null
            var lastHttpError: String? = null

            for (url in candidateUrls) {
                try {
                    val cacheBustedUrl = Uri.parse(url).buildUpon()
                        .appendQueryParameter("t", System.currentTimeMillis().toString())
                        .build()
                    Log.d(TAG, "Checking update from $cacheBustedUrl")
                    val request = Request.Builder()
                        .url(cacheBustedUrl.toString())
                        .header("User-Agent", "RCS-Vault-Android/${currentVersionName}")
                        .header("Cache-Control", "no-cache")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            bodyString = response.body?.string()
                        } else {
                            lastHttpError = "Update server returned HTTP ${response.code}"
                            Log.w(TAG, "$lastHttpError from $url")
                        }
                    }

                    if (!bodyString.isNullOrBlank()) break
                } catch (e: Exception) {
                    lastHttpError = e.localizedMessage ?: "Network error"
                    Log.w(TAG, "Update check failed for $url", e)
                }
            }

            if (bodyString.isNullOrBlank()) {
                return@withContext UpdateStatus.Error(
                    lastHttpError ?: "Update server is unavailable. Publish version.json on the official website.",
                    isManualCheck
                )
            }

            val json = JSONObject(bodyString)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "")
            val apkUrl = json.optString("apkUrl", "")
            val forceUpdate = json.optBoolean("forceUpdate", false)

            val notesArray = json.optJSONArray("releaseNotes")
            val releaseNotes = mutableListOf<String>()
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    releaseNotes.add(notesArray.optString(i))
                }
            }

            if (apkUrl.isBlank()) {
                return@withContext UpdateStatus.Error("Invalid update configuration: missing APK URL", isManualCheck)
            }

            val versionInfo = VersionInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                apkUrl = apkUrl,
                releaseNotes = releaseNotes,
                forceUpdate = forceUpdate
            )

            Log.d(TAG, "Current versionCode=$currentVersionCode, Remote versionCode=$remoteVersionCode")

            if (remoteVersionCode > currentVersionCode) {
                UpdateStatus.UpdateAvailable(versionInfo, isManualCheck)
            } else {
                UpdateStatus.UpToDate("$currentVersionName (Build $currentVersionCode)", isManualCheck)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update", e)
            UpdateStatus.Error("Failed to check for updates: ${e.localizedMessage ?: "Network error"}", isManualCheck)
        }
    }

    /**
     * Downloads APK over HTTPS to app cache and verifies package before installation
     */
    suspend fun downloadApk(
        info: VersionInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()

            val targetFile = File(updatesDir, "rcs-vault-${info.versionName}.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            Log.d(TAG, "Downloading APK from ${info.apkUrl} to ${targetFile.absolutePath}")

            val request = Request.Builder()
                .url(info.apkUrl)
                .header("User-Agent", "RCS-Vault-Android/${currentVersionName}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (contentLength > 0) totalBytesRead.toFloat() / contentLength else 0f
                    onProgress(progress, totalBytesRead, contentLength)
                }
                outputStream.flush()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            // Verify downloaded APK package validity
            val packageArchiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    targetFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)
            }

            if (packageArchiveInfo != null) {
                Log.d(TAG, "Downloaded APK verified: pkg=${packageArchiveInfo.packageName}, verCode=${packageArchiveInfo.versionCode}")
                if (packageArchiveInfo.packageName != null &&
                    packageArchiveInfo.packageName != context.packageName &&
                    packageArchiveInfo.packageName != "com.example" &&
                    packageArchiveInfo.packageName != "com.aistudio.rcsbackup.zqmvxp") {
                    Log.w(TAG, "Package name mismatch: ${packageArchiveInfo.packageName}")
                }
            } else {
                Log.w(TAG, "Package archive inspection returned null. APK file size=${targetFile.length()} bytes")
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            Result.failure(e)
        }
    }

    /**
     * Triggers official Android system Package Installer
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Check if unknown app sources permission is required on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    // Still trigger installation intent after opening settings
                }
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }
}
