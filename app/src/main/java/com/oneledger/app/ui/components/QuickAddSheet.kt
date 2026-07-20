package com.oneledger.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.BrandBlueLight
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.OneLedgerMotion
import com.oneledger.app.util.CalendarDateCell
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.calendarDateCells
import com.oneledger.app.util.monthLabel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class QuickAddInputMode {
    AMOUNT,
    NOTE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (NewTransaction, Boolean) -> Unit,
    initialTransaction: TransactionListItem? = null,
    onDelete: (() -> Unit)? = null,
    errorMessage: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = null,
    ) {
        QuickAddContent(
            accounts = accounts,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onDismiss = onDismiss,
            onSave = onSave,
            initialTransaction = initialTransaction,
            onDelete = onDelete,
            errorMessage = errorMessage,
            nowMillis = nowMillis,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.97f),
        )
    }
}

@Composable
private fun QuickAddContent(
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (NewTransaction, Boolean) -> Unit,
    initialTransaction: TransactionListItem?,
    onDelete: (() -> Unit)?,
    errorMessage: String?,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val editKey = initialTransaction?.id
    val isEditing = initialTransaction != null
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var type by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var amount by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.amountMinor?.amountInputValue().orEmpty()) }
    var accumulator by rememberSaveable(editKey) { mutableStateOf<String?>(null) }
    var pendingOperator by rememberSaveable(editKey) { mutableStateOf<String?>(null) }
    var note by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.note.orEmpty()) }
    var inputMode by remember(editKey) { mutableStateOf(QuickAddInputMode.AMOUNT) }
    var selectedCategoryId by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.categoryId) }
    var selectedAccountId by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.accountId) }
    var selectedToAccountId by rememberSaveable(editKey) { mutableStateOf(initialTransaction?.toAccountId) }
    var occurredAt by rememberSaveable(editKey) { mutableLongStateOf(initialTransaction?.occurredAt ?: nowMillis) }
    var showDateTimePicker by rememberSaveable(editKey) { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable(editKey) { mutableStateOf(false) }
    val categories = when (type) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        else -> emptyList()
    }
    val accent = when (type) {
        TransactionType.EXPENSE -> ExpenseCoral
        TransactionType.INCOME -> IncomeMint
        else -> BrandBlue
    }
    val calculatorState = AmountCalculatorState(
        current = amount,
        accumulator = accumulator,
        pendingOperator = pendingOperator,
    )
    val resolvedAmount = calculatorState.resolvedAmount()
    val parsedAmount = MoneyFormatter.parseToMinor(resolvedAmount.orEmpty())
    val canSave = parsedAmount != null && parsedAmount > 0 && selectedAccountId != null && when (type) {
        TransactionType.TRANSFER -> selectedToAccountId != null && selectedToAccountId != selectedAccountId
        else -> selectedCategoryId != null
    }

    LaunchedEffect(type, categories) {
        if (type != TransactionType.TRANSFER && categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == selectedAccountId }) selectedAccountId = accounts.firstOrNull()?.id
        if (accounts.none { it.id == selectedToAccountId } || selectedToAccountId == selectedAccountId) {
            selectedToAccountId = accounts.firstOrNull { it.id != selectedAccountId }?.id
        }
    }

    fun inputDigit(value: String) {
        val next = AmountCalculatorState(amount, accumulator, pendingOperator).inputDigit(value)
        amount = next.current
        accumulator = next.accumulator
        pendingOperator = next.pendingOperator
    }

    fun inputOperator(operator: String) {
        val next = AmountCalculatorState(amount, accumulator, pendingOperator).inputOperator(operator)
        amount = next.current
        accumulator = next.accumulator
        pendingOperator = next.pendingOperator
    }

    fun backspace() {
        val next = AmountCalculatorState(amount, accumulator, pendingOperator).backspace()
        amount = next.current
        accumulator = next.accumulator
        pendingOperator = next.pendingOperator
    }

    fun save(keepOpen: Boolean) {
        if (!canSave) return
        onSave(
            NewTransaction(
                type = type,
                amountMinor = requireNotNull(parsedAmount),
                categoryId = if (type == TransactionType.TRANSFER) null else selectedCategoryId,
                accountId = requireNotNull(selectedAccountId),
                toAccountId = if (type == TransactionType.TRANSFER) selectedToAccountId else null,
                note = note,
                occurredAt = occurredAt,
            ),
            keepOpen,
        )
        if (keepOpen) {
            amount = ""
            accumulator = null
            pendingOperator = null
            note = ""
            occurredAt = nowMillis
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        QuickAddHeader(type = type, isEditing = isEditing, onTypeSelected = { type = it }, onDismiss = onDismiss)
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = type,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                fadeIn(tween(OneLedgerMotion.ContentEnterMillis)) togetherWith
                    fadeOut(tween(OneLedgerMotion.ContentExitMillis))
            },
            contentKey = { it },
            label = "quick-add-type-content",
        ) { activeType ->
            if (activeType == TransactionType.TRANSFER) {
                TransferAccountSelector(
                    accounts = accounts,
                    fromAccountId = selectedAccountId,
                    toAccountId = selectedToAccountId,
                    onFromSelected = { selectedAccountId = it },
                    onToSelected = { selectedToAccountId = it },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val activeCategories = if (activeType == TransactionType.EXPENSE) expenseCategories else incomeCategories
                CategoryGrid(
                    categories = activeCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelected = { selectedCategoryId = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        QuickOptionRow(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = { selectedAccountId = it },
            onDateClick = { showDateTimePicker = true },
        )
        Spacer(Modifier.height(8.dp))
        AmountPanel(
            amount = calculatorState.displayAmount,
            expressionPrefix = calculatorState.expressionPrefix,
            trailingOperator = calculatorState.trailingOperator,
            accent = accent,
            note = note,
            occurredAt = occurredAt,
            onNoteChange = { if (it.length <= 60) note = it },
            onNoteFocusChanged = { focused ->
                if (focused) inputMode = QuickAddInputMode.NOTE
            },
            onAmountClick = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                inputMode = QuickAddInputMode.AMOUNT
            },
            onNoteDone = {
                inputMode = QuickAddInputMode.AMOUNT
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            },
            onDateClick = { showDateTimePicker = true },
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, start = 4.dp, end = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = ExpenseCoral,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        AnimatedVisibility(
            visible = inputMode == QuickAddInputMode.AMOUNT,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = OneLedgerMotion.NoBounceDamping,
                    stiffness = OneLedgerMotion.NavigationStiffness,
                ),
            ) + fadeIn(tween(OneLedgerMotion.InputSwapFadeMillis)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(OneLedgerMotion.InputSwapExitMillis),
            ) + fadeOut(tween(OneLedgerMotion.InputSwapFadeMillis)),
            label = "calculator-visibility",
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                CalculatorPad(
                    accent = accent,
                    pendingOperator = pendingOperator,
                    canSave = canSave,
                    isEditing = isEditing,
                    onDigit = ::inputDigit,
                    onOperator = ::inputOperator,
                    onBackspace = ::backspace,
                    onSaveAgain = { save(true) },
                    onDone = { save(false) },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        }
    }

    if (showDateTimePicker) {
        TransactionDateTimeDialog(
            initialMillis = occurredAt,
            onDismiss = { showDateTimePicker = false },
            onConfirm = {
                occurredAt = it
                showDateTimePicker = false
            },
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这笔账单？") },
            text = { Text("账单会从余额、预算和统计中移除，删除后可在提示中撤销。") },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text("删除账单", color = ExpenseCoral, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun QuickAddHeader(
    type: String,
    isEditing: Boolean,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressableSurface(
            onClick = onDismiss,
            modifier = Modifier.size(44.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(15.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (isEditing) "关闭账单编辑" else "关闭记账",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(3.dp),
        ) {
            TransactionTypeTab("支出", TransactionType.EXPENSE, type, ExpenseCoral, onTypeSelected)
            TransactionTypeTab("收入", TransactionType.INCOME, type, IncomeMint, onTypeSelected)
            TransactionTypeTab("转账", TransactionType.TRANSFER, type, BrandBlue, onTypeSelected)
        }
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                if (isEditing) "编辑" else "账本",
                color = BrandBlueLight,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TransactionTypeTab(
    label: String,
    value: String,
    selected: String,
    accent: Color,
    onSelected: (String) -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (value == selected) accent.copy(alpha = 0.20f) else Color.Transparent,
        animationSpec = tween(140),
        label = "quick-type-color",
    )
    PressableSurface(
        onClick = { onSelected(value) },
        color = background,
        shape = RoundedCornerShape(15.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = if (value == selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val color = colorFromLong(category.colorHex)
            PressableSurface(
                onClick = { onSelected(category.id) },
                color = Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (category.id == selectedCategoryId) color else MaterialTheme.colorScheme.surface,
                                CircleShape,
                            )
                            .border(
                                1.dp,
                                if (category.id == selectedCategoryId) Color.White.copy(alpha = 0.28f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            categoryIcon(category.iconKey),
                            contentDescription = category.name,
                            tint = if (category.id == selectedCategoryId) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                    Text(
                        category.name,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (category.id == selectedCategoryId) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferAccountSelector(
    accounts: List<AccountEntity>,
    fromAccountId: String?,
    toAccountId: String?,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("转出账户", style = MaterialTheme.typography.titleMedium)
        AccountGrid(accounts, fromAccountId, onFromSelected)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
        }
        Text("转入账户", style = MaterialTheme.typography.titleMedium)
        AccountGrid(accounts.filter { it.id != fromAccountId }, toAccountId, onToSelected)
    }
}

@Composable
private fun AccountGrid(accounts: List<AccountEntity>, selected: String?, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.take(4).forEach { account ->
            PressableSurface(
                onClick = { onSelected(account.id) },
                color = if (account.id == selected) BrandBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AccountGlyph(account.type, colorFromLong(account.colorHex), modifier = Modifier.size(38.dp))
                    Text(account.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun QuickOptionRow(
    accounts: List<AccountEntity>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onDateClick: () -> Unit,
) {
    val selectedIndex = accounts.indexOfFirst { it.id == selectedAccountId }.coerceAtLeast(0)
    val account = accounts.getOrNull(selectedIndex)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            QuickOptionChip(
                icon = Icons.Default.AccountBalanceWallet,
                label = account?.name ?: "选择账户",
                accent = BrandBlue,
                onClick = {
                    if (accounts.isNotEmpty()) onAccountSelected(accounts[(selectedIndex + 1) % accounts.size].id)
                },
            )
        }
        item {
            QuickOptionChip(Icons.Default.CalendarMonth, "修改时间", BrandBlueLight, onClick = onDateClick)
        }
    }
}

@Composable
private fun QuickOptionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    PressableSurface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AmountPanel(
    amount: String,
    expressionPrefix: String?,
    trailingOperator: String?,
    accent: Color,
    note: String,
    occurredAt: Long,
    onNoteChange: (String) -> Unit,
    onNoteFocusChanged: (Boolean) -> Unit,
    onAmountClick: () -> Unit,
    onNoteDone: () -> Unit,
    onDateClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(22.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("¥", style = MaterialTheme.typography.headlineMedium, color = accent)
                AmountValueButton(
                    amount = amount,
                    expressionPrefix = expressionPrefix,
                    trailingOperator = trailingOperator,
                    accent = accent,
                    onClick = onAmountClick,
                )
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)))
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PressableSurface(
                    onClick = onDateClick,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text(
                            SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(occurredAt)),
                            modifier = Modifier.padding(start = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                BasicTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                        .onFocusChanged { onNoteFocusChanged(it.isFocused) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onNoteDone() },
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (note.isBlank()) {
                                Text(
                                    "点击填写备注",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AmountValueButton(
    amount: String,
    expressionPrefix: String?,
    trailingOperator: String?,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) snap() else spring(
            dampingRatio = OneLedgerMotion.NoBounceDamping,
            stiffness = OneLedgerMotion.PressStiffness,
        ),
        label = "amount-press-feedback",
    )
    Row(
        modifier = Modifier
            .padding(start = 2.dp)
            .graphicsLayer {
                scaleX = 1f - (pressProgress * 0.018f)
                scaleY = 1f - (pressProgress * 0.018f)
                alpha = 1f - (pressProgress * 0.14f)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 3.dp, end = 8.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        expressionPrefix?.let {
            Text(
                text = "$it  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
        Text(
            text = amount.ifBlank { "0.00" },
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
            color = accent,
            maxLines = 1,
        )
        trailingOperator?.let {
            Text(
                text = "  $it",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun CalculatorPad(
    accent: Color,
    pendingOperator: String?,
    canSave: Boolean,
    isEditing: Boolean,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onBackspace: () -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    val secondaryAction = if (isEditing) "删除" else "再记"
    val primaryAction = if (isEditing) "保存" else "完成"
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "−"),
        listOf("7", "8", "9", secondaryAction),
        listOf(".", "0", "⌫", primaryAction),
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { key ->
                    val primary = key == primaryAction
                    val secondary = key == secondaryAction
                    val operator = key == "+" || key == "−"
                    val selectedOperator = operator && key == pendingOperator
                    PressableSurface(
                        onClick = {
                            when (key) {
                                "+", "−" -> onOperator(key)
                                "⌫" -> onBackspace()
                                "再记" -> onSaveAgain()
                                "删除" -> onDelete()
                                primaryAction -> onDone()
                                else -> onDigit(key)
                            }
                        },
                        enabled = when (key) {
                            primaryAction, "再记" -> canSave
                            else -> true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        color = when {
                            primary -> accent
                            key == "删除" -> ExpenseCoral.copy(alpha = 0.16f)
                            secondary -> accent.copy(alpha = 0.16f)
                            selectedOperator -> accent.copy(alpha = 0.18f)
                            operator -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when (key) {
                                "+" -> Icon(
                                    Icons.Default.Add,
                                    contentDescription = "加",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selectedOperator) accent else MaterialTheme.colorScheme.onSurface,
                                )
                                "−" -> Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "减",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selectedOperator) accent else MaterialTheme.colorScheme.onSurface,
                                )
                                "⌫" -> Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "退格",
                                    modifier = Modifier.size(24.dp),
                                )
                                primaryAction -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(primaryAction, modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold)
                                }
                                else -> Text(
                                    key,
                                    style = if (secondary) {
                                        MaterialTheme.typography.labelLarge
                                    } else {
                                        MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp)
                                    },
                                    color = if (key == "删除") ExpenseCoral else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (secondary) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDateTimeDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.46f))
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            TransactionDateTimeContent(
                initialMillis = initialMillis,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TransactionDateTimeContent(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftMillis by rememberSaveable { mutableLongStateOf(initialMillis) }
    var monthStart by rememberSaveable {
        mutableLongStateOf(MonthWindow.current(initialMillis).start)
    }
    var showClock by rememberSaveable { mutableStateOf(false) }
    val window = remember(monthStart) { MonthWindow.current(monthStart) }
    val cells = remember(window) { window.calendarDateCells() }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 18.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(window.monthLabel(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                DateArrow(Icons.Default.ChevronLeft, "上个月") { monthStart = monthStart.shiftMonth(-1) }
                Spacer(Modifier.width(8.dp))
                DateArrow(Icons.Default.ChevronRight, "下个月") { monthStart = monthStart.shiftMonth(1) }
            }
            Spacer(Modifier.height(16.dp))
            DateGrid(
                cells = cells,
                selectedMillis = draftMillis,
                onSelected = { dayStart -> draftMillis = combineDateAndTime(dayStart, draftMillis) },
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("时间", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                PressableSurface(
                    onClick = { showClock = true },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                        Text(
                            SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(draftMillis)),
                            modifier = Modifier.padding(start = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(15.dp),
                ) { Text("取消", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { onConfirm(draftMillis) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(15.dp),
                ) { Text("确定", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showClock) {
        ClockDialog(
            initialMillis = draftMillis,
            onDismiss = { showClock = false },
            onConfirm = { hour, minute ->
                draftMillis = draftMillis.withTime(hour, minute)
                showClock = false
            },
        )
    }
}

@Composable
private fun DateArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        color = BrandBlue.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = BrandBlueLight)
        }
    }
}

@Composable
private fun DateGrid(
    cells: List<CalendarDateCell>,
    selectedMillis: Long,
    onSelected: (Long) -> Unit,
) {
    val selectedDay = selectedMillis.localDayKey()
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    val selected = cell.startMillis.localDayKey() == selectedDay
                    PressableSurface(
                        onClick = { onSelected(cell.startMillis) },
                        enabled = cell.inCurrentMonth,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        color = if (selected) BrandBlue else Color.Transparent,
                        shape = CircleShape,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (cell.inCurrentMonth) cell.dayNumber.toString() else "",
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val initial = remember(initialMillis) { Calendar.getInstance().apply { timeInMillis = initialMillis } }
    val state = rememberTimePickerState(
        initialHour = initial.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial.get(Calendar.MINUTE),
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("选择时间", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(14.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text("取消") }
                    Button(
                        onClick = { onConfirm(state.hour, state.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
                    ) { Text("确定") }
                }
            }
        }
    }
}

@Composable
internal fun QuickAddPreviewSurface(
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    nowMillis: Long,
    initialTransaction: TransactionListItem? = null,
) {
    QuickAddContent(
        accounts = accounts,
        expenseCategories = expenseCategories,
        incomeCategories = incomeCategories,
        onDismiss = {},
        onSave = { _, _ -> },
        initialTransaction = initialTransaction,
        onDelete = if (initialTransaction == null) null else ({ }),
        errorMessage = null,
        nowMillis = nowMillis,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun TransactionDateTimePreviewSurface(nowMillis: Long) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        BrandBlue.copy(alpha = 0.18f),
                    ),
                ),
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        TransactionDateTimeContent(
            initialMillis = nowMillis,
            onDismiss = {},
            onConfirm = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Long.amountInputValue(): String = BigDecimal.valueOf(this, 2)
    .setScale(2, RoundingMode.UNNECESSARY)
    .toPlainString()

private fun Long.shiftMonth(delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = this@shiftMonth
    add(Calendar.MONTH, delta)
}.timeInMillis

private fun combineDateAndTime(dayStart: Long, timeSource: Long): Long {
    val day = Calendar.getInstance().apply { timeInMillis = dayStart }
    val time = Calendar.getInstance().apply { timeInMillis = timeSource }
    day.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
    day.set(Calendar.MINUTE, time.get(Calendar.MINUTE))
    day.set(Calendar.SECOND, 0)
    day.set(Calendar.MILLISECOND, 0)
    return day.timeInMillis
}

private fun Long.withTime(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    timeInMillis = this@withTime
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun Long.localDayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(this))
