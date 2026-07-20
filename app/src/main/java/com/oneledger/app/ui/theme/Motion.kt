package com.oneledger.app.ui.theme

import androidx.compose.animation.core.Spring

/** Shared motion tokens keep navigation and touch feedback physically consistent. */
object OneLedgerMotion {
    const val SelectionMillis = 140
    const val NavigationEnterMillis = 180
    const val NavigationExitMillis = 110
    const val ContentEnterMillis = 220
    const val ContentExitMillis = 140
    const val NoBounceDamping = Spring.DampingRatioNoBouncy
    const val NavigationStiffness = Spring.StiffnessMedium
    const val KeyboardStiffness = Spring.StiffnessMediumLow
    const val PressStiffness = Spring.StiffnessHigh
}
