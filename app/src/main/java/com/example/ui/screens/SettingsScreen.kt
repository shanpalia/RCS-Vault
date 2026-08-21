package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.update.UpdateManager
import com.example.data.update.UpdateStatus
import com.example.ui.components.AppHeader
import com.example.ui.components.RcsLimitationBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = viewModel.repository.preferences

    var isAutoBackup by remember { mutableStateOf(prefs.isAutoBackupEnabled) }
    var isWifiOnly by remember { mutableStateOf(prefs.isWifiOnly) }
    var isMobileData by remember { mutableStateOf(prefs.isMobileDataAllowed) }
    var incImages by remember { mutableStateOf(prefs.includeImages) }
    var incVideos by remember { mutableStateOf(prefs.includeVideos) }
    var incAudio by remember { mutableStateOf(prefs.includeAudio) }
    var incDocs by remember { mutableStateOf(prefs.includeDocuments) }
    var retentionDays by remember { mutableStateOf(prefs.autoDeleteDays) }
    var isAppLock by remember { mutableStateOf(prefs.isAppLockEnabled) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        AppHeader(
            title = "Backup Settings",
            subtitle = "Manage automation, storage, and privacy"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Section 1: System Permissions Status
            Text("SYSTEM INTEGRATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isNotificationAccessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (isNotificationAccessGranted) StatusSuccess else StatusWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Listener Access",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isNotificationAccessGranted) "Granted • Background capture active" else "Not Granted • Tap to configure",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiaryDark)
                        )
                    }
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    ) {
                        Text("Configure", color = MintPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Automation Rules
            Text("AUTOMATIC BACKUP RULES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Auto Backup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Monitor incoming RCS/SMS notifications in real-time", fontSize = 11.sp, color = TextTertiaryDark)
                        }
                        Switch(
                            checked = isAutoBackup,
                            onCheckedChange = {
                                isAutoBackup = it
                                viewModel.toggleAutoBackup(it)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineVariant)

                    // Wi-Fi Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wi-Fi Only for Media", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Preserve large media files only when on Wi-Fi", fontSize = 11.sp, color = TextTertiaryDark)
                        }
                        Switch(
                            checked = isWifiOnly,
                            onCheckedChange = {
                                isWifiOnly = it
                                prefs.isWifiOnly = it
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineVariant)

                    // Mobile Data Allowed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mobile Data Allowed", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Capture text notifications on cellular network", fontSize = 11.sp, color = TextTertiaryDark)
                        }
                        Switch(
                            checked = isMobileData,
                            onCheckedChange = {
                                isMobileData = it
                                prefs.isMobileDataAllowed = it
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Media Inclusion
            Text("MEDIA TYPES TO PRESERVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Images (Photos, Screenshots)", fontSize = 13.sp)
                        Checkbox(checked = incImages, onCheckedChange = { incImages = it; prefs.includeImages = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Videos", fontSize = 13.sp)
                        Checkbox(checked = incVideos, onCheckedChange = { incVideos = it; prefs.includeVideos = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Audio & Voice Recordings", fontSize = 13.sp)
                        Checkbox(checked = incAudio, onCheckedChange = { incAudio = it; prefs.includeAudio = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Documents & PDFs", fontSize = 13.sp)
                        Checkbox(checked = incDocs, onCheckedChange = { incDocs = it; prefs.includeDocuments = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Security & Lock
            Text("SECURITY & ACCESS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PIN Security Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Require a 4-digit PIN to open the app", fontSize = 11.sp, color = TextTertiaryDark)
                        }
                        Switch(
                            checked = isAppLock,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinDialog = true
                                } else {
                                    viewModel.disableAppLock()
                                    isAppLock = false
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPrivacy() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Policy & RCS Notice", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("100% On-Device Privacy Architecture", fontSize = 11.sp, color = TextTertiaryDark)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiaryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: App Updates & Version
            val updateStatus by viewModel.updateStatus.collectAsState()
            Text("APP UPDATES & SERVER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6FFFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RCS Vault v${viewModel.updateManager.currentVersionName} (Build ${viewModel.updateManager.currentVersionCode})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Official Update Server",
                                fontSize = 11.sp,
                                color = TextTertiaryDark
                            )
                        }
                    }

                    Surface(
                        color = SurfaceOffWhite,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "SERVER BASE URL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiaryDark,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = UpdateManager.UPDATE_SERVER_BASE_URL,
                                fontSize = 11.sp,
                                color = MintPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Live Status Feedback
                    when (val st = updateStatus) {
                        is UpdateStatus.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MintPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting to update server...", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                        is UpdateStatus.UpdateAvailable -> {
                            Surface(
                                color = Color(0xFFE6FFFA),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.NewReleases, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "New Update Available: v${st.info.versionName} (Build ${st.info.versionCode})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSlate
                                    )
                                }
                            }
                        }
                        is UpdateStatus.UpToDate -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("App is up to date (${st.currentVersion})", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                        is UpdateStatus.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(st.message, fontSize = 11.sp, color = StatusError)
                            }
                        }
                        else -> {}
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates(isManualCheck = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_updates_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 5: Data Management & Export
            Text("DATA MANAGEMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextTertiaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Export Full Backup
                    OutlinedButton(
                        onClick = {
                            viewModel.exportBackup { file ->
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Full Backup"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_export_backup_btn")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Full Backup (ZIP Archive)")
                    }

                    // Clear All Data
                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_clear_data_btn")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Backup Data")
                    }
                }
            }

            if (com.example.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("DEVELOPER DIAGNOSTICS (DEBUG ONLY)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Testing tools compiled only in debug mode. All created test items are explicitly prefixed with [TEST DATA].",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.generateDebugTestData()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("debug_generate_test_data_btn")
                        ) {
                            Icon(Icons.Filled.BugReport, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Populate Diagnostic Test Data", color = Color(0xFF92400E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RcsLimitationBanner(onLearnMoreClick = onNavigateToPrivacy)

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showPinDialog) {
        var pinValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to protect your backup messages from unauthorized access on this device.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4) pinValue = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            viewModel.setAppPin(pinValue)
                            isAppLock = true
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                ) {
                    Text("Set PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Delete All Backup Data?", fontWeight = FontWeight.Bold, color = StatusError) },
            text = {
                Text("This will permanently delete all messages, media files, and reports stored in this app. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
