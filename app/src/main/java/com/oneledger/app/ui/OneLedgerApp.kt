package com.oneledger.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.unit.IntOffset
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
import com.oneledger.app.ui.theme.BrandBlueLight
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.OneLedgerMotion
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
    var editingTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingError by rememberSaveable { mutableStateOf<String?>(null) }
    val editingTransaction = state.transactions.firstOrNull { it.id == editingTransactionId }
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
        onQuickAdd = {
            editingTransactionId = null
            editingError = null
            showQuickAdd = true
        },
        onTransactionClick = { transactionId ->
            showQuickAdd = false
            editingError = null
            editingTransactionId = transactionId
        },
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
            onSave = { transaction, keepOpen ->
                viewModel.addTransaction(transaction) { transactionId ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (!keepOpen) showQuickAdd = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (keepOpen) "已保存，可以继续记账" else "已记入日常账本",
                            actionLabel = "撤销",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoTransaction(transactionId)
                    }
                }
            },
        )
    }

    if (editingTransaction != null) {
        QuickAddSheet(
            accounts = state.accounts,
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            initialTransaction = editingTransaction,
            errorMessage = editingError,
            onDismiss = {
                editingError = null
                editingTransactionId = null
            },
            onSave = { transaction, _ ->
                editingError = null
                viewModel.updateTransaction(editingTransaction.id, transaction) { result ->
                    if (result.isSuccess) {
                        editingTransactionId = null
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "账单已更新",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    } else {
                        editingError = "账单未保存，请重试。已填写内容不会丢失。"
                    }
                }
            },
            onDelete = {
                val transactionId = editingTransaction.id
                editingError = null
                viewModel.deleteTransaction(transactionId) { result ->
                    if (result.isSuccess) {
                        editingTransactionId = null
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "账单已删除",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Long,
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                viewModel.restoreTransaction(transactionId) { restoreResult ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = if (restoreResult.isSuccess) "账单已恢复" else "账单未恢复，请重试",
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        editingError = "账单未删除，请重试。原账单仍然安全。"
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
    onTransactionClick: (String) -> Unit = {},
    onBudgetSaved: (MonthWindow, String?, Long) -> Unit = { _, _, _ -> },
    nowMillis: Long = System.currentTimeMillis(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(enabled = destination == Destination.LEDGER && ledgerPage != LedgerPage.OVERVIEW) {
        onLedgerPageChanged(LedgerPage.OVERVIEW)
    }
    val tabStateHolder = rememberSaveableStateHolder()
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
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = OneLedgerMotion.ContentEnterMillis,
                            delayMillis = 28,
                            easing = FastOutSlowInEasing,
                        ),
                    ) togetherWith fadeOut(
                        animationSpec = tween(OneLedgerMotion.ContentExitMillis),
                    )
                },
                contentKey = { it },
                label = "main-destination",
            ) { activeDestination ->
                tabStateHolder.SaveableStateProvider(activeDestination.name) {
                    when (activeDestination) {
                        Destination.LEDGER -> LedgerDestination(
                            state = state,
                            ledgerPage = ledgerPage,
                            onLedgerPageChanged = onLedgerPageChanged,
                            onQuickAdd = onQuickAdd,
                            onTransactionClick = onTransactionClick,
                            onBudgetSaved = onBudgetSaved,
                            nowMillis = nowMillis,
                        )
                        Destination.ASSETS -> AssetsScreen(state, nowMillis = nowMillis)
                        Destination.SAVINGS -> SavingsScreen(state)
                        Destination.STATISTICS -> StatisticsScreen(state, nowMillis = nowMillis)
                    }
                }
            }

            AnimatedVisibility(
                visible = destination == Destination.LEDGER && ledgerPage == LedgerPage.OVERVIEW,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = fadeIn(tween(OneLedgerMotion.ContentEnterMillis)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = OneLedgerMotion.NoBounceDamping,
                            stiffness = OneLedgerMotion.NavigationStiffness,
                        ),
                        initialOffsetY = { it / 3 },
                    ),
                exit = fadeOut(tween(OneLedgerMotion.ContentExitMillis)) +
                    slideOutVertically(
                        animationSpec = spring(
                            dampingRatio = OneLedgerMotion.NoBounceDamping,
                            stiffness = OneLedgerMotion.NavigationStiffness,
                        ),
                        targetOffsetY = { it / 3 },
                    ),
                label = "quick-add-visibility",
            ) {
                LiquidGlassAddButton(
                    onClick = onQuickAdd,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 86.dp),
                )
            }

            AnimatedVisibility(
                visible = destination != Destination.LEDGER || ledgerPage == LedgerPage.OVERVIEW,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(OneLedgerMotion.ContentEnterMillis)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = OneLedgerMotion.NoBounceDamping,
                            stiffness = OneLedgerMotion.NavigationStiffness,
                        ),
                        initialOffsetY = { it / 2 },
                    ),
                exit = fadeOut(tween(OneLedgerMotion.ContentExitMillis)) +
                    slideOutVertically(
                        animationSpec = spring(
                            dampingRatio = OneLedgerMotion.NoBounceDamping,
                            stiffness = OneLedgerMotion.NavigationStiffness,
                        ),
                        targetOffsetY = { it / 2 },
                    ),
                label = "bottom-bar-visibility",
            ) {
                OneLedgerBottomBar(
                    selected = destination,
                    onSelected = onDestinationSelected,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LedgerDestination(
    state: OneLedgerUiState,
    ledgerPage: LedgerPage,
    onLedgerPageChanged: (LedgerPage) -> Unit,
    onQuickAdd: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onBudgetSaved: (MonthWindow, String?, Long) -> Unit,
    nowMillis: Long,
) {
    AnimatedContent(
        targetState = ledgerPage,
        transitionSpec = {
            val isForward = targetState != LedgerPage.OVERVIEW
            val enterOffset: (Int) -> Int = { width -> if (isForward) width / 12 else -width / 12 }
            val exitOffset: (Int) -> Int = { width -> if (isForward) -width / 12 else width / 12 }
            val positionSpec = spring<IntOffset>(
                dampingRatio = OneLedgerMotion.NoBounceDamping,
                stiffness = OneLedgerMotion.NavigationStiffness,
            )
            (fadeIn(tween(OneLedgerMotion.NavigationEnterMillis, easing = FastOutSlowInEasing)) +
                slideInHorizontally(positionSpec, enterOffset)) togetherWith
                (fadeOut(tween(OneLedgerMotion.NavigationExitMillis)) +
                    slideOutHorizontally(positionSpec, exitOffset))
        },
        contentKey = { it },
        label = "ledger-page",
    ) { activePage ->
        when (activePage) {
            LedgerPage.OVERVIEW -> LedgerScreen(
                state = state,
                nowMillis = nowMillis,
                onBudgetClick = { onLedgerPageChanged(LedgerPage.BUDGET) },
                onCalendarClick = { onLedgerPageChanged(LedgerPage.CALENDAR) },
                onTransactionClick = onTransactionClick,
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
                onTransactionClick = onTransactionClick,
                nowMillis = nowMillis,
            )
        }
    }
}

@Composable
private fun LiquidGlassAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(19.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) {
            snap()
        } else {
            spring(
                dampingRatio = OneLedgerMotion.NoBounceDamping,
                stiffness = OneLedgerMotion.PressStiffness,
            )
        },
        label = "quick-add-press",
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .size(58.dp)
            .scale(scale),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
        shadowElevation = 14.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            BrandBlueLight.copy(alpha = 0.88f),
                            BrandBlue.copy(alpha = 0.94f),
                            BrandTeal.copy(alpha = 0.68f),
                        ),
                    ),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        ),
                        RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 7.dp)
                    .size(10.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
            )
            Icon(
                Icons.Default.Add,
                contentDescription = "快速记账",
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun OneLedgerBottomBar(
    selected: Destination,
    onSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = Destination.entries.indexOf(selected)
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
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val itemWidth = maxWidth / Destination.entries.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = OneLedgerMotion.NoBounceDamping,
                    stiffness = OneLedgerMotion.NavigationStiffness,
                ),
                label = "bottom-indicator-position",
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        RoundedCornerShape(20.dp),
                    ),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
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
        animationSpec = if (pressed) {
            snap()
        } else {
            spring(
                dampingRatio = OneLedgerMotion.NoBounceDamping,
                stiffness = OneLedgerMotion.PressStiffness,
            )
        },
        label = "bottom-item-press",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(OneLedgerMotion.SelectionMillis),
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
                .size(30.dp),
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
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(16.dp)
                .height(2.dp)
                .background(content.copy(alpha = if (selected) 1f else 0f), CircleShape),
        )
    }
}
