package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.service.MediaBackupManager
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationDetailScreen(
    conversation: ConversationEntity,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onGenerateReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messagesFlow = remember(conversation.conversationId) {
        viewModel.repository.getMessagesForConversation(conversation.conversationId)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = SurfacePureWhite,
                shadowElevation = 2.dp,
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
                        modifier = Modifier.testTag("conv_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }

                    ContactAvatar(
                        name = conversation.contactName,
                        colorHex = conversation.avatarColorHex,
                        size = 40
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = conversation.contactName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Protected Backup",
                                tint = MintPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = conversation.contactPhone ?: "RCS Verified Backup",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextTertiaryDark,
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onGenerateReportClick,
                        modifier = Modifier.testTag("conv_detail_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "Generate PDF Report",
                            tint = MintPrimary
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("conv_detail_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Conversation",
                            tint = StatusError
                        )
                    }
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
        ) {
            // Backup Information Bar
            Surface(
                color = MintPrimaryLight.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MintPrimaryDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted On-Device Backup • ${messages.size} Messages Preserved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MintPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No backed-up messages in this thread.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiaryDark)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        MessageBubbleItem(message = msg)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Backed Up Thread?") },
            text = {
                Text("This will permanently remove this backed-up conversation and its messages from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(conversation.conversationId)
                        showDeleteConfirmDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubbleItem(message: MessageEntity) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val isIncoming = message.isIncoming
    val alignment = if (isIncoming) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isIncoming) 4.dp else 16.dp,
                bottomEnd = if (isIncoming) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isIncoming) BubbleIncoming else BubbleOutgoing
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isIncoming) BubbleIncomingBorder else MintPrimary.copy(alpha = 0.25f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .testTag("msg_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender label
                if (isIncoming) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MintPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Attached Media Box if present
                if (message.hasAttachment) {
                    MediaAttachmentBox(message = message)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text
                if (message.messageText.isNotBlank()) {
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimaryDark,
                            lineHeight = 19.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Footer with Timestamp & Status Check
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiaryDark,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Backed up",
                        tint = MintPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MediaAttachmentBox(message: MessageEntity) {
    val category = message.mediaType ?: "MEDIA"
    val (icon, labelColor) = when (category) {
        "IMAGE" -> Icons.Filled.Image to Color(0xFF0288D1)
        "VIDEO" -> Icons.Filled.Videocam to Color(0xFFE65100)
        "AUDIO" -> Icons.Filled.Mic to Color(0xFF7B1FA2)
        else -> Icons.Filled.Description to Color(0xFF00796B)
    }

    Surface(
        color = SurfaceVariantLight,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(labelColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.mediaFileName ?: "$category Attachment",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
                val sizeText = if (message.mediaSizeBytes > 0) {
                    MediaBackupManager.formatBytes(message.mediaSizeBytes)
                } else "Preserved in backup"
                Text(
                    text = "$category • $sizeText",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiaryDark,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
