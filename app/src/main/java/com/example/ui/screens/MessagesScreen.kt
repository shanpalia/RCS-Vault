package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.ContactAvatar
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    onOpenConversation: (ConversationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            val q = searchQuery.trim().lowercase()
            conversations.filter {
                it.contactName.lowercase().contains(q) ||
                        (it.contactPhone?.contains(q) == true) ||
                        it.lastMessage.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        AppHeader(
            title = "Backed Up Messages",
            subtitle = "${conversations.size} conversation${if (conversations.size == 1) "" else "s"} preserved"
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, number, message...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = TextTertiaryDark
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfacePureWhite,
                unfocusedContainerColor = SurfacePureWhite,
                focusedBorderColor = MintPrimary,
                unfocusedBorderColor = OutlineBorder
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("messages_search_bar")
        )

        if (filteredConversations.isEmpty()) {
            if (searchQuery.isNotEmpty()) {
                EmptyStateWidget(
                    title = "No matching conversations",
                    description = "No backed-up messages found matching \"$searchQuery\"",
                    actionText = "Clear Search",
                    icon = Icons.Outlined.SearchOff,
                    onActionClick = { viewModel.setSearchQuery("") }
                )
            } else {
                EmptyStateWidget(
                    title = "Your backup is empty",
                    description = "Turn on Auto Backup to start preserving your RCS/SMS messages and media automatically.",
                    actionText = "Enable Auto Backup",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    onActionClick = { viewModel.toggleAutoBackup(true) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filteredConversations,
                    key = { it.conversationId }
                ) { conversation ->
                    ConversationItemCard(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItemCard(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(conversation.lastTimestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
            .testTag("conversation_card_${conversation.conversationId}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = conversation.contactName,
                colorHex = conversation.avatarColorHex,
                size = 48
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiaryDark,
                            fontSize = 11.sp
                        )
                    )
                }

                if (!conversation.contactPhone.isNullOrBlank()) {
                    Text(
                        text = conversation.contactPhone,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MintPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = conversation.lastMessage.ifEmpty { "Attachment preserved" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MintPrimaryLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${conversation.messageCount} msg${if (conversation.messageCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MintPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (conversation.mediaCount > 0) {
                        Surface(
                            color = Color(0xFFE1F5FE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${conversation.mediaCount} media",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF0277BD),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = conversation.appSource,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiaryDark,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
