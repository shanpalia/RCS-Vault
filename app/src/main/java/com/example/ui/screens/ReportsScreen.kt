package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.ReportEntity
import com.example.service.MediaBackupManager
import com.example.ui.components.AppHeader
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PdfGenerationState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reports by viewModel.reports.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var previewReportEntity by remember { mutableStateOf<ReportEntity?>(null) }

    LaunchedEffect(pdfState) {
        if (pdfState is PdfGenerationState.Success) {
            val success = pdfState as PdfGenerationState.Success
            previewFile = success.file
            previewReportEntity = success.reportEntity
            viewModel.resetPdfState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        AppHeader(
            title = "PDF Reports",
            subtitle = "${reports.size} report${if (reports.size == 1) "" else "s"} generated"
        )

        // Prominent "Generate Report" Action Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MintPrimaryLight),
            border = androidx.compose.foundation.BorderStroke(1.dp, MintPrimary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MintPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Generate PDF Report",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MintPrimaryDark
                        )
                    )
                    Text(
                        text = "Create printable, verified PDF transcripts of entire backup or single chats.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showGenerateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("generate_report_main_btn")
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (pdfState is PdfGenerationState.Generating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = MintPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Compiling PDF report and media inventory...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (reports.isEmpty()) {
            EmptyStateWidget(
                title = "No Reports Generated Yet",
                description = "Tap 'Create' above to generate your first verified PDF backup report.",
                actionText = "Generate New Report",
                icon = Icons.Outlined.Description,
                onActionClick = { showGenerateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = reports, key = { it.id }) { report ->
                    ReportItemCard(
                        report = report,
                        onOpen = {
                            val f = File(report.filePath)
                            if (f.exists()) {
                                previewFile = f
                                previewReportEntity = report
                            } else {
                                viewModel.repository.pdfGenerator
                            }
                        },
                        onShare = {
                            try {
                                val file = File(report.filePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Report"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onDelete = { viewModel.deleteReport(report) }
                    )
                }
            }
        }
    }

    if (showGenerateDialog) {
        GenerateReportModal(
            conversations = conversations,
            onDismiss = { showGenerateDialog = false },
            onConfirm = { title, convId, convName, phone, range, incMsgs, incMedia ->
                viewModel.generatePdfReport(
                    title = title,
                    targetConversationId = convId,
                    conversationName = convName,
                    contactPhone = phone,
                    dateRangeText = range,
                    includeMessages = incMsgs,
                    includeMedia = incMedia
                )
                showGenerateDialog = false
            }
        )
    }

    if (previewFile != null) {
        PdfPreviewDialog(
            pdfFile = previewFile!!,
            reportEntity = previewReportEntity,
            onDismiss = {
                previewFile = null
                previewReportEntity = null
            }
        )
    }
}

@Composable
fun ReportItemCard(
    report: ReportEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = timeFormat.format(Date(report.generatedTimestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("report_item_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MintPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                    Text(
                        text = "${report.id} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiaryDark,
                            fontSize = 11.sp
                        )
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = MintPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = StatusError)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = SurfaceVariantLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target: ${report.conversationName}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("${report.messageCount} msgs • ${report.imageCount + report.videoCount + report.audioCount + report.documentCount} media", fontSize = 11.sp, color = MintPrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateReportModal(
    conversations: List<com.example.data.model.ConversationEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        targetConvId: String?,
        convName: String,
        phone: String?,
        dateRange: String,
        includeMessages: Boolean,
        includeMedia: Boolean
    ) -> Unit
) {
    var isEntireBackup by remember { mutableStateOf(true) }
    var selectedConversationId by remember { mutableStateOf(conversations.firstOrNull()?.conversationId ?: "") }
    var selectedDateRange by remember { mutableStateOf("All Time") }
    var includeMessages by remember { mutableStateOf(true) }
    var includeMedia by remember { mutableStateOf(true) }
    var reportTitle by remember { mutableStateOf("RCS Backup Transcript") }

    val dateRanges = listOf("All Time", "Last 7 Days", "Last 30 Days")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("PDF Report Generator", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    label = { Text("Report Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Scope:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isEntireBackup, onClick = { isEntireBackup = true })
                    Text("Entire Backup", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isEntireBackup, onClick = { isEntireBackup = false })
                    Text("One Conversation", style = MaterialTheme.typography.bodyMedium)
                }

                if (!isEntireBackup && conversations.isNotEmpty()) {
                    Text("Select Conversation:", style = MaterialTheme.typography.labelSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        conversations.take(4).forEach { conv ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedConversationId = conv.conversationId }
                            ) {
                                RadioButton(
                                    selected = selectedConversationId == conv.conversationId,
                                    onClick = { selectedConversationId = conv.conversationId }
                                )
                                Text(conv.contactName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Text("Date Range:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dateRanges.forEach { range ->
                        FilterChip(
                            selected = selectedDateRange == range,
                            onClick = { selectedDateRange = range },
                            label = { Text(range, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Content Options:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMessages, onCheckedChange = { includeMessages = it })
                    Text("Include Messages", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(checked = includeMedia, onCheckedChange = { includeMedia = it })
                    Text("Include Media", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetConv = if (isEntireBackup) null else conversations.find { it.conversationId == selectedConversationId }
                    val targetName = if (isEntireBackup) "All Conversations" else targetConv?.contactName ?: "Selected Chat"
                    val phone = targetConv?.contactPhone

                    onConfirm(
                        reportTitle,
                        if (isEntireBackup) null else selectedConversationId,
                        targetName,
                        phone,
                        selectedDateRange,
                        includeMessages,
                        includeMedia
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                modifier = Modifier.testTag("submit_generate_pdf_btn")
            ) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
