package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val arMonths = listOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    private val arWeekdays = listOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    fun currentYM(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun todayISO(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun yesterdayISO(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun formatYM(ym: String): String {
        if (ym == "all") return "كل الأشهر"
        val parts = ym.split("-")
        if (parts.size != 2) return ym
        val year = parts[0]
        val m = parts[1].toIntOrNull() ?: return ym
        val monthName = arMonths.getOrNull(m - 1) ?: parts[1]
        return "$monthName $year"
    }

    fun formatShortMonth(ym: String): String {
        val parts = ym.split("-")
        if (parts.size != 2) return ym
        val m = parts[1].toIntOrNull() ?: return ym
        return arMonths.getOrNull(m - 1) ?: parts[1]
    }

    fun shiftYM(ym: String, deltaMonths: Int): String {
        val parts = ym.split("-")
        if (parts.size != 2) return currentYM()
        val y = parts[0].toIntOrNull() ?: return currentYM()
        val m = parts[1].toIntOrNull() ?: return currentYM()
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, 1)
        cal.add(Calendar.MONTH, deltaMonths)
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun daysInMonth(ym: String): Int {
        val parts = ym.split("-")
        if (parts.size != 2) return 30
        val y = parts[0].toIntOrNull() ?: 2026
        val m = parts[1].toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun daysBetweenISO(fromISO: String, toISO: String): Int? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val d1 = sdf.parse(fromISO) ?: return null
            val d2 = sdf.parse(toISO) ?: return null
            val diff = d2.time - d1.time
            (diff / 86_400_000L).toInt()
        } catch (_: Exception) {
            null
        }
    }

    fun weekDatesISO(weekOffset: Int): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        cal.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY) + weekOffset * 7)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (0 until 7).map {
            val iso = sdf.format(cal.time)
            val dayName = arWeekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            val mName = arMonths[cal.get(Calendar.MONTH)]
            val label = "$dayName $dayNum $mName"
            cal.add(Calendar.DAY_OF_MONTH, 1)
            iso to label
        }
    }

    fun formatDayHeader(iso: String): String {
        return when (iso) {
            todayISO() -> "اليوم ($iso)"
            yesterdayISO() -> "أمس ($iso)"
            else -> iso
        }
    }
}
