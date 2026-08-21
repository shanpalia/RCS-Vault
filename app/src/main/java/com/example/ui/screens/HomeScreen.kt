package com.example.ui.screens
import com.example.util.PermissionHelper

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackupEventEntity
import com.example.data.model.ConversationEntity
import com.example.data.update.UpdateStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationTab
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val isAutoBackupActive by remember(stats) { derivedStateOf { stats.isAutoBackupActive } }
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    var showTestDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // App Header: Clean White with High Density Pro Badge & Action Icons
        item {
            AppHeader(
                title = "RCS Vault",
                subtitle = "SECURELY PRESERVE WHAT MATTERS",
                actions = {
                    IconButton(
                        onClick = { viewModel.selectTab(NavigationTab.MESSAGES) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("header_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToPrivacy() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("privacy_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings & Privacy",
                            tint = MintPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        // App Update Notification Banner (if update available)
        if (updateStatus is UpdateStatus.UpdateAvailable) {
            val updateInfo = (updateStatus as UpdateStatus.UpdateAvailable).info
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6FFFA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = "Update Available",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New Version Available: v${updateInfo.versionName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSlate
                                )
                            )
                            Text(
                                text = "A new update is available on the official server.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondaryDark
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.startUpdateDownload(updateInfo) },
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("home_update_now_btn")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Notification Access Warning Banner (if missing)
        if (!isNotificationAccessGranted) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Action Required",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Access Required",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            )
                            Text(
                                text = "Grant notification access to allow automatic background RCS backup.
If Android says "App was denied access", open App Info and choose "Allow restricted settings" from the ⋮ menu, then enable Notification access.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF78350F)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        } catch (_: Exception) { }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("grant_notification_access_btn")
                                ) {
                                    Text("Notification Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(PermissionHelper.getAppInfoIntent(context))
                                        } catch (_: Exception) { }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("open_app_info_btn")
                                ) {
                                    Text("App Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // High Density Hero Card: Dark Slate (#2D3748) with Glowing Accent & Switch
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HeroDarkSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x22FFFFFF), Color.Transparent),
                                radius = 260f
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup Status",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAutoBackupActive) "Automatic Active" else "Automatic Paused",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            )
                            val syncTime = if (stats.lastBackupTimestamp > 0) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(stats.lastBackupTimestamp))
                            } else "Just now"
                            Text(
                                text = "Last sync: $syncTime",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Switch(
                            checked = isAutoBackupActive,
                            onCheckedChange = { viewModel.toggleAutoBackup(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MintPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF4A5568)
                            ),
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }
                }
            }
        }

        // 2x2 High Density Stats Grid (Messages, Images, Videos, Audio)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Messages",
                        value = "${stats.totalMessages}",
                        icon = Icons.Filled.ChatBubble,
                        subtext = "+${recentEvents.count { it.eventType == "NOTIFICATION_CAPTURED" || it.eventType == "TEST_CAPTURE" }} Today",
                        accentColor = MintPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(NavigationTab.MESSAGES) }
                    )
                    StatCard(
                        title = "Images",
                        value = "${stats.totalImages}",
                        icon = Icons.Filled.Image,
                        subtext = stats.formattedStorage,
                        accentColor = CategoryImage,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.selectMediaCategory("IMAGE")
                            viewModel.selectTab(NavigationTab.MEDIA)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalStatCard(
                        title = "Videos",
                        value = "${stats.totalVideos}",
                        icon = Icons.Filled.Videocam,
                        accentColor = CategoryVideo,
                        iconBgColor = CategoryVideoBg,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.selectMediaCategory("VIDEO")
                            viewModel.selectTab(NavigationTab.MEDIA)
                        }
                    )
                    HorizontalStatCard(
                        title = "Audio",
                        value = "${stats.totalAudio}",
                        icon = Icons.Filled.Mic,
                        accentColor = CategoryAudio,
                        iconBgColor = CategoryAudioBg,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.selectMediaCategory("AUDIO")
                            viewModel.selectTab(NavigationTab.MEDIA)
                        }
                    )
                }
            }
        }

        // Section: Recent Conversations (High Density List)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT CONVERSATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextTertiaryDark,
                        letterSpacing = 0.8.sp,
                        fontSize = 11.sp
                    )
                )
                if (conversations.isNotEmpty()) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MintPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { viewModel.selectTab(NavigationTab.MESSAGES) }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (conversations.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No backed-up data yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )
                            Text(
                                text = "Incoming authorized messages will appear here once received.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiaryDark)
                            )
                        }
                        if (com.example.BuildConfig.DEBUG) {
                            TextButton(
                                onClick = { showTestDialog = true },
                                modifier = Modifier.testTag("home_simulate_btn")
                            ) {
                                Text("Test Sim", color = MintPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(conversations.take(4)) { conv ->
                HomeConversationCard(
                    conversation = conv,
                    onClick = { viewModel.openConversation(conv) }
                )
            }
        }

        // Primary High Density CTA: GENERATE PDF REPORT
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { viewModel.selectTab(NavigationTab.REPORTS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_generate_report_cta")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE PDF REPORT",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // Section Title: Quick Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (com.example.BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { showTestDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfacePureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("quick_simulate_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.BugReport, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dev Test Sim", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.scanAccessibleMedia() },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfacePureWhite),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("quick_scan_media_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Device Media", color = TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // RCS Official Limitation Notice
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                RcsLimitationBanner(onLearnMoreClick = onNavigateToPrivacy)
            }
        }

        // Section Title: Recent Backup Logs
        item {
            Text(
                text = "RECENT BACKUP LOGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextTertiaryDark,
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp)
            )
        }

        if (recentEvents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent backup events recorded yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextTertiaryDark)
                    )
                }
            }
        } else {
            items(recentEvents.take(6)) { event ->
                BackupEventRow(event = event)
            }
        }
    }

    if (showTestDialog) {
        TestMessageDialog(
            onDismiss = { showTestDialog = false },
            onSend = { contact, msg, hasMedia, mediaType ->
                viewModel.simulateTestIncomingMessage(contact, msg, hasMedia, mediaType)
                showTestDialog = false
            }
        )
    }
}

@Composable
fun HomeConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = if (conversation.lastTimestamp > 0) {
        timeFormat.format(Date(conversation.lastTimestamp))
    } else "Recent"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("home_conv_${conversation.conversationId}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = conversation.contactName,
                colorHex = conversation.avatarColorHex,
                size = 44
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiaryDark,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = conversation.lastMessage.ifBlank { "No messages" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            MessageCountBadge(
                countText = "${conversation.messageCount} msg",
                isPrimary = conversation.messageCount > 0
            )
        }
    }
}

@Composable
fun BackupEventRow(event: BackupEventEntity) {
    val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(event.timestamp))

    val icon = when (event.eventType) {
        "NOTIFICATION_CAPTURED", "TEST_CAPTURE" -> Icons.Filled.Chat
        "MEDIA_PRESERVED" -> Icons.Filled.PermMedia
        "REPORT_GENERATED" -> Icons.Filled.Description
        "CLEAR_DATA" -> Icons.Filled.Delete
        else -> Icons.Filled.CheckCircle
    }

    val iconColor = when (event.status) {
        "WARNING" -> StatusWarning
        "ERROR" -> StatusError
        else -> MintPrimary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedTime • ${event.eventType}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiaryDark,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun TestMessageDialog(
    onDismiss: () -> Unit,
    onSend: (contact: String, message: String, hasMedia: Boolean, mediaType: String?) -> Unit
) {
    var contactName by remember { mutableStateOf("[TEST DATA] Diagnostic Contact") }
    var messageText by remember { mutableStateOf("[TEST DATA] Diagnostic test message payload") }
    var attachMedia by remember { mutableStateOf(false) }
    var mediaType by remember { mutableStateOf("IMAGE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Developer Test Simulator", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DEV DIAGNOSTIC ONLY: Tests notification parsing and database pipeline. All created items are explicitly tagged [TEST DATA].",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Sender Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contactName.isNotBlank() && messageText.isNotBlank()) {
                        onSend(contactName, messageText, attachMedia, if (attachMedia) mediaType else null)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Insert Test Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



