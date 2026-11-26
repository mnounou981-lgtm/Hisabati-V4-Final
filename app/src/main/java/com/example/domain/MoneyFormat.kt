package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object MoneyFormat {
    private val symbols = DecimalFormatSymbols(Locale.FRENCH).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = '.'
    }
    private val wholeFormatter = DecimalFormat("#,##0", symbols)
    private val decimalFormatter = DecimalFormat("#,##0.##", symbols)

    fun toCents(amount: Double): Long = (amount * 100.0).roundToLong()

    fun parseToCents(raw: String): Long? {
        val normalized = normalizeDigits(raw)
            .replace(",", ".")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u2066", "")
            .replace("\u2069", "")
            .trim()
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (!value.isFinite()) return null
        return toCents(value)
    }

    fun fromCents(cents: Long): Double = cents / 100.0

    fun formatDzd(cents: Long, privacyMode: Boolean = false): String {
        if (privacyMode) return "•••• دج"
        val sign = if (cents < 0) "-" else ""
        val absCents = abs(cents)
        val whole = absCents / 100L
        val rem = absCents % 100L
        val numStr = if (rem == 0L) {
            wholeFormatter.format(whole)
        } else {
            decimalFormatter.format(absCents / 100.0)
        }
        return "\u2066$sign$numStr\u2069 دج"
    }

    fun formatNumberOnly(cents: Long): String {
        val absCents = abs(cents)
        val rem = absCents % 100L
        val sign = if (cents < 0) "-" else ""
        return sign + if (rem == 0L) (absCents / 100L).toString() else decimalFormatter.format(absCents / 100.0).replace(" ", "")
    }

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '٠', '۰' -> sb.append('0')
                '١', '۱' -> sb.append('1')
                '٢', '۲' -> sb.append('2')
                '٣', '۳' -> sb.append('3')
                '٤', '۴' -> sb.append('4')
                '٥', '۵' -> sb.append('5')
                '٦', '۶' -> sb.append('6')
                '٧', '۷' -> sb.append('7')
                '٨', '۸' -> sb.append('8')
                '٩', '۹' -> sb.append('9')
                '٫' -> sb.append('.')
                '٬' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
