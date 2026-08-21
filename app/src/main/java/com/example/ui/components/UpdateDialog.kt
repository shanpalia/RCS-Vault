package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.update.UpdateStatus
import com.example.data.update.VersionInfo
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun UpdateDialog(viewModel: MainViewModel) {
    val updateStatus by viewModel.updateStatus.collectAsState()

    when (val status = updateStatus) {
        is UpdateStatus.UpdateAvailable -> {
            UpdateContentDialog(
                info = status.info,
                currentVersionCode = viewModel.updateManager.currentVersionCode,
                currentVersionName = viewModel.updateManager.currentVersionName,
                isDownloading = false,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                onUpdateNow = { viewModel.startUpdateDownload(status.info) },
                onDismiss = { if (!status.info.forceUpdate) viewModel.dismissUpdateDialog() }
            )
        }
        is UpdateStatus.Downloading -> {
            // Find current info from downloading or fallback
            UpdateDownloadingDialog(
                progress = status.progress,
                downloadedBytes = status.bytesDownloaded,
                totalBytes = status.totalBytes
            )
        }
        is UpdateStatus.ReadyToInstall -> {
            UpdateReadyDialog(
                info = status.info,
                onInstall = { viewModel.installDownloadedApk(status.apkFile) },
                onDismiss = { if (!status.info.forceUpdate) viewModel.dismissUpdateDialog() }
            )
        }
        else -> {
            // Idle, Checking, UpToDate, or Error (handled via snackbars/settings)
        }
    }
}

@Composable
private fun UpdateContentDialog(
    info: VersionInfo,
    currentVersionCode: Long,
    currentVersionName: String,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !info.forceUpdate,
            dismissOnClickOutside = !info.forceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfacePureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6FFFA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "New Update Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "A new version of RCS Vault is ready.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Version Comparison Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceOffWhite)
                        .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT VERSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiaryDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currentVersionName (v$currentVersionCode)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(18.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NEW VERSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = MintPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${info.versionName} (v${info.versionCode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Release Notes Section
                if (info.releaseNotes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceOffWhite)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Stars,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WHAT'S NEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        info.releaseNotes.forEach { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintPrimary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = note,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Action Buttons
                Button(
                    onClick = onUpdateNow,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("update_now_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (!info.forceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_later_btn")
                    ) {
                        Text(
                            text = "Later",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDownloadingDialog(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long
) {
    Dialog(
        onDismissRequest = { /* Non-dismissible while downloading */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfacePureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { if (progress > 0f) progress else 0.05f },
                    color = MintPrimary,
                    trackColor = OutlineBorder,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Downloading Update...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val downloadedMb = (downloadedBytes.toFloat() / (1024 * 1024))
                val totalMb = (totalBytes.toFloat() / (1024 * 1024))
                val percentInt = (progress * 100).toInt()

                Text(
                    text = if (totalBytes > 0) {
                        "%.1f MB / %.1f MB ($percentInt%%)".format(downloadedMb, totalMb)
                    } else {
                        "Connecting to update server..."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    color = MintPrimary,
                    trackColor = OutlineBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Using official HTTPS update server.",
                    fontSize = 11.sp,
                    color = TextTertiaryDark
                )
            }
        }
    }
}

@Composable
private fun UpdateReadyDialog(
    info: VersionInfo,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !info.forceUpdate,
            dismissOnClickOutside = !info.forceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfacePureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6FFFA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Update Ready to Install",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Version ${info.versionName} has been downloaded and verified.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("install_now_btn")
                ) {
                    Icon(imageVector = Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (!info.forceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Later",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}
