package com.oneledger.app.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddKeyboardStateTest {
    @Test
    fun customModeNeverReportsSystemImeEvenWhileImeIsFinishing() {
        val state = QuickAddKeyboardState(
            mode = KeyboardMode.CUSTOM_NUMBER,
            currentFocusField = QuickAddFocusField.AMOUNT,
            imeVisible = true,
        )

        assertTrue(state.customKeyboardVisible)
        assertFalse(state.systemKeyboardVisible)
    }

    @Test
    fun systemModeOnlyReportsVisibleAfterImeActuallyAppears() {
        val waiting = QuickAddKeyboardState(
            mode = KeyboardMode.SYSTEM_IME,
            currentFocusField = QuickAddFocusField.NOTE,
            imeVisible = false,
        )
        val visible = QuickAddKeyboardState(
            mode = KeyboardMode.SYSTEM_IME,
            currentFocusField = QuickAddFocusField.NOTE,
            imeVisible = true,
        )

        assertFalse(waiting.customKeyboardVisible)
        assertFalse(waiting.systemKeyboardVisible)
        assertFalse(visible.customKeyboardVisible)
        assertTrue(visible.systemKeyboardVisible)
    }

    @Test
    fun handoffModeReportsNeitherKeyboard() {
        val state = QuickAddKeyboardState(
            mode = KeyboardMode.NONE,
            currentFocusField = QuickAddFocusField.AMOUNT,
            imeVisible = true,
        )

        assertFalse(state.customKeyboardVisible)
        assertFalse(state.systemKeyboardVisible)
    }
}
