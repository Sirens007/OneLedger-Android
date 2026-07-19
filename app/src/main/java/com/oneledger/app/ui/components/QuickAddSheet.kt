package com.oneledger.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.BrandBlueLight
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (NewTransaction, Boolean) -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var accumulator by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOperator by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var showNote by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedToAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var occurredAt by rememberSaveable { mutableLongStateOf(nowMillis) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
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
    val displayAmount = amount.ifBlank { accumulator.orEmpty() }
    val resolvedAmount = resolveAmount(accumulator, pendingOperator, amount)
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
        val next = when (value) {
            "." -> if (amount.contains('.')) amount else if (amount.isBlank()) "0." else "$amount."
            else -> if (amount == "0") value else amount + value
        }
        if (next.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amount = next
    }

    fun inputOperator(operator: String) {
        val resolved = resolveAmount(accumulator, pendingOperator, amount) ?: return
        accumulator = resolved
        amount = ""
        pendingOperator = operator
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
            showNote = false
            occurredAt = nowMillis
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        QuickAddHeader(type = type, onTypeSelected = { type = it }, onDismiss = onDismiss)
        Spacer(Modifier.height(10.dp))
        if (type == TransactionType.TRANSFER) {
            TransferAccountSelector(
                accounts = accounts,
                fromAccountId = selectedAccountId,
                toAccountId = selectedToAccountId,
                onFromSelected = { selectedAccountId = it },
                onToSelected = { selectedToAccountId = it },
                modifier = Modifier.weight(1f),
            )
        } else {
            CategoryGrid(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelected = { selectedCategoryId = it },
                modifier = Modifier.weight(1f),
            )
        }
        QuickOptionRow(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = { selectedAccountId = it },
            noteEnabled = showNote,
            onToggleNote = { showNote = !showNote },
            onDateClick = { showDateTimePicker = true },
        )
        Spacer(Modifier.height(8.dp))
        AmountPanel(
            amount = displayAmount,
            pendingOperator = pendingOperator,
            accent = accent,
            note = note,
            showNote = showNote,
            occurredAt = occurredAt,
            onNoteChange = { if (it.length <= 60) note = it },
            onDateClick = { showDateTimePicker = true },
        )
        Spacer(Modifier.height(10.dp))
        CalculatorPad(
            accent = accent,
            canSave = canSave,
            onDigit = ::inputDigit,
            onOperator = ::inputOperator,
            onBackspace = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
            onSaveAgain = { save(true) },
            onDone = { save(false) },
        )
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
}

@Composable
private fun QuickAddHeader(
    type: String,
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
                Icon(Icons.Default.Close, contentDescription = "关闭记账", modifier = Modifier.size(24.dp))
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
            Text("账本", color = BrandBlueLight, style = MaterialTheme.typography.titleMedium)
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
    noteEnabled: Boolean,
    onToggleNote: () -> Unit,
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
            QuickOptionChip(
                icon = Icons.AutoMirrored.Filled.Notes,
                label = if (noteEnabled) "收起备注" else "填写备注",
                accent = BrandTeal,
                onClick = onToggleNote,
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
    pendingOperator: String?,
    accent: Color,
    note: String,
    showNote: Boolean,
    occurredAt: Long,
    onNoteChange: (String) -> Unit,
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text("¥", style = MaterialTheme.typography.headlineMedium, color = accent)
                Text(
                    text = amount.ifBlank { "0.00" },
                    modifier = Modifier.padding(start = 2.dp),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                    color = accent,
                    maxLines = 1,
                )
                pendingOperator?.let {
                    Text("  $it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                }
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
                if (!showNote) {
                    Text(
                        "点击上方“填写备注”添加说明",
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showNote) {
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = { Text("点击填写备注") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent.copy(alpha = 0.65f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun CalculatorPad(
    accent: Color,
    canSave: Boolean,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onBackspace: () -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "−"),
        listOf("7", "8", "9", "再记"),
        listOf(".", "0", "⌫", "完成"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { key ->
                    val primary = key == "完成"
                    val secondary = key == "再记"
                    PressableSurface(
                        onClick = {
                            when (key) {
                                "+", "−" -> onOperator(key)
                                "⌫" -> onBackspace()
                                "再记" -> onSaveAgain()
                                "完成" -> onDone()
                                else -> onDigit(key)
                            }
                        },
                        enabled = when (key) {
                            "完成", "再记" -> canSave
                            else -> true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        color = when {
                            primary -> accent
                            secondary -> accent.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when (key) {
                                "⌫" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "退格")
                                "完成" -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("完成", modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold)
                                }
                                else -> Text(
                                    key,
                                    style = if (key == "再记") MaterialTheme.typography.labelLarge else MaterialTheme.typography.headlineMedium,
                                    color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (key == "再记") FontWeight.Bold else FontWeight.Medium,
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
) {
    QuickAddContent(
        accounts = accounts,
        expenseCategories = expenseCategories,
        incomeCategories = incomeCategories,
        onDismiss = {},
        onSave = { _, _ -> },
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

private fun resolveAmount(accumulator: String?, operator: String?, current: String): String? {
    val right = current.toBigDecimalOrNull()
    if (accumulator == null) return right?.plainAmount()
    if (operator == null || right == null) return accumulator
    val left = accumulator.toBigDecimalOrNull() ?: return null
    val result = when (operator) {
        "+" -> left + right
        "−" -> left - right
        else -> right
    }
    return result.coerceAtLeast(BigDecimal.ZERO).plainAmount()
}

private fun BigDecimal.plainAmount(): String = setScale(2, RoundingMode.HALF_UP)
    .stripTrailingZeros()
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
