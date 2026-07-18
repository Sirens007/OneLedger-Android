package com.oneledger.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object MoneyFormatter {
    private val formatter = DecimalFormat("#,##0.00")

    fun format(minor: Long, showSign: Boolean = false): String {
        val absolute = kotlin.math.abs(minor)
        val amount = formatter.format(absolute / 100.0)
        val sign = when {
            minor < 0 -> "-"
            showSign && minor > 0 -> "+"
            else -> ""
        }
        return "$sign¥$amount"
    }

    fun parseToMinor(input: String): Long? = runCatching {
        input.trim()
            .takeIf { it.isNotEmpty() }
            ?.let(::BigDecimal)
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.movePointRight(2)
            ?.longValueExact()
    }.getOrNull()
}
