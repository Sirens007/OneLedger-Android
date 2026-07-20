package com.oneledger.app.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddKeyboardStateTest {
    @Test
    fun amountFocusOwnsTheCustomKeyboardEvenWhileImeIsFinishing() {
        val state = QuickAddKeyboardState.from(
            currentFocusField = QuickAddFocusField.AMOUNT,
            imeVisible = true,
        )

        assertTrue(state.customKeyboardVisible)
        assertFalse(state.systemKeyboardVisible)
    }

    @Test
    fun noteFocusOnlyReportsSystemKeyboardAfterImeBecomesVisible() {
        val waiting = QuickAddKeyboardState.from(
            currentFocusField = QuickAddFocusField.NOTE,
            imeVisible = false,
        )
        val visible = QuickAddKeyboardState.from(
            currentFocusField = QuickAddFocusField.NOTE,
            imeVisible = true,
        )

        assertFalse(waiting.customKeyboardVisible)
        assertFalse(waiting.systemKeyboardVisible)
        assertFalse(visible.customKeyboardVisible)
        assertTrue(visible.systemKeyboardVisible)
    }
}
