package com.example.outstandingmanager.data.parser

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ParsedXlsResult(
    val fileName: String,
    val totalRowsFound: Int,
    val headerRowIndex: Int,
    val headers: List<String>,
    val rawRows: List<List<String>>
)

object XlsParser {

    private val OLE2_SIGNATURE = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
    )

    private val HEADER_KEYWORDS = listOf(
        "kno", "accno", "account", "consumer", "name", "address",
        "feeder", "mobile", "tariff", "meter", "outstanding",
        "amount", "dues", "si no", "s.no", "bill", "status"
    )

    fun parse(inputStream: InputStream, fileName: String): ParsedXlsResult {
        val bytes = inputStream.readBytes()
        if (bytes.size < 8) {
            throw IllegalArgumentException("File is empty or invalid.")
        }

        val rows: List<List<String>> = if (isOle2(bytes)) {
            parseBiff8(bytes)
        } else {
            // Fallback for HTML table or CSV/TSV with .xls extension
            parseTextOrHtmlTable(bytes)
        }

        if (rows.isEmpty()) {
            throw IllegalArgumentException("No data rows found in this file.")
        }

        val headerRowIdx = detectHeaderRowIndex(rows)
        val headers = if (headerRowIdx in rows.indices) {
            rows[headerRowIdx].map { it.trim() }
        } else {
            // Fallback: generate col_1, col_2...
            val maxCols = rows.maxOfOrNull { it.size } ?: 0
            (1..maxCols).map { "Column $it" }
        }

        // Data rows starting after header
        val dataRows = if (headerRowIdx in rows.indices && headerRowIdx + 1 < rows.size) {
            rows.subList(headerRowIdx + 1, rows.size).filter { row ->
                row.any { it.isNotBlank() }
            }
        } else {
            rows
        }

        return ParsedXlsResult(
            fileName = fileName,
            totalRowsFound = dataRows.size,
            headerRowIndex = headerRowIdx,
            headers = headers,
            rawRows = dataRows
        )
    }

    private fun isOle2(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        for (i in 0 until 8) {
            if (bytes[i] != OLE2_SIGNATURE[i]) return false
        }
        return true
    }

    /**
     * Parses OLE2 compound file to extract Workbook stream and decode BIFF8 cells
     */
    private fun parseBiff8(data: ByteArray): List<List<String>> {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val sectorSize = 1 shl buf.getShort(30).toInt()
        val numFatSectors = buf.getInt(44)
        val dirStartSector = buf.getInt(48)

        // Read MSAT / DIFAT
        val fatSectors = mutableListOf<Int>()
        for (i in 0 until 109) {
            val s = buf.getInt(76 + i * 4)
            val u = s.toLong() and 0xFFFFFFFFL
            if (u < 0xFFFFFFFDL) {
                fatSectors.add(s)
            }
        }

        // Read FAT
        val fat = IntArray(fatSectors.size * (sectorSize / 4))
        var fatIdx = 0
        for (sec in fatSectors) {
            val secOffset = (sec + 1) * sectorSize
            if (secOffset + sectorSize <= data.size) {
                val secBuf = ByteBuffer.wrap(data, secOffset, sectorSize).order(ByteOrder.LITTLE_ENDIAN)
                while (secBuf.hasRemaining() && fatIdx < fat.size) {
                    fat[fatIdx++] = secBuf.getInt()
                }
            }
        }

        fun getChain(startSector: Int): List<Int> {
            val chain = mutableListOf<Int>()
            var cur = startSector
            val visited = mutableSetOf<Int>()
            while (cur >= 0 && (cur.toLong() and 0xFFFFFFFFL) < 0xFFFFFFFDL && cur < fat.size && visited.add(cur)) {
                chain.add(cur)
                cur = fat[cur]
            }
            return chain
        }

        // Read Directory
        val dirChain = getChain(dirStartSector)
        val dirData = ByteArray(dirChain.size * sectorSize)
        var dirOffset = 0
        for (sec in dirChain) {
            val secOffset = (sec + 1) * sectorSize
            if (secOffset + sectorSize <= data.size) {
                System.arraycopy(data, secOffset, dirData, dirOffset, sectorSize)
                dirOffset += sectorSize
            }
        }

        var workbookStartSector = -1
        var workbookSize = 0L

        val dirBuf = ByteBuffer.wrap(dirData).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until dirData.size step 128) {
            if (i + 128 > dirData.size) break
            val nameLen = dirBuf.getShort(i + 64).toInt()
            if (nameLen > 0) {
                val nameBytes = ByteArray(nameLen - 2)
                System.arraycopy(dirData, i, nameBytes, 0, nameLen - 2)
                val name = String(nameBytes, Charsets.UTF_16LE)
                if (name.equals("Workbook", ignoreCase = true) || name.equals("Book", ignoreCase = true)) {
                    workbookStartSector = dirBuf.getInt(i + 116)
                    workbookSize = dirBuf.getLong(i + 120)
                    break
                }
            }
        }

        if (workbookStartSector < 0) {
            return emptyList()
        }

        // Read Workbook stream
        val wbChain = getChain(workbookStartSector)
        val wbTotalLen = (wbChain.size * sectorSize).coerceAtMost(workbookSize.toInt())
        val wbData = ByteArray(wbTotalLen)
        var wbPos = 0
        for (sec in wbChain) {
            val secOffset = (sec + 1) * sectorSize
            val toCopy = (wbTotalLen - wbPos).coerceAtMost(sectorSize)
            if (toCopy > 0 && secOffset + toCopy <= data.size) {
                System.arraycopy(data, secOffset, wbData, wbPos, toCopy)
                wbPos += toCopy
            }
        }

        return decodeBiffWorkbook(wbData)
    }

    private fun decodeBiffWorkbook(wb: ByteArray): List<List<String>> {
        val sstStrings = mutableListOf<String>()
        var offset = 0

        // Pass 1: Parse Shared String Table (SST)
        while (offset + 4 <= wb.size) {
            val recType = (wb[offset].toInt() and 0xFF) or ((wb[offset + 1].toInt() and 0xFF) shl 8)
            val recLen = (wb[offset + 2].toInt() and 0xFF) or ((wb[offset + 3].toInt() and 0xFF) shl 8)

            if (recType == 0x00FC) { // SST
                val sstBytes = mutableListOf<Byte>()
                // Copy rec data
                val end = (offset + 4 + recLen).coerceAtMost(wb.size)
                for (b in (offset + 4 + 8) until end) {
                    sstBytes.add(wb[b])
                }
                var continueOffset = offset + 4 + recLen
                while (continueOffset + 4 <= wb.size) {
                    val nextType = (wb[continueOffset].toInt() and 0xFF) or ((wb[continueOffset + 1].toInt() and 0xFF) shl 8)
                    val nextLen = (wb[continueOffset + 2].toInt() and 0xFF) or ((wb[continueOffset + 3].toInt() and 0xFF) shl 8)
                    if (nextType == 0x003C) { // CONTINUE
                        val contEnd = (continueOffset + 4 + nextLen).coerceAtMost(wb.size)
                        for (b in (continueOffset + 4) until contEnd) {
                            sstBytes.add(wb[b])
                        }
                        continueOffset += 4 + nextLen
                    } else {
                        break
                    }
                }

                val rawSst = sstBytes.toByteArray()
                var p = 0
                while (p + 2 <= rawSst.size) {
                    val cch = (rawSst[p].toInt() and 0xFF) or ((rawSst[p + 1].toInt() and 0xFF) shl 8)
                    p += 2
                    if (p >= rawSst.size) break
                    val grbit = rawSst[p].toInt() and 0xFF
                    p += 1

                    val isUnicode = (grbit and 1) != 0
                    val isRich = (grbit and 8) != 0
                    val isExt = (grbit and 4) != 0

                    var cRun = 0
                    if (isRich && p + 2 <= rawSst.size) {
                        cRun = (rawSst[p].toInt() and 0xFF) or ((rawSst[p + 1].toInt() and 0xFF) shl 8)
                        p += 2
                    }
                    var cbExt = 0
                    if (isExt && p + 4 <= rawSst.size) {
                        cbExt = (rawSst[p].toInt() and 0xFF) or
                                ((rawSst[p + 1].toInt() and 0xFF) shl 8) or
                                ((rawSst[p + 2].toInt() and 0xFF) shl 16) or
                                ((rawSst[p + 3].toInt() and 0xFF) shl 24)
                        p += 4
                    }

                    var charsLeft = cch
                    val strBytes = mutableListOf<Byte>()
                    while (charsLeft > 0 && p < rawSst.size) {
                        if (isUnicode) {
                            val take = (charsLeft * 2).coerceAtMost(rawSst.size - p)
                            for (k in 0 until take) strBytes.add(rawSst[p + k])
                            p += take
                            charsLeft -= take / 2
                        } else {
                            val take = charsLeft.coerceAtMost(rawSst.size - p)
                            for (k in 0 until take) strBytes.add(rawSst[p + k])
                            p += take
                            charsLeft -= take
                        }
                    }

                    val s = if (isUnicode) {
                        String(strBytes.toByteArray(), Charsets.UTF_16LE)
                    } else {
                        String(strBytes.toByteArray(), Charsets.ISO_8859_1)
                    }
                    p += cRun * 4 + cbExt
                    sstStrings.add(s)
                }
                break
            }
            offset += 4 + recLen
        }

        // Pass 2: Parse Cells
        val cellMap = mutableMapOf<Pair<Int, Int>, String>()
        var maxRow = 0
        var maxCol = 0
        offset = 0

        while (offset + 4 <= wb.size) {
            val recType = (wb[offset].toInt() and 0xFF) or ((wb[offset + 1].toInt() and 0xFF) shl 8)
            val recLen = (wb[offset + 2].toInt() and 0xFF) or ((wb[offset + 3].toInt() and 0xFF) shl 8)
            val p = offset + 4

            if (p + recLen <= wb.size) {
                when (recType) {
                    0x00FD -> { // LABELSST
                        if (recLen >= 10) {
                            val row = (wb[p].toInt() and 0xFF) or ((wb[p + 1].toInt() and 0xFF) shl 8)
                            val col = (wb[p + 2].toInt() and 0xFF) or ((wb[p + 3].toInt() and 0xFF) shl 8)
                            val sstIdx = (wb[p + 6].toInt() and 0xFF) or
                                    ((wb[p + 7].toInt() and 0xFF) shl 8) or
                                    ((wb[p + 8].toInt() and 0xFF) shl 16) or
                                    ((wb[p + 9].toInt() and 0xFF) shl 24)
                            val str = if (sstIdx in sstStrings.indices) sstStrings[sstIdx] else ""
                            cellMap[Pair(row, col)] = str
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                    0x0204 -> { // LABEL
                        if (recLen >= 8) {
                            val row = (wb[p].toInt() and 0xFF) or ((wb[p + 1].toInt() and 0xFF) shl 8)
                            val col = (wb[p + 2].toInt() and 0xFF) or ((wb[p + 3].toInt() and 0xFF) shl 8)
                            val cch = (wb[p + 6].toInt() and 0xFF) or ((wb[p + 7].toInt() and 0xFF) shl 8)
                            val isUnicode = if (recLen >= 9) (wb[p + 8].toInt() and 1) != 0 else false
                            val strOffset = if (recLen >= 9) p + 9 else p + 8
                            val str = if (isUnicode && strOffset + cch * 2 <= wb.size) {
                                String(wb, strOffset, cch * 2, Charsets.UTF_16LE)
                            } else if (!isUnicode && strOffset + cch <= wb.size) {
                                String(wb, strOffset, cch, Charsets.ISO_8859_1)
                            } else {
                                ""
                            }
                            cellMap[Pair(row, col)] = str
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                    0x0203 -> { // NUMBER
                        if (recLen >= 14) {
                            val row = (wb[p].toInt() and 0xFF) or ((wb[p + 1].toInt() and 0xFF) shl 8)
                            val col = (wb[p + 2].toInt() and 0xFF) or ((wb[p + 3].toInt() and 0xFF) shl 8)
                            val numBuf = ByteBuffer.wrap(wb, p + 6, 8).order(ByteOrder.LITTLE_ENDIAN)
                            val dbl = numBuf.getDouble()
                            val str = if (dbl == dbl.toLong().toDouble()) dbl.toLong().toString() else dbl.toString()
                            cellMap[Pair(row, col)] = str
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                    0x027E -> { // RK
                        if (recLen >= 10) {
                            val row = (wb[p].toInt() and 0xFF) or ((wb[p + 1].toInt() and 0xFF) shl 8)
                            val col = (wb[p + 2].toInt() and 0xFF) or ((wb[p + 3].toInt() and 0xFF) shl 8)
                            val rkVal = (wb[p + 6].toInt() and 0xFF) or
                                    ((wb[p + 7].toInt() and 0xFF) shl 8) or
                                    ((wb[p + 8].toInt() and 0xFF) shl 16) or
                                    ((wb[p + 9].toInt() and 0xFF) shl 24)
                            val num = decodeRk(rkVal)
                            val str = if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                            cellMap[Pair(row, col)] = str
                            if (row > maxRow) maxRow = row
                            if (col > maxCol) maxCol = col
                        }
                    }
                    0x00BD -> { // MULRK
                        if (recLen >= 6) {
                            val row = (wb[p].toInt() and 0xFF) or ((wb[p + 1].toInt() and 0xFF) shl 8)
                            val colFirst = (wb[p + 2].toInt() and 0xFF) or ((wb[p + 3].toInt() and 0xFF) shl 8)
                            val colLast = (wb[p + recLen - 2].toInt() and 0xFF) or ((wb[p + recLen - 1].toInt() and 0xFF) shl 8)
                            var curCol = colFirst
                            var byteIdx = p + 4
                            while (curCol <= colLast && byteIdx + 6 <= p + recLen) {
                                val rkVal = (wb[byteIdx + 2].toInt() and 0xFF) or
                                        ((wb[byteIdx + 3].toInt() and 0xFF) shl 8) or
                                        ((wb[byteIdx + 4].toInt() and 0xFF) shl 16) or
                                        ((wb[byteIdx + 5].toInt() and 0xFF) shl 24)
                                val num = decodeRk(rkVal)
                                val str = if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                                cellMap[Pair(row, curCol)] = str
                                if (curCol > maxCol) maxCol = curCol
                                curCol++
                                byteIdx += 6
                            }
                            if (row > maxRow) maxRow = row
                        }
                    }
                }
            }
            offset += 4 + recLen
        }

        if (cellMap.isEmpty()) return emptyList()

        val resultRows = mutableListOf<List<String>>()
        for (r in 0..maxRow) {
            val rowValues = mutableListOf<String>()
            var hasAnyData = false
            for (c in 0..maxCol) {
                val v = cellMap[Pair(r, c)] ?: ""
                if (v.isNotBlank()) hasAnyData = true
                rowValues.add(v)
            }
            if (hasAnyData) {
                resultRows.add(rowValues)
            }
        }
        return resultRows
    }

    private fun decodeRk(rkVal: Int): Double {
        val is100 = (rkVal and 1) != 0
        val isInt = (rkVal and 2) != 0
        val raw = rkVal and 3.inv()
        var valDbl: Double
        if (isInt) {
            var v = raw shr 2
            if ((v and 0x20000000) != 0) {
                v -= 0x40000000
            }
            valDbl = v.toDouble()
        } else {
            val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0)
            buf.putInt(raw)
            buf.flip()
            valDbl = buf.getDouble()
        }
        if (is100) {
            valDbl /= 100.0
        }
        return valDbl
    }

    /**
     * Fallback for HTML tables or CSVs masquerading as .xls
     */
    private fun parseTextOrHtmlTable(bytes: ByteArray): List<List<String>> {
        val text = String(bytes, Charsets.UTF_8)
        if (text.contains("<table", ignoreCase = true)) {
            val rows = mutableListOf<List<String>>()
            val trRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            val tdRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", RegexOption.DOT_MATCHES_ALL)

            for (trMatch in trRegex.findAll(text)) {
                val rowHtml = trMatch.groupValues[1]
                val rowCells = mutableListOf<String>()
                for (tdMatch in tdRegex.findAll(rowHtml)) {
                    val cellText = tdMatch.groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .trim()
                    rowCells.add(cellText)
                }
                if (rowCells.any { it.isNotBlank() }) {
                    rows.add(rowCells)
                }
            }
            return rows
        }

        // CSV / TSV fallback
        return text.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val delimiter = if (line.contains('\t')) '\t' else ','
                line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            }
            .toList()
    }

    /**
     * Intelligently detects the actual table header row index.
     * Looks for row with highest count of known keywords (KNO, Accno, Consumer Name, etc.)
     */
    fun detectHeaderRowIndex(rows: List<List<String>>): Int {
        var bestRow = 0
        var bestScore = 0

        for (i in 0 until minOf(rows.size, 15)) {
            val row = rows[i]
            var score = 0
            for (cell in row) {
                val lower = cell.lowercase().trim()
                if (HEADER_KEYWORDS.any { lower.contains(it) }) {
                    score++
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestRow = i
            }
        }

        return bestRow
    }
}
