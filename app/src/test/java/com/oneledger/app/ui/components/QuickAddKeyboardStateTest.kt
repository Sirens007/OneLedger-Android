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
        assertTrue(state.customKeyboardEnabled)
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
    fun customToSystemHandoffKeepsOutgoingSurfaceComposedAndEnablesNote() {
        val stateBeforeIme = QuickAddKeyboardState(
            mode = KeyboardMode.CUSTOM_TO_SYSTEM,
            currentFocusField = QuickAddFocusField.NOTE,
            imeVisible = false,
        )
        val stateDuringIme = stateBeforeIme.copy(imeVisible = true)

        assertTrue(stateBeforeIme.customKeyboardVisible)
        assertTrue(stateDuringIme.customKeyboardVisible)
        assertFalse(stateBeforeIme.customKeyboardEnabled)
        assertTrue(stateBeforeIme.noteInputEnabled)
    }

    @Test
    fun systemToCustomHandoffExposesOnlyTheCustomKeyboardForInput() {
        val state = QuickAddKeyboardState(
            mode = KeyboardMode.SYSTEM_TO_CUSTOM,
            currentFocusField = QuickAddFocusField.AMOUNT,
            imeVisible = true,
        )

        assertTrue(state.customKeyboardVisible)
        assertTrue(state.customKeyboardEnabled)
        assertFalse(state.systemKeyboardVisible)
        assertFalse(state.noteInputEnabled)
    }
}
