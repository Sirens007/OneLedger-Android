package com.oneledger.app.util

import java.util.Calendar

data class CalendarDateCell(
    val startMillis: Long,
    val dayNumber: Int,
    val inCurrentMonth: Boolean,
)

fun MonthWindow.calendarDateCells(): List<CalendarDateCell> {
    val monthStart = Calendar.getInstance().apply { timeInMillis = start }
    val gridStart = (monthStart.clone() as Calendar).apply {
        val daysFromSunday = (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -daysFromSunday)
    }
    return List(42) { index ->
        val date = (gridStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
        CalendarDateCell(
            startMillis = date.timeInMillis,
            dayNumber = date.get(Calendar.DAY_OF_MONTH),
            inCurrentMonth = date.timeInMillis >= start && date.timeInMillis < endExclusive,
        )
    }
}

fun Long.startOfLocalDay(): Long = Calendar.getInstance().apply {
    timeInMillis = this@startOfLocalDay
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun Long.nextLocalDayStart(): Long = Calendar.getInstance().apply {
    timeInMillis = this@nextLocalDayStart
    add(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis
