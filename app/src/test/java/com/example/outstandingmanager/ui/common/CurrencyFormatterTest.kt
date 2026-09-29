package com.example.outstandingmanager.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testIndianCurrencyFormatting() {
        assertEquals("₹17,65,872", CurrencyFormatter.formatIndianCurrency(1765872.0))
        assertEquals("₹5,42,100", CurrencyFormatter.formatIndianCurrency(542100.0))
        assertEquals("₹9,850", CurrencyFormatter.formatIndianCurrency(9850.0))
        assertEquals("₹1,00,000", CurrencyFormatter.formatIndianCurrency(100000.0))
        assertEquals("₹5,000", CurrencyFormatter.formatIndianCurrency(5000.0))
        assertEquals("₹0", CurrencyFormatter.formatIndianCurrency(0.0))
        assertEquals("₹450", CurrencyFormatter.formatIndianCurrency(450.0))
    }

    @Test
    fun testParseAmount() {
        assertEquals(1765872.0, CurrencyFormatter.parseAmount("₹17,65,872"), 0.001)
        assertEquals(1765872.0, CurrencyFormatter.parseAmount("1765872.00"), 0.001)
        assertEquals(1765872.0, CurrencyFormatter.parseAmount("17,65,872"), 0.001)
        assertEquals(542100.0, CurrencyFormatter.parseAmount("Rs. 5,42,100"), 0.001)
        assertEquals(0.0, CurrencyFormatter.parseAmount(""), 0.001)
        assertEquals(0.0, CurrencyFormatter.parseAmount(null), 0.001)
    }
}
