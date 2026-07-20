package com.oneledger.app.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/** Small, deterministic state machine for the quick-add amount keypad. */
internal data class AmountCalculatorState(
    val current: String = "",
    val accumulator: String? = null,
    val pendingOperator: String? = null,
) {
    val displayAmount: String
        get() = current.ifBlank { accumulator.orEmpty() }

    val expressionPrefix: String?
        get() = if (pendingOperator != null && accumulator != null && current.isNotBlank()) {
            "$accumulator $pendingOperator"
        } else {
            null
        }

    val expressionAmount: String?
        get() = accumulator?.takeIf { pendingOperator != null && current.isNotBlank() }

    val expressionOperator: String?
        get() = pendingOperator?.takeIf { accumulator != null && current.isNotBlank() }

    val trailingOperator: String?
        get() = pendingOperator?.takeIf { current.isBlank() }

    fun inputDigit(value: String): AmountCalculatorState {
        val next = when (value) {
            "." -> when {
                current.contains('.') -> current
                current.isBlank() -> "0."
                else -> "$current."
            }

            else -> if (current == "0") value else current + value
        }
        return if (next.matches(AMOUNT_PATTERN)) copy(current = next) else this
    }

    fun inputOperator(operator: String): AmountCalculatorState {
        if (operator !in SUPPORTED_OPERATORS) return this
        if (current.isBlank()) {
            return if (accumulator != null) copy(pendingOperator = operator) else this
        }
        val nextAccumulator = resolvedAmount() ?: return this
        return AmountCalculatorState(
            current = "",
            accumulator = nextAccumulator,
            pendingOperator = operator,
        )
    }

    fun backspace(): AmountCalculatorState = when {
        current.isNotEmpty() -> copy(current = current.dropLast(1))
        pendingOperator != null && accumulator != null -> AmountCalculatorState(current = accumulator)
        accumulator != null -> AmountCalculatorState(current = accumulator.dropLast(1))
        else -> this
    }

    fun resolvedAmount(): String? {
        val right = current.toBigDecimalOrNull()
        if (accumulator == null) return right?.plainAmount()
        if (pendingOperator == null || right == null) return accumulator
        val left = accumulator.toBigDecimalOrNull() ?: return null
        val result = when (pendingOperator) {
            "+" -> left + right
            "−" -> left - right
            else -> return null
        }
        return result.coerceAtLeast(BigDecimal.ZERO).plainAmount()
    }

    private companion object {
        val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")
        val SUPPORTED_OPERATORS = setOf("+", "−")
    }
}

private fun BigDecimal.plainAmount(): String = setScale(2, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()
