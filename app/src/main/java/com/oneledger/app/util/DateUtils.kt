package com.oneledger.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthWindow(
    val start: Long,
    val endExclusive: Long,
) {
    companion object {
        fun current(now: Long = System.currentTimeMillis()): MonthWindow {
            return offset(monthOffset = 0, now = now)
        }

        fun offset(
            monthOffset: Int,
            now: Long = System.currentTimeMillis(),
        ): MonthWindow {
            val start = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, monthOffset)
            }
            val end = start.clone() as Calendar
            end.add(Calendar.MONTH, 1)
            return MonthWindow(start.timeInMillis, end.timeInMillis)
        }
    }
}

fun Long.isIn(window: MonthWindow): Boolean = this >= window.start && this < window.endExclusive

fun Long.dayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(this))

fun Long.dayLabel(): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@dayLabel }
    val week = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "周一"
        Calendar.TUESDAY -> "周二"
        Calendar.WEDNESDAY -> "周三"
        Calendar.THURSDAY -> "周四"
        Calendar.FRIDAY -> "周五"
        Calendar.SATURDAY -> "周六"
        else -> "周日"
    }
    return SimpleDateFormat("MM/dd", Locale.CHINA).format(Date(this)) + "  " + week
}

fun Long.timeLabel(): String = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(this))

fun MonthWindow.monthLabel(): String = SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Date(start))

fun Long.shortDate(): String = SimpleDateFormat("yyyy.M.d", Locale.CHINA).format(Date(this))
