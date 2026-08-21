package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Surface(
                color = SurfacePureWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("privacy_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "Privacy & Legal Notice",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundWhite)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Highlight Badge
            Surface(
                color = MintPrimaryLight,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MintPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% On-Device & Private",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MintPrimaryDark
                            )
                        )
                        Text(
                            text = "Zero cloud uploads. Zero third-party trackers.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrivacySectionItem(
                icon = Icons.Outlined.Security,
                title = "Android & RCS Official Scope",
                body = "Android does not provide third-party applications with unrestricted access to Google Messages or RCS internal databases. This application operates entirely within official Android frameworks, monitoring incoming message notifications when authorized by the user and archiving accessible media."
            )

            PrivacySectionItem(
                icon = Icons.Outlined.FolderSpecial,
                title = "Local Storage & Data Isolation",
                body = "All message transcripts, sender information, timestamps, and saved media files are stored locally in the application's isolated app directory on your device. The app never transmits your private communications to external servers."
            )

            PrivacySectionItem(
                icon = Icons.Outlined.Lock,
                title = "Notification Listener Permission",
                body = "The Android Notification Listener Service is used exclusively to detect incoming SMS/RCS message events as they appear on your notification shade and preserve their text and sender name for your backup archive."
            )

            PrivacySectionItem(
                icon = Icons.Outlined.PhotoLibrary,
                title = "Media Storage Access",
                body = "Media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO) allow the app to discover and preserve media files sent to your device, ensuring you retain copies even if deleted elsewhere."
            )

            PrivacySectionItem(
                icon = Icons.Outlined.DeleteForever,
                title = "Complete User Control",
                body = "You have full control over your data. You can delete individual conversations, prune specific media files, export full ZIP backups, or wipe all backup records instantly in Settings."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PrivacySectionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )
            )
        }
    }
}
