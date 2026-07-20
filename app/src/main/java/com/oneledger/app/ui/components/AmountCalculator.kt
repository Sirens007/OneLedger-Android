package com.oneledger.app.ui.components

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import java.math.BigDecimal
import java.math.RoundingMode

internal enum class AmountOperator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−");

    companion object {
        fun fromSymbol(symbol: String): AmountOperator? = when (symbol) {
            "+" -> ADD
            "-", "−" -> SUBTRACT
            else -> null
        }
    }
}

internal sealed interface ExpressionToken {
    data class NumberToken(val raw: String) : ExpressionToken
    data class OperatorToken(val operator: AmountOperator) : ExpressionToken
}

/**
 * Editable add/subtract expression. Operators only edit tokens; evaluation happens exclusively via [evaluate].
 */
internal data class AmountCalculatorState(
    val tokens: List<ExpressionToken> = emptyList(),
    val justEvaluated: Boolean = false,
) {
    val displayText: String
        get() = tokens.joinToString(separator = " ") { token ->
            when (token) {
                is ExpressionToken.NumberToken -> token.raw
                is ExpressionToken.OperatorToken -> token.operator.symbol
            }
        }

    val isExpressionMode: Boolean
        get() = tokens.any { it is ExpressionToken.OperatorToken }

    val waitingOperator: AmountOperator?
        get() = (tokens.lastOrNull() as? ExpressionToken.OperatorToken)?.operator

    val canEvaluate: Boolean
        get() = isExpressionMode && hasValidAlternatingTokens()

    /** Only a settled single number may be persisted. Pending expressions are never calculated implicitly. */
    val settledAmountOrNull: String?
        get() = if (!isExpressionMode) {
            (tokens.singleOrNull() as? ExpressionToken.NumberToken)
                ?.raw
                ?.takeIf(::isCompleteNumber)
        } else {
            null
        }

    fun inputDigit(value: String): AmountCalculatorState {
        if (value == ".") return inputDecimal()
        if (value.length != 1 || !value[0].isDigit()) return this

        if (justEvaluated || tokens.isEmpty()) {
            return AmountCalculatorState(tokens = listOf(ExpressionToken.NumberToken(value)))
        }

        return when (val last = tokens.last()) {
            is ExpressionToken.OperatorToken -> withTokens(tokens + ExpressionToken.NumberToken(value))
            is ExpressionToken.NumberToken -> {
                val nextRaw = when (last.raw) {
                    "0" -> value
                    "-0" -> "-$value"
                    else -> last.raw + value
                }
                if (!nextRaw.matches(EDITABLE_NUMBER_PATTERN)) this
                else withTokens(tokens.dropLast(1) + ExpressionToken.NumberToken(nextRaw))
            }
        }
    }

    private fun inputDecimal(): AmountCalculatorState {
        if (justEvaluated || tokens.isEmpty()) {
            return AmountCalculatorState(tokens = listOf(ExpressionToken.NumberToken("0.")))
        }

        return when (val last = tokens.last()) {
            is ExpressionToken.OperatorToken -> withTokens(tokens + ExpressionToken.NumberToken("0."))
            is ExpressionToken.NumberToken -> {
                if (last.raw.contains('.')) return this
                val nextRaw = last.raw + "."
                if (!nextRaw.matches(EDITABLE_NUMBER_PATTERN)) this
                else withTokens(tokens.dropLast(1) + ExpressionToken.NumberToken(nextRaw))
            }
        }
    }

    fun inputOperator(symbol: String): AmountCalculatorState {
        val operator = AmountOperator.fromSymbol(symbol) ?: return this
        val last = tokens.lastOrNull() ?: return this
        return when (last) {
            is ExpressionToken.OperatorToken -> copy(
                tokens = tokens.dropLast(1) + ExpressionToken.OperatorToken(operator),
                justEvaluated = false,
            )

            is ExpressionToken.NumberToken -> {
                if (!isCompleteNumber(last.raw)) this
                else withTokens(tokens + ExpressionToken.OperatorToken(operator))
            }
        }
    }

    fun backspace(): AmountCalculatorState {
        val last = tokens.lastOrNull() ?: return this
        return when (last) {
            is ExpressionToken.OperatorToken -> copy(
                tokens = tokens.dropLast(1),
                justEvaluated = false,
            )

            is ExpressionToken.NumberToken -> {
                val shortened = last.raw.dropLast(1)
                val nextTokens = if (shortened.isEmpty() || shortened == "-") {
                    tokens.dropLast(1)
                } else {
                    tokens.dropLast(1) + ExpressionToken.NumberToken(shortened)
                }
                copy(tokens = nextTokens, justEvaluated = false)
            }
        }
    }

    fun evaluate(): AmountCalculatorState {
        if (!canEvaluate) return this
        var result = (tokens.first() as ExpressionToken.NumberToken).raw.toBigDecimal()
        var index = 1
        while (index < tokens.size) {
            val operator = (tokens[index] as ExpressionToken.OperatorToken).operator
            val operand = (tokens[index + 1] as ExpressionToken.NumberToken).raw.toBigDecimal()
            result = when (operator) {
                AmountOperator.ADD -> result + operand
                AmountOperator.SUBTRACT -> result - operand
            }
            index += 2
        }
        return AmountCalculatorState(
            tokens = listOf(ExpressionToken.NumberToken(result.plainAmount())),
            justEvaluated = true,
        )
    }

    private fun hasValidAlternatingTokens(): Boolean {
        if (tokens.size < 3 || tokens.size % 2 == 0) return false
        return tokens.withIndex().all { (index, token) ->
            if (index % 2 == 0) {
                token is ExpressionToken.NumberToken && isCompleteNumber(token.raw)
            } else {
                token is ExpressionToken.OperatorToken
            }
        }
    }

    private fun withTokens(nextTokens: List<ExpressionToken>): AmountCalculatorState {
        val next = copy(tokens = nextTokens, justEvaluated = false)
        return if (next.displayText.length <= MAX_EXPRESSION_LENGTH) next else this
    }

    companion object {
        private const val MAX_EXPRESSION_LENGTH = 96
        // The expression length is bounded globally. Do not impose a smaller digit cap here: a valid
        // calculation may naturally cross the previous 9-digit boundary. Persistence range is checked
        // separately by MoneyFormatter.parseToMinor before the transaction can be saved.
        private val EDITABLE_NUMBER_PATTERN = Regex("^-?\\d*(\\.\\d{0,2})?$")
        private val COMPLETE_NUMBER_PATTERN = Regex("^-?\\d+(\\.\\d{1,2})?$")

        fun fromAmount(amount: String): AmountCalculatorState = AmountCalculatorState(
            tokens = amount.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(ExpressionToken.NumberToken(it)) }
                .orEmpty(),
        )

        val Saver: Saver<AmountCalculatorState, Any> = listSaver(
            save = { state ->
                buildList {
                    add(if (state.justEvaluated) "E:1" else "E:0")
                    state.tokens.forEach { token ->
                        add(
                            when (token) {
                                is ExpressionToken.NumberToken -> "N:${token.raw}"
                                is ExpressionToken.OperatorToken -> "O:${token.operator.name}"
                            },
                        )
                    }
                }
            },
            restore = { saved ->
                val justEvaluated = saved.firstOrNull() == "E:1"
                val restoredTokens = saved.drop(1).mapNotNull { encoded ->
                    when {
                        encoded.startsWith("N:") -> ExpressionToken.NumberToken(encoded.removePrefix("N:"))
                        encoded.startsWith("O:") -> runCatching {
                            ExpressionToken.OperatorToken(
                                AmountOperator.valueOf(encoded.removePrefix("O:")),
                            )
                        }.getOrNull()
                        else -> null
                    }
                }
                AmountCalculatorState(tokens = restoredTokens, justEvaluated = justEvaluated)
            },
        )

        private fun isCompleteNumber(raw: String): Boolean = raw.matches(COMPLETE_NUMBER_PATTERN)
    }
}

private fun BigDecimal.plainAmount(): String = setScale(2, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()
