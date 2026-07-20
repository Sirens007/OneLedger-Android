package com.oneledger.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountCalculatorStateTest {
    @Test
    fun expressionIsOnlyEvaluatedAfterEquals() {
        val twelve = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")

        assertEquals("12", twelve.displayText)
        assertFalse(twelve.isExpressionMode)
        assertEquals("12", twelve.settledAmountOrNull)

        val plus = twelve.inputOperator("+")
        assertEquals("12 +", plus.displayText)
        assertTrue(plus.isExpressionMode)
        assertEquals(AmountOperator.ADD, plus.waitingOperator)
        assertFalse(plus.canEvaluate)
        assertNull(plus.settledAmountOrNull)

        val plusOne = plus.inputDigit("1")
        assertEquals("12 + 1", plusOne.displayText)
        assertNull(plusOne.waitingOperator)
        assertTrue(plusOne.canEvaluate)
        assertNull(plusOne.settledAmountOrNull)

        val minus = plusOne.inputOperator("−")
        assertEquals("12 + 1 −", minus.displayText)
        assertEquals(AmountOperator.SUBTRACT, minus.waitingOperator)
        assertFalse(minus.canEvaluate)

        val completeExpression = minus.inputDigit("2")
        assertEquals("12 + 1 − 2", completeExpression.displayText)
        assertNull(completeExpression.waitingOperator)
        assertTrue(completeExpression.canEvaluate)
        assertNull(completeExpression.settledAmountOrNull)

        val result = completeExpression.evaluate()
        assertEquals("11", result.displayText)
        assertEquals("11", result.settledAmountOrNull)
        assertFalse(result.isExpressionMode)
        assertFalse(result.canEvaluate)
        assertTrue(result.justEvaluated)
    }

    @Test
    fun consecutiveOperatorReplacesTheTrailingOperator() {
        val state = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")
            .inputOperator("−")

        assertEquals("12 −", state.displayText)
        assertEquals(AmountOperator.SUBTRACT, state.waitingOperator)
        assertFalse(state.canEvaluate)
    }

    @Test
    fun asciiMinusIsNormalizedToTheMathematicalMinusGlyph() {
        val state = AmountCalculatorState()
            .inputDigit("9")
            .inputOperator("-")

        assertEquals("9 −", state.displayText)
        assertEquals(AmountOperator.SUBTRACT, state.waitingOperator)
    }

    @Test
    fun nonConsecutiveOperatorAppendsWithoutCalculatingPreviousTokens() {
        val state = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")
            .inputDigit("1")
            .inputOperator("+")

        assertEquals("12 + 1 +", state.displayText)
        assertEquals(AmountOperator.ADD, state.waitingOperator)
        assertFalse(state.canEvaluate)
        assertNull(state.settledAmountOrNull)
    }

    @Test
    fun incompleteExpressionDoesNotEvaluate() {
        val state = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")

        assertSame(state, state.evaluate())
        assertEquals("12 +", state.evaluate().displayText)
    }

    @Test
    fun decimalInputRejectsDuplicatePointAndKeepsExactMoneyPrecision() {
        val state = AmountCalculatorState()
            .inputDigit("0")
            .inputDigit(".")
            .inputDigit("1")
            .inputDigit(".")
            .inputOperator("+")
            .inputDigit(".")
            .inputDigit("2")

        assertEquals("0.1 + 0.2", state.displayText)
        assertTrue(state.canEvaluate)
        assertEquals("0.3", state.evaluate().settledAmountOrNull)
    }

    @Test
    fun trailingDecimalIsNotACompleteNumber() {
        val state = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit(".")

        assertEquals("1.", state.displayText)
        assertNull(state.settledAmountOrNull)
        assertEquals("1.", state.inputOperator("+").displayText)
    }

    @Test
    fun backspaceUnderstandsNumbersAndOperatorTokens() {
        val expression = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")
            .inputDigit("1")
            .inputOperator("−")

        val withoutMinus = expression.backspace()
        assertEquals("12 + 1", withoutMinus.displayText)
        assertTrue(withoutMinus.canEvaluate)

        val withoutOne = withoutMinus.backspace()
        assertEquals("12 +", withoutOne.displayText)
        assertEquals(AmountOperator.ADD, withoutOne.waitingOperator)
        assertFalse(withoutOne.canEvaluate)

        val plainAmount = withoutOne.backspace()
        assertEquals("12", plainAmount.displayText)
        assertFalse(plainAmount.isExpressionMode)
        assertEquals("12", plainAmount.settledAmountOrNull)
    }

    @Test
    fun evaluationCanProduceANegativeAmount() {
        val result = AmountCalculatorState()
            .inputDigit("3")
            .inputOperator("−")
            .inputDigit("5")
            .evaluate()

        assertEquals("-2", result.displayText)
        assertEquals("-2", result.settledAmountOrNull)
        assertTrue(result.justEvaluated)
    }

    @Test
    fun evaluatedResultCanContinueIntoAnotherExpression() {
        val eleven = AmountCalculatorState()
            .inputDigit("1")
            .inputDigit("2")
            .inputOperator("+")
            .inputDigit("1")
            .inputOperator("−")
            .inputDigit("2")
            .evaluate()

        val continued = eleven
            .inputOperator("+")
            .inputDigit("4")

        assertEquals("11 + 4", continued.displayText)
        assertTrue(continued.canEvaluate)
        assertEquals("15", continued.evaluate().settledAmountOrNull)
    }

    @Test
    fun digitAfterEvaluationStartsANewAmount() {
        val evaluated = AmountCalculatorState()
            .inputDigit("1")
            .inputOperator("+")
            .inputDigit("1")
            .evaluate()

        val replaced = evaluated.inputDigit("7")

        assertEquals("7", replaced.displayText)
        assertEquals("7", replaced.settledAmountOrNull)
        assertFalse(replaced.justEvaluated)
    }

    @Test
    fun longExpressionRemainsEditableAndEvaluatesLeftToRight() {
        var state = AmountCalculatorState().inputDigit("1")
        repeat(20) {
            state = state.inputOperator("+").inputDigit("1")
        }

        assertTrue(state.displayText.length < 96)
        assertTrue(state.canEvaluate)
        assertEquals("21", state.evaluate().settledAmountOrNull)
    }

    @Test
    fun expressionLengthIsBoundedWithoutCorruptingTokens() {
        var state = AmountCalculatorState().inputDigit("1")
        repeat(60) {
            state = state.inputOperator("+").inputDigit("1")
        }

        assertTrue(state.displayText.length <= 96)
        assertFalse(state.displayText.contains("+ +"))
        if (state.canEvaluate) {
            assertTrue(state.evaluate().settledAmountOrNull != null)
        } else {
            assertTrue(state.waitingOperator != null)
        }
    }

    @Test
    fun waitingOperatorHighlightClearsAfterOperandStarts() {
        val waiting = AmountCalculatorState()
            .inputDigit("8")
            .inputOperator("+")

        assertEquals(AmountOperator.ADD, waiting.waitingOperator)

        val editingOperand = waiting.inputDigit("2")
        assertNull(editingOperand.waitingOperator)
        assertEquals("8 + 2", editingOperand.displayText)
    }

    @Test
    fun fromAmountRestoresASettledValue() {
        val state = AmountCalculatorState.fromAmount("123.45")

        assertEquals("123.45", state.displayText)
        assertEquals("123.45", state.settledAmountOrNull)
        assertFalse(state.isExpressionMode)
    }

    @Test
    fun evaluatedResultCanCrossFormerNineDigitBoundaryAndRemainEditable() {
        var state = AmountCalculatorState.fromAmount("999999999")
        state = state.inputOperator("+")
        state = state.inputDigit("1")
        state = state.evaluate()

        assertEquals("1000000000", state.displayText)
        assertEquals("1000000000", state.settledAmountOrNull)

        state = state.inputOperator("−")
        assertEquals("1000000000 −", state.displayText)
        assertTrue(state.isExpressionMode)
    }
}
