package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.ReportEntity
import com.example.service.MediaBackupManager
import com.example.ui.theme.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PdfPreviewDialog(
    pdfFile: File,
    reportEntity: ReportEntity?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfacePureWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PDF Report Generated",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = pdfFile.name,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiaryDark),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PDF Visual Document Preview Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Simulated PDF Page Header
                        Surface(
                            color = MintPrimary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "RCS Backup & Recovery",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Official Android Backup Report • Verified Format",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MintPrimaryLight,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Box
                        Surface(
                            color = SurfacePureWhite,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("SUMMARY METADATA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MintPrimaryDark)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Target: ${reportEntity?.conversationName ?: "Complete Backup"}", fontSize = 12.sp)
                                Text("• Date Range: ${reportEntity?.dateRangeText ?: "All Time"}", fontSize = 12.sp)
                                Text("• Messages Included: ${reportEntity?.messageCount ?: "Yes"}", fontSize = 12.sp)
                                Text("• Media Preserved: ${reportEntity?.imageCount ?: 0} imgs, ${reportEntity?.videoCount ?: 0} vids, ${reportEntity?.audioCount ?: 0} audio", fontSize = 12.sp)
                                Text("• File Size: ${MediaBackupManager.formatBytes(pdfFile.length())}", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TRANSCRIPT PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextTertiaryDark
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = SurfacePureWhite,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("[Format: Verified Chronological Transcript]", fontSize = 10.sp, color = TextTertiaryDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✓ Ready for Legal, Audit, Record Keeping, or Sharing", fontSize = 12.sp, color = MintPrimaryDark, fontWeight = FontWeight.SemiBold)
                                Text("✓ Embedded Timestamps & Sender Identifiers", fontSize = 12.sp)
                                Text("✓ Media Inventory References & Hashes", fontSize = 12.sp)
                            }
                        }

                        if (savedMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = savedMessage!!,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Share, Print, Save to Device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save to Device
                    OutlinedButton(
                        onClick = {
                            try {
                                val downloadsDir = context.getExternalFilesDir(null) ?: context.filesDir
                                val destFile = File(downloadsDir, pdfFile.name)
                                FileInputStream(pdfFile).use { input ->
                                    FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                savedMessage = "Saved to: ${destFile.name}"
                            } catch (e: Exception) {
                                savedMessage = "Error saving file: ${e.message}"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pdf_save_btn")
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PDF", fontSize = 12.sp)
                    }

                    // Share PDF
                    Button(
                        onClick = {
                            try {
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "RCS Backup Report")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pdf_share_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 12.sp)
                    }

                    // Print PDF
                    IconButton(
                        onClick = {
                            try {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                printManager?.let { pm ->
                                    val printAdapter = object : PrintDocumentAdapter() {
                                        override fun onLayout(
                                            oldAttributes: PrintAttributes?,
                                            newAttributes: PrintAttributes?,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: LayoutResultCallback?,
                                            extras: android.os.Bundle?
                                        ) {
                                            callback?.onLayoutFinished(
                                                android.print.PrintDocumentInfo.Builder(pdfFile.name)
                                                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                    .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                                    .build(),
                                                true
                                            )
                                        }

                                        override fun onWrite(
                                            pages: Array<out android.print.PageRange>?,
                                            destination: android.os.ParcelFileDescriptor?,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: WriteResultCallback?
                                        ) {
                                            try {
                                                FileInputStream(pdfFile).use { input ->
                                                    FileOutputStream(destination?.fileDescriptor).use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                            } catch (e: Exception) {
                                                callback?.onWriteFailed(e.message)
                                            }
                                        }
                                    }
                                    pm.print("RCS_Backup_Report", printAdapter, PrintAttributes.Builder().build())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.testTag("pdf_print_btn")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print PDF", tint = MintPrimary)
                    }
                }
            }
        }
    }
}
