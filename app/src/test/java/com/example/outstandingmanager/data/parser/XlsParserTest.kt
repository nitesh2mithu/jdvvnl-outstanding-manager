package com.example.outstandingmanager.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class XlsParserTest {

    @Test
    fun testParseSampleXls() {
        var sampleFile = File("KNO Wise Oustanding Dues(web)_Sample.xls")
        if (!sampleFile.exists()) {
            sampleFile = File("../KNO Wise Oustanding Dues(web)_Sample.xls")
        }
        if (!sampleFile.exists()) {
            println("Sample file not found at working dir: ${sampleFile.absolutePath}")
            return
        }

        FileInputStream(sampleFile).use { stream ->
            val result = XlsParser.parse(stream, sampleFile.name)
            println("Parsed result:")
            println("File: ${result.fileName}")
            println("Detected header row index: ${result.headerRowIndex}")
            println("Headers found (${result.headers.size}): ${result.headers}")
            println("Data rows found: ${result.totalRowsFound}")

            // Assertions based on INSTRUCTION.MD
            assertEquals(3, result.headerRowIndex)
            assertEquals(32, result.headers.size)
            assertTrue("Expected around 1145 data records", result.totalRowsFound in 1140..1155)

            // Check first data row (Indian Oil Corporation)
            val firstRow = result.rawRows[0]
            println("First data row sample: $firstRow")
            assertTrue(firstRow.any { it.contains("Indian Oil", ignoreCase = true) })
        }
    }
}
