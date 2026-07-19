package com.oneledger.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChineseCalendarLabelTest {
    @Test
    fun `solar festival takes priority`() {
        val label = chineseCalendarLabel(2026, 8, 1)

        assertEquals("建军节", label.text)
        assertTrue(label.isFestival)
    }

    @Test
    fun `lunar festival takes priority over lunar day`() {
        val label = chineseCalendarLabel(2026, 2, 17)

        assertEquals("春节", label.text)
        assertTrue(label.isFestival)
    }

    @Test
    fun `first lunar day uses month label`() {
        val label = chineseCalendarLabel(2026, 7, 14)

        assertEquals("六月", label.text)
        assertFalse(label.isFestival)
    }

    @Test
    fun `ordinary date uses lunar day`() {
        val label = chineseCalendarLabel(2026, 7, 19)

        assertEquals("初六", label.text)
        assertFalse(label.isFestival)
    }
}
