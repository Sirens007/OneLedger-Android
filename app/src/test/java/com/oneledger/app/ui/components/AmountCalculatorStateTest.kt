package com.oneledger.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountCalculatorStateTest {
    @Test
    fun additionResolvesWhenSaving() {
        val state = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")
            .inputDigit("3")

        assertEquals("15", state.resolvedAmount())
        assertEquals("12 +", state.expressionPrefix)
        assertEquals("12", state.expressionAmount)
        assertEquals("+", state.expressionOperator)
        assertEquals("3", state.displayAmount)
    }

    @Test
    fun subtractionDoesNotCreateANegativeTransactionAmount() {
        val state = AmountCalculatorState()
            .inputDigit("3")
            .inputOperator("−")
            .inputDigit("5")

        assertEquals("0", state.resolvedAmount())
    }

    @Test
    fun secondOperatorReplacesPendingOperator() {
        val state = AmountCalculatorState()
            .inputDigit("8")
            .inputOperator("+")
            .inputOperator("−")

        assertEquals("−", state.pendingOperator)
        assertEquals("8", state.accumulator)
    }

    @Test
    fun backspaceEditsTheCurrentOperand() {
        val state = AmountCalculatorState()
            .inputDigit("8")
            .inputOperator("+")
            .inputDigit("2")
            .inputDigit("5")
            .backspace()

        assertEquals("2", state.current)
        assertEquals("10", state.resolvedAmount())
    }

    @Test
    fun backspaceAfterOperatorRestoresEditableAmount() {
        val state = AmountCalculatorState()
            .inputDigit("8")
            .inputOperator("+")
            .backspace()

        assertEquals(AmountCalculatorState(current = "8"), state)
    }

    @Test
    fun chainedOperationUsesPreviousResult() {
        val state = AmountCalculatorState()
            .inputDigit("8")
            .inputOperator("+")
            .inputDigit("2")
            .inputOperator("−")
            .inputDigit("3")

        assertEquals("7", state.resolvedAmount())
        assertEquals("10 −", state.expressionPrefix)
    }
}
