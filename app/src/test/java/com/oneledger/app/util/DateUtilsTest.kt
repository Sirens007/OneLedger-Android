package com.oneledger.app.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun offsetBuildsPreviousCalendarMonth() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 18, 12, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val window = MonthWindow.offset(monthOffset = -1, now = now)
        val start = Calendar.getInstance().apply { timeInMillis = window.start }
        val end = Calendar.getInstance().apply { timeInMillis = window.endExclusive }

        assertEquals(2026, start.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, start.get(Calendar.MONTH))
        assertEquals(1, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.JULY, end.get(Calendar.MONTH))
        assertEquals(1, end.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun offsetBuildsFutureCalendarMonth() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 18, 12, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val window = MonthWindow.offset(monthOffset = 18, now = now)
        val start = Calendar.getInstance().apply { timeInMillis = window.start }
        val end = Calendar.getInstance().apply { timeInMillis = window.endExclusive }

        assertEquals(2028, start.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, start.get(Calendar.MONTH))
        assertEquals(1, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FEBRUARY, end.get(Calendar.MONTH))
        assertEquals(1, end.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun calendarGridStartsOnSundayAndAlwaysContainsSixWeeks() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 18, 12, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cells = MonthWindow.current(now).calendarDateCells()
        val first = Calendar.getInstance().apply { timeInMillis = cells.first().startMillis }

        assertEquals(42, cells.size)
        assertEquals(Calendar.SUNDAY, first.get(Calendar.DAY_OF_WEEK))
        assertEquals(1, cells.first { it.inCurrentMonth }.dayNumber)
        assertEquals(31, cells.last { it.inCurrentMonth }.dayNumber)
    }
}
