package com.oneledger.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun parsesDecimalAmountIntoMinorUnits() {
        assertEquals(1234L, MoneyFormatter.parseToMinor("12.34"))
        assertEquals(1200L, MoneyFormatter.parseToMinor("12"))
        assertEquals(1L, MoneyFormatter.parseToMinor("0.005"))
    }

    @Test
    fun rejectsInvalidAmount() {
        assertNull(MoneyFormatter.parseToMinor(""))
        assertNull(MoneyFormatter.parseToMinor("abc"))
    }
}
