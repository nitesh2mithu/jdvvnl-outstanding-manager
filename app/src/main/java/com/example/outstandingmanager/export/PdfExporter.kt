package com.example.outstandingmanager.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.ui.common.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfOrientation {
    AUTO,
    PORTRAIT,
    LANDSCAPE
}

object PdfExporter {

    // A4 dimensions at 72 DPI (PostScript points)
    private const val A4_WIDTH_PORTRAIT = 595
    private const val A4_HEIGHT_PORTRAIT = 842

    private const val A4_WIDTH_LANDSCAPE = 842
    private const val A4_HEIGHT_LANDSCAPE = 595

    private const val MARGIN = 30f
    private const val ROW_HEIGHT = 20f
    private const val HEADER_ROW_HEIGHT = 22f

    fun exportPdf(
        context: Context,
        datasetName: String,
        records: List<ConsumerRecord>,
        selectedColumns: List<String>,
        filterCriteria: FilterCriteria,
        orientation: PdfOrientation = PdfOrientation.AUTO
    ): File {
        val useLandscape = when (orientation) {
            PdfOrientation.LANDSCAPE -> true
            PdfOrientation.PORTRAIT -> false
            PdfOrientation.AUTO -> selectedColumns.size >= 5
        }

        val pageWidth = if (useLandscape) A4_WIDTH_LANDSCAPE else A4_WIDTH_PORTRAIT
        val pageHeight = if (useLandscape) A4_HEIGHT_LANDSCAPE else A4_HEIGHT_PORTRAIT

        val pdfDoc = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(70, 70, 70)
            textSize = 9.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val filterPaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val thBgPaint = Paint().apply {
            color = Color.rgb(240, 242, 245)
            style = Paint.Style.FILL
        }

        val thTextPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cellTextPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val cellRightPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(220, 224, 230)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        val totalPaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.rgb(130, 130, 130)
            textSize = 8f
            isAntiAlias = true
        }

        val usableWidth = pageWidth - (2 * MARGIN)

        // Calculate column widths
        val colWeights = selectedColumns.map { col ->
            when (col.lowercase()) {
                "consumer name", "name", "address" -> 2.5f
                "kno", "account no", "accno" -> 1.5f
                "outstanding amount", "amount" -> 1.5f
                "status", "app status" -> 1.3f
                "remark" -> 2.2f
                "feeder name", "feeder" -> 1.5f
                "mobile no", "mobile" -> 1.2f
                else -> 1.0f
            }
        }
        val totalWeight = colWeights.sum()
        val colWidths = colWeights.map { (it / totalWeight) * usableWidth }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        // Build filter summary string
        val filterParts = mutableListOf<String>()
        if (filterCriteria.query.isNotBlank()) filterParts.add("Search: \"${filterCriteria.query}\"")
        val minAmt = filterCriteria.effectiveMinAmount
        if (minAmt != null) filterParts.add("Dues > ₹${minAmt.toInt()}")
        val maxAmt = filterCriteria.maxAmount
        if (maxAmt != null) filterParts.add("Dues < ₹${maxAmt.toInt()}")
        if (!filterCriteria.feeder.isNullOrBlank()) filterParts.add("Feeder: ${filterCriteria.feeder}")
        if (!filterCriteria.appStatus.isNullOrBlank()) filterParts.add("Status: ${filterCriteria.appStatus}")
        val filterSummary = if (filterParts.isNotEmpty()) "Filters: " + filterParts.joinToString(" | ") else "Filters: None (All active)"

        // Pagination
        var pageNum = 1
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN

        fun startNewPage() {
            if (currentPage != null) {
                // Draw footer on previous page
                canvas?.drawText("Page $pageNum", pageWidth - MARGIN - 40f, pageHeight - MARGIN / 2, footerPaint)
                canvas?.drawText("Generated on $generatedDate • Outstanding Manager • build by nitesh2mithu", MARGIN, pageHeight - MARGIN / 2, footerPaint)
                pdfDoc.finishPage(currentPage)
                pageNum++
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val newPage = pdfDoc.startPage(pageInfo)
            currentPage = newPage
            val c = newPage.canvas
            canvas = c
            y = MARGIN

            // Draw Header on every page
            c.drawText("OUTSTANDING MANAGER", MARGIN, y + 12f, titlePaint)
            c.drawText("JdVVNL Outstanding Consumer List • Dataset: $datasetName", MARGIN, y + 26f, subtitlePaint)
            c.drawText(filterSummary, MARGIN, y + 38f, filterPaint)

            y += 48f

            // Draw Table Headers
            c.drawRect(MARGIN, y, MARGIN + usableWidth, y + HEADER_ROW_HEIGHT, thBgPaint)
            c.drawRect(MARGIN, y, MARGIN + usableWidth, y + HEADER_ROW_HEIGHT, borderPaint)

            var x = MARGIN
            for (i in selectedColumns.indices) {
                val colName = selectedColumns[i]
                val cw = colWidths[i]
                val textX = x + 4f
                val textY = y + 14f

                // Clip and draw header
                val bounds = Rect(x.toInt(), y.toInt(), (x + cw).toInt(), (y + HEADER_ROW_HEIGHT).toInt())
                c.save()
                c.clipRect(bounds)
                c.drawText(colName, textX, textY, thTextPaint)
                c.restore()

                c.drawLine(x + cw, y, x + cw, y + HEADER_ROW_HEIGHT, borderPaint)
                x += cw
            }
            y += HEADER_ROW_HEIGHT
        }

        startNewPage()

        val totalOutstanding = records.sumOf { it.outstandingAmount }

        for (rec in records) {
            // Check if page overflow
            if (y + ROW_HEIGHT > pageHeight - MARGIN - 40f) {
                startNewPage()
            }

            // Draw alternating row background
            var x = MARGIN
            for (i in selectedColumns.indices) {
                val colName = selectedColumns[i]
                val cw = colWidths[i]
                val cellVal = getCellDisplayValue(rec, colName)
                val isNumeric = colName.contains("amount", ignoreCase = true) || colName.contains("dues", ignoreCase = true)

                val bounds = Rect(x.toInt(), y.toInt(), (x + cw).toInt(), (y + ROW_HEIGHT).toInt())
                val c = canvas ?: continue
                c.save()
                c.clipRect(bounds)

                if (isNumeric) {
                    c.drawText(cellVal, x + cw - 4f, y + 13f, cellRightPaint)
                } else {
                    c.drawText(cellVal, x + 4f, y + 13f, cellTextPaint)
                }
                c.restore()

                // Cell borders
                c.drawRect(x, y, x + cw, y + ROW_HEIGHT, borderPaint)
                x += cw
            }
            y += ROW_HEIGHT
        }

        // Draw Totals section at end of table
        if (y + 40f > pageHeight - MARGIN - 30f) {
            startNewPage()
        }
        y += 15f
        canvas?.drawText("Total Consumers: ${records.size}", MARGIN, y + 12f, totalPaint)
        canvas?.drawText("Total Outstanding: ${CurrencyFormatter.formatIndianCurrency(totalOutstanding)}", MARGIN + 200f, y + 12f, totalPaint)

        // Footer on final page
        canvas?.drawText("Page $pageNum", pageWidth - MARGIN - 40f, pageHeight - MARGIN / 2, footerPaint)
        canvas?.drawText("Generated on $generatedDate • Outstanding Manager • build by nitesh2mithu", MARGIN, pageHeight - MARGIN / 2, footerPaint)
        if (currentPage != null) {
            pdfDoc.finishPage(currentPage)
        }

        // Save to file
        val outputDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val outputFile = File(outputDir, "Outstanding_List_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outputFile
    }

    private fun getCellDisplayValue(rec: ConsumerRecord, colName: String): String {
        return when (colName.lowercase()) {
            "kno" -> rec.kno
            "accno", "account no", "account" -> rec.accountNo
            "consumer name", "name" -> rec.consumerName
            "father's name", "father name" -> rec.fatherName
            "address" -> rec.address
            "feeder name", "feeder" -> rec.feederName
            "mobile no", "mobile" -> rec.mobileNo
            "tariffcode", "tariff" -> rec.tariffCode
            "consumer status" -> rec.consumerStatus
            "meter no", "meter" -> rec.meterNo
            "meter status" -> rec.meterStatus
            "outstanding amount", "amount" -> CurrencyFormatter.formatIndianCurrency(rec.outstandingAmount)
            "last received amount", "last received" -> if (rec.lastReceivedAmount != null) CurrencyFormatter.formatIndianCurrency(rec.lastReceivedAmount) else ""
            "last_paid", "last paid" -> rec.lastPaidDate
            "dues since", "dues_since" -> rec.duesSince
            "status", "app status" -> rec.displayStatus
            "remark" -> rec.remark
            else -> rec.dynamicFields[colName] ?: ""
        }
    }

    /**
     * Shares or opens the PDF via Android Share Sheet
     */
    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "JdVVNL Outstanding List")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Outstanding List PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
