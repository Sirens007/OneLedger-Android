package com.oneledger.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oneledger.app.ui.components.QuickAddSheet
import com.oneledger.app.ui.screens.AssetsScreen
import com.oneledger.app.ui.screens.BudgetDetailScreen
import com.oneledger.app.ui.screens.IncomeExpenseCalendarScreen
import com.oneledger.app.ui.screens.LedgerScreen
import com.oneledger.app.ui.screens.SavingsScreen
import com.oneledger.app.ui.screens.StatisticsScreen
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.util.MonthWindow
import kotlinx.coroutines.launch

internal enum class Destination(
    val label: String,
    val icon: ImageVector,
) {
    LEDGER("账本", Icons.AutoMirrored.Filled.ReceiptLong),
    ASSETS("资产", Icons.Default.AccountBalanceWallet),
    SAVINGS("存钱", Icons.Default.Savings),
    STATISTICS("统计", Icons.Default.BarChart),
}

internal enum class LedgerPage {
    OVERVIEW,
    BUDGET,
    CALENDAR,
}

@Composable
fun OneLedgerApp(viewModel: OneLedgerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.LEDGER) }
    var ledgerPage by rememberSaveable { mutableStateOf(LedgerPage.OVERVIEW) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    OneLedgerFrame(
        state = state,
        destination = destination,
        ledgerPage = ledgerPage,
        onDestinationSelected = {
            destination = it
            ledgerPage = LedgerPage.OVERVIEW
        },
        onLedgerPageChanged = { ledgerPage = it },
        onQuickAdd = { showQuickAdd = true },
        onBudgetSaved = { window, categoryId, limitMinor ->
            viewModel.saveBudget(
                periodStart = window.start,
                periodEnd = window.endExclusive,
                limitMinor = limitMinor,
                categoryId = categoryId,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (categoryId == null) "总预算已更新" else "分类预算已更新",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState,
    )

    if (showQuickAdd) {
        QuickAddSheet(
            accounts = state.accounts,
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            onDismiss = { showQuickAdd = false },
            onSave = { transaction ->
                viewModel.addTransaction(transaction) { transactionId ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    showQuickAdd = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "已记入日常账本",
                            actionLabel = "撤销",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoTransaction(transactionId)
                    }
                }
            },
        )
    }
}

@Composable
internal fun OneLedgerFrame(
    state: OneLedgerUiState,
    destination: Destination,
    onDestinationSelected: (Destination) -> Unit,
    onQuickAdd: () -> Unit,
    ledgerPage: LedgerPage = LedgerPage.OVERVIEW,
    onLedgerPageChanged: (LedgerPage) -> Unit = {},
    onBudgetSaved: (MonthWindow, String?, Long) -> Unit = { _, _, _ -> },
    nowMillis: Long = System.currentTimeMillis(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(enabled = destination == Destination.LEDGER && ledgerPage != LedgerPage.OVERVIEW) {
        onLedgerPageChanged(LedgerPage.OVERVIEW)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            key(destination) {
                when (destination) {
                    Destination.LEDGER -> when (ledgerPage) {
                        LedgerPage.OVERVIEW -> LedgerScreen(
                            state = state,
                            nowMillis = nowMillis,
                            onBudgetClick = { onLedgerPageChanged(LedgerPage.BUDGET) },
                            onCalendarClick = { onLedgerPageChanged(LedgerPage.CALENDAR) },
                        )
                        LedgerPage.BUDGET -> BudgetDetailScreen(
                            state = state,
                            onBack = { onLedgerPageChanged(LedgerPage.OVERVIEW) },
                            onSaveBudget = onBudgetSaved,
                            nowMillis = nowMillis,
                        )
                        LedgerPage.CALENDAR -> IncomeExpenseCalendarScreen(
                            state = state,
                            onBack = { onLedgerPageChanged(LedgerPage.OVERVIEW) },
                            onQuickAdd = onQuickAdd,
                            nowMillis = nowMillis,
                        )
                    }
                    Destination.ASSETS -> AssetsScreen(state, nowMillis = nowMillis)
                    Destination.SAVINGS -> SavingsScreen(state)
                    Destination.STATISTICS -> StatisticsScreen(state, nowMillis = nowMillis)
                }
            }

            if (destination == Destination.LEDGER && ledgerPage == LedgerPage.OVERVIEW) {
                QuickAddButton(
                    onClick = onQuickAdd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 86.dp),
                )
            }

            if (destination != Destination.LEDGER || ledgerPage == LedgerPage.OVERVIEW) {
                OneLedgerBottomBar(
                    selected = destination,
                    onSelected = onDestinationSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) snap() else tween(160),
        label = "quick-add-press",
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .width(112.dp)
            .height(50.dp)
            .scale(scale)
            .background(
                brush = Brush.horizontalGradient(listOf(BrandBlue, BrandTeal)),
                shape = shape,
            ),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "快速记账",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("记一笔", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OneLedgerBottomBar(
    selected: Destination,
    onSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp),
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelected(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = if (pressed) snap() else tween(160),
        label = "bottom-item-press",
    )
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(180),
        label = "bottom-item-background",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(140),
        label = "bottom-item-content",
    )
    Column(
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(destination.icon, contentDescription = destination.label, tint = content, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            destination.label,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(16.dp)
                .height(2.dp)
                .background(if (selected) BrandTeal else Color.Transparent, CircleShape),
        )
    }
}
