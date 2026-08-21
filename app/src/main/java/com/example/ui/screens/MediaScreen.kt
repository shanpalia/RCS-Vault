package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.MediaFileEntity
import com.example.service.MediaBackupManager
import com.example.ui.components.AppHeader
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mediaFiles by viewModel.mediaFiles.collectAsState()
    val selectedCategory by viewModel.selectedMediaCategory.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var isGridView by remember { mutableStateOf(false) }
    var selectedMediaForDetails by remember { mutableStateOf<MediaFileEntity?>(null) }

    val categories = listOf("ALL", "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "RECOVERED")

    val filteredMedia = remember(mediaFiles, selectedCategory) {
        when (selectedCategory) {
            "ALL" -> mediaFiles
            "RECOVERED" -> mediaFiles.filter { it.isPreserved }
            else -> mediaFiles.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        AppHeader(
            title = "Preserved Media",
            subtitle = "${mediaFiles.size} media files • ${stats.formattedStorage} used",
            actions = {
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier.testTag("media_view_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = "Toggle View",
                        tint = MintPrimary
                    )
                }
                IconButton(
                    onClick = { viewModel.scanAccessibleMedia() },
                    modifier = Modifier.testTag("media_scan_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Scan Media",
                        tint = MintPrimary
                    )
                }
            }
        )

        // Storage Usage Meter Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Storage Allocation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stats.formattedStorage,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MintPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Segmented Progress Bar
                LinearProgressIndicator(
                    progress = { 
                        if (stats.totalStorageBytes <= 0) 0f 
                        else (stats.totalStorageBytes.toFloat() / (1024 * 1024 * 500f)).coerceIn(0f, 1.0f) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MintPrimary,
                    trackColor = MintPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Images: ${stats.totalImages}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Videos: ${stats.totalVideos}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Audio: ${stats.totalAudio}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Docs: ${stats.totalDocuments}", fontSize = 11.sp, color = TextSecondaryDark)
                }
            }
        }

        // Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = BackgroundWhite,
            contentColor = MintPrimary,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectMediaCategory(cat) },
                    label = {
                        Text(
                            text = when (cat) {
                                "ALL" -> "All (${mediaFiles.size})"
                                "IMAGE" -> "Images (${stats.totalImages})"
                                "VIDEO" -> "Videos (${stats.totalVideos})"
                                "AUDIO" -> "Audio (${stats.totalAudio})"
                                "DOCUMENT" -> "Docs (${stats.totalDocuments})"
                                "RECOVERED" -> "Recovered"
                                else -> cat
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MintPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SurfacePureWhite,
                        labelColor = TextPrimaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = OutlineBorder,
                        selectedBorderColor = MintPrimary
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp, top = 4.dp, bottom = 6.dp)
                        .testTag("media_filter_$cat")
                )
            }
        }

        // Media Content Area
        if (filteredMedia.isEmpty()) {
            EmptyStateWidget(
                title = "No preserved media in this category",
                description = "Scan accessible files or enable Auto Backup to automatically preserve photos, videos, and documents.",
                actionText = "Scan Accessible Media",
                icon = Icons.Outlined.PermMedia,
                onActionClick = { viewModel.scanAccessibleMedia() }
            )
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filteredMedia, key = { it.id }) { media ->
                        MediaGridItem(
                            media = media,
                            onClick = { selectedMediaForDetails = media }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filteredMedia, key = { it.id }) { media ->
                        MediaListItem(
                            media = media,
                            onClick = { selectedMediaForDetails = media }
                        )
                    }
                }
            }
        }
    }

    // Media Detail Dialog
    if (selectedMediaForDetails != null) {
        val media = selectedMediaForDetails!!
        MediaActionDialog(
            media = media,
            onDismiss = { selectedMediaForDetails = null },
            onShare = {
                try {
                    val file = File(media.filePath)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = media.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Preserved Media"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                selectedMediaForDetails = null
            },
            onDelete = {
                viewModel.deleteMedia(media)
                selectedMediaForDetails = null
            }
        )
    }
}

@Composable
fun MediaGridItem(media: MediaFileEntity, onClick: () -> Unit) {
    val (icon, color) = when (media.category) {
        "IMAGE" -> Icons.Filled.Image to Color(0xFF0288D1)
        "VIDEO" -> Icons.Filled.Videocam to Color(0xFFE65100)
        "AUDIO" -> Icons.Filled.Mic to Color(0xFF7B1FA2)
        else -> Icons.Filled.Description to Color(0xFF00796B)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .testTag("media_grid_item_${media.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = media.fileName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = MediaBackupManager.formatBytes(media.sizeBytes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextTertiaryDark)
            )
        }
    }
}

@Composable
fun MediaListItem(media: MediaFileEntity, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(media.timestamp))

    val (icon, color) = when (media.category) {
        "IMAGE" -> Icons.Filled.Image to Color(0xFF0288D1)
        "VIDEO" -> Icons.Filled.Videocam to Color(0xFFE65100)
        "AUDIO" -> Icons.Filled.Mic to Color(0xFF7B1FA2)
        else -> Icons.Filled.Description to Color(0xFF00796B)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("media_list_item_${media.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${media.category} • ${MediaBackupManager.formatBytes(media.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "$formattedTime • ${media.senderName ?: "Preserved"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiaryDark,
                        fontSize = 10.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiaryDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MediaActionDialog(
    media: MediaFileEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = media.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category: ${media.category}", style = MaterialTheme.typography.bodyMedium)
                Text("MIME Type: ${media.mimeType}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${MediaBackupManager.formatBytes(media.sizeBytes)}", style = MaterialTheme.typography.bodyMedium)
                Text("Preserved: ${timeFormat.format(Date(media.timestamp))}", style = MaterialTheme.typography.bodySmall)
                Text("SHA-256 Hash: ${media.sha256Hash}", style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark)
                Text("Location: App Protected Directory", style = MaterialTheme.typography.bodySmall, color = MintPrimaryDark)
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
