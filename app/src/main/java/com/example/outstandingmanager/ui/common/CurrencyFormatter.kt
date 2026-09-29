package com.example.outstandingmanager.ui.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.absoluteValue

object CurrencyFormatter {

    /**
     * Formats a numeric amount in Indian Currency Format:
     * e.g., 1765872 -> "₹17,65,872"
     * 542100 -> "₹5,42,100"
     * 9850 -> "₹9,850"
     * 100000 -> "₹1,00,000"
     */
    fun formatIndianCurrency(amount: Double, includeSymbol: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = amount.absoluteValue
        val longVal = absAmount.toLong()
        val decimalPart = ((absAmount - longVal) * 100).toInt()

        val longStr = longVal.toString()
        val formattedInt = if (longStr.length <= 3) {
            longStr
        } else {
            val lastThree = longStr.substring(longStr.length - 3)
            val remaining = longStr.substring(0, longStr.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in remaining.length - 1 downTo 0) {
                sb.append(remaining[i])
                count++
                if (count == 2 && i > 0) {
                    sb.append(',')
                    count = 0
                }
            }
            sb.reverse().toString() + "," + lastThree
        }

        val result = if (decimalPart > 0) {
            "$formattedInt.${String.format(Locale.US, "%02d", decimalPart)}"
        } else {
            formattedInt
        }

        val prefix = if (isNegative) "- " else ""
        return if (includeSymbol) "$prefix₹$result" else "$prefix$result"
    }

    /**
     * Parses any currency string like "₹17,65,872", "1765872.00", "17,65,872", etc.
     */
    fun parseAmount(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val clean = text.replace("₹", "")
            .replace(",", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }
}
