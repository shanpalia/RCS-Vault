package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.db.AppDatabase
import com.example.data.model.MediaFileEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ReportEntity
import com.example.service.MediaBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val reportDao = database.reportDao()

    companion object {
        private const val PAGE_WIDTH = 595 // Standard A4 points (72 dpi)
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    }

    /**
     * Generates a professional multi-page PDF report.
     */
    suspend fun generateReport(
        title: String,
        targetConversationId: String?,
        conversationName: String,
        contactPhone: String?,
        dateRangeText: String,
        messages: List<MessageEntity>,
        mediaFiles: List<MediaFileEntity>,
        includeMessages: Boolean = true,
        includeMedia: Boolean = true
    ): File = withContext(Dispatchers.IO) {
        val reportId = "RCS-REP-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}"
        val reportsDir = File(context.filesDir, "rcs_backup/reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, "${reportId}.pdf")
        val document = PdfDocument()

        val imageCount = mediaFiles.count { it.category == "IMAGE" }
        val videoCount = mediaFiles.count { it.category == "VIDEO" }
        val audioCount = mediaFiles.count { it.category == "AUDIO" }
        val docCount = mediaFiles.count { it.category == "DOCUMENT" }

        // Paints
        val brandPaint = Paint().apply {
            color = Color.rgb(0, 137, 123) // #00897B Mint / Emerald
            isAntiAlias = true
        }
        val darkTextPaint = Paint().apply {
            color = Color.rgb(25, 28, 27)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderPaint = Paint().apply {
            color = Color.rgb(224, 242, 241)
            textSize = 10f
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(0, 91, 79)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            isAntiAlias = true
        }
        val boxBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 243)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(218, 229, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val bubbleIncomingPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val bubbleOutgoingPaint = Paint().apply {
            color = Color.rgb(216, 243, 220)
            isAntiAlias = true
        }
        val secondaryTextPaint = Paint().apply {
            color = Color.rgb(111, 121, 118)
            textSize = 8.5f
            isAntiAlias = true
        }
        val badgeBgPaint = Paint().apply {
            color = Color.rgb(224, 242, 241)
            isAntiAlias = true
        }
        val badgeTextPaint = Paint().apply {
            color = Color.rgb(0, 137, 123)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        // Helper to draw Header
        fun drawPageHeader(c: Canvas, isFirstPage: Boolean) {
            // Draw mint top bar
            val headerHeight = if (isFirstPage) 70f else 40f
            val headerRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight)
            c.drawRect(headerRect, brandPaint)

            if (isFirstPage) {
                c.drawText("RCS Vault", MARGIN, 28f, headerTitlePaint)
                c.drawText("Securely preserve what matters. • Verified Backup Transcript", MARGIN, 44f, subheaderPaint)
                c.drawText("Report ID: $reportId", MARGIN, 58f, subheaderPaint)
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
                val dateWidth = subheaderPaint.measureText("Generated: $dateStr")
                c.drawText("Generated: $dateStr", PAGE_WIDTH - MARGIN - dateWidth, 58f, subheaderPaint)
            } else {
                c.drawText("RCS Vault Report - $conversationName", MARGIN, 24f, headerTitlePaint.apply { textSize = 11f })
                val pageStr = "ID: $reportId"
                val strWidth = subheaderPaint.measureText(pageStr)
                c.drawText(pageStr, PAGE_WIDTH - MARGIN - strWidth, 24f, subheaderPaint)
            }
        }

        // Helper to draw Footer
        fun drawPageFooter(c: Canvas, pageNum: Int) {
            val footerY = PAGE_HEIGHT - 20f
            c.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, borderPaint)
            val discl = "Generated from data actually stored by RCS Vault."
            c.drawText(discl, MARGIN, footerY, secondaryTextPaint)
            val pageStr = "Page $pageNum"
            val pWidth = secondaryTextPaint.measureText(pageStr)
            c.drawText(pageStr, PAGE_WIDTH - MARGIN - pWidth, footerY, secondaryTextPaint)
        }

        drawPageHeader(canvas, true)
        y = 85f

        // Draw Summary Box on First Page
        val summaryBoxRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 95f)
        canvas.drawRoundRect(summaryBoxRect, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(summaryBoxRect, 8f, 8f, borderPaint)

        canvas.drawText("REPORT SUMMARY", MARGIN + 12f, y + 18f, sectionTitlePaint.apply { textSize = 10f })
        canvas.drawText("Conversation: $conversationName", MARGIN + 12f, y + 34f, darkTextPaint)
        val phoneDisplay = if (!contactPhone.isNullOrBlank()) contactPhone else "Not available"
        canvas.drawText("Phone: $phoneDisplay", MARGIN + 12f, y + 48f, darkTextPaint)
        canvas.drawText("Date Range: $dateRangeText", MARGIN + 12f, y + 62f, darkTextPaint)
        canvas.drawText("Storage Reference: On-device protected Room database", MARGIN + 12f, y + 76f, secondaryTextPaint)

        // Right column stats
        val rightX = MARGIN + CONTENT_WIDTH * 0.55f
        canvas.drawText("Total Messages: ${messages.size}", rightX, y + 34f, darkTextPaint)
        canvas.drawText("Images: $imageCount | Videos: $videoCount", rightX, y + 48f, darkTextPaint)
        canvas.drawText("Audio: $audioCount | Docs: $docCount", rightX, y + 62f, darkTextPaint)
        canvas.drawText("Source: Authorized Notification & Storage Events", rightX, y + 76f, secondaryTextPaint)

        y += 115f

        fun checkPageBreak(neededHeight: Float) {
            if (y + neededHeight > PAGE_HEIGHT - 40f) {
                drawPageFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawPageHeader(canvas, false)
                y = 55f
            }
        }

        if (messages.isEmpty() && mediaFiles.isEmpty()) {
            val emptyBoxRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 60f)
            canvas.drawRoundRect(emptyBoxRect, 8f, 8f, boxBgPaint)
            canvas.drawRoundRect(emptyBoxRect, 8f, 8f, borderPaint)
            canvas.drawText(
                "No backed-up data available for the selected period.",
                MARGIN + 16f,
                y + 35f,
                darkTextPaint.apply { typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) }
            )
            y += 80f
        }

        // Section: Messages
        if (includeMessages && messages.isNotEmpty()) {
            checkPageBreak(30f)
            canvas.drawText("CHRONOLOGICAL MESSAGE TRANSCRIPT", MARGIN, y, sectionTitlePaint.apply { textSize = 12f })
            y += 18f

            val timeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US)

            for (msg in messages) {
                val formattedTime = timeFormat.format(Date(msg.timestamp))
                val senderHeader = if (msg.isIncoming) "${msg.senderName} (Incoming)" else "Me (Outgoing)"
                val textLines = breakTextIntoLines(msg.messageText, CONTENT_WIDTH - 24f, darkTextPaint)
                val cardHeight = 24f + (textLines.size * 13f) + (if (msg.hasAttachment) 16f else 0f) + 10f

                checkPageBreak(cardHeight + 8f)

                val cardRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + cardHeight)
                val bgPaint = if (msg.isIncoming) bubbleIncomingPaint else bubbleOutgoingPaint
                canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)
                canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

                // Header in bubble
                canvas.drawText(senderHeader, MARGIN + 10f, y + 14f, darkTextPaint.apply { typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) })
                val timeW = secondaryTextPaint.measureText(formattedTime)
                canvas.drawText(formattedTime, PAGE_WIDTH - MARGIN - 10f - timeW, y + 14f, secondaryTextPaint)

                var textY = y + 27f
                darkTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                for (line in textLines) {
                    canvas.drawText(line, MARGIN + 10f, textY, darkTextPaint)
                    textY += 13f
                }

                if (msg.hasAttachment) {
                    val badgeRect = RectF(MARGIN + 10f, textY - 2f, MARGIN + 120f, textY + 12f)
                    canvas.drawRoundRect(badgeRect, 4f, 4f, badgeBgPaint)
                    canvas.drawText("📎 Attachment: ${msg.mediaType ?: "MEDIA"}", MARGIN + 14f, textY + 8f, badgeTextPaint)
                }

                y += cardHeight + 8f
            }
        }

        // Section: Media References
        if (includeMedia && mediaFiles.isNotEmpty()) {
            checkPageBreak(40f)
            y += 10f
            canvas.drawText("PRESERVED MEDIA INVENTORY", MARGIN, y, sectionTitlePaint.apply { textSize = 12f })
            y += 18f

            val mediaTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            for (media in mediaFiles) {
                val mediaHeight = 36f
                checkPageBreak(mediaHeight + 6f)

                val mRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + mediaHeight)
                canvas.drawRoundRect(mRect, 4f, 4f, boxBgPaint)
                canvas.drawRoundRect(mRect, 4f, 4f, borderPaint)

                // Type Icon / Tag
                val typeTag = "[${media.category}]"
                canvas.drawText(typeTag, MARGIN + 8f, y + 15f, sectionTitlePaint.apply { textSize = 9f })
                canvas.drawText(media.fileName, MARGIN + 55f, y + 15f, darkTextPaint.apply { typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) })

                val metaStr = "${MediaBackupManager.formatBytes(media.sizeBytes)} • ${mediaTimeFormat.format(Date(media.timestamp))} • SHA-256: ${media.sha256Hash.take(12)}..."
                canvas.drawText(metaStr, MARGIN + 8f, y + 28f, secondaryTextPaint)

                y += mediaHeight + 6f
            }
        }

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        // Write to output file
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        val reportEntity = ReportEntity(
            id = reportId,
            generatedTimestamp = System.currentTimeMillis(),
            title = title,
            targetType = if (targetConversationId == null) "ALL_CONVERSATIONS" else "SINGLE_CONVERSATION",
            conversationName = conversationName,
            contactPhone = contactPhone,
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            messageCount = messages.size,
            imageCount = imageCount,
            videoCount = videoCount,
            audioCount = audioCount,
            documentCount = docCount,
            dateRangeText = dateRangeText,
            status = "GENERATED"
        )
        reportDao.insertReport(reportEntity)

        pdfFile
    }

    private fun breakTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (para in paragraphs) {
            val words = para.split(" ")
            var currentLine = ""

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = paint.measureText(candidate)
                if (width <= maxWidth) {
                    currentLine = candidate
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                    }
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
        }
        return if (lines.isEmpty()) listOf("(No message text)") else lines
    }
}
