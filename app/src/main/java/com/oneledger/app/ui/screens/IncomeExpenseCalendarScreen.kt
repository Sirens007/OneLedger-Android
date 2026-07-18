package com.oneledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.CategoryGlyph
import com.oneledger.app.ui.components.DetailHeader
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.SavingsAmber
import com.oneledger.app.util.CalendarDateCell
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.calendarDateCells
import com.oneledger.app.util.dayKey
import com.oneledger.app.util.dayLabel
import com.oneledger.app.util.isIn
import com.oneledger.app.util.monthLabel
import com.oneledger.app.util.nextLocalDayStart
import com.oneledger.app.util.startOfLocalDay
import com.oneledger.app.util.timeLabel
import java.text.DecimalFormat

@Composable
fun IncomeExpenseCalendarScreen(
    state: OneLedgerUiState,
    onBack: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val monthWindow = remember(monthOffset, nowMillis) { MonthWindow.offset(monthOffset, nowMillis) }
    var selectedDayStart by rememberSaveable(monthWindow.start) {
        mutableLongStateOf(
            if (nowMillis.isIn(monthWindow)) nowMillis.startOfLocalDay() else monthWindow.start,
        )
    }
    val cells = remember(monthWindow) { monthWindow.calendarDateCells() }
    val transactionsByDay = state.transactions.groupBy { it.occurredAt.dayKey() }
    val monthTransactions = state.transactions.filter { it.occurredAt.isIn(monthWindow) }
    val monthExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val monthIncome = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val selectedTransactions = state.transactions.filter {
        it.occurredAt >= selectedDayStart && it.occurredAt < selectedDayStart.nextLocalDayStart()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        DetailHeader(
            title = "收支日历",
            onBack = onBack,
            eyebrow = "DAILY FLOW",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            action = {
                PressableSurface(
                    onClick = onQuickAdd,
                    modifier = Modifier.size(40.dp),
                    color = BrandBlue,
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "记一笔", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CalendarMonthHeader(
                    label = monthWindow.monthLabel(),
                    canMoveForward = monthOffset < 0,
                    onPrevious = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 },
                )
            }
            item {
                MonthFlowStrip(expenseMinor = monthExpense, incomeMinor = monthIncome)
            }
            item {
                OneLedgerCard(Modifier.fillMaxWidth()) {
                    CalendarGrid(
                        cells = cells,
                        transactionsByDay = transactionsByDay,
                        selectedDayStart = selectedDayStart,
                        todayStart = nowMillis.startOfLocalDay(),
                        onDaySelected = { selectedDayStart = it },
                    )
                }
            }
            item {
                SelectedDayTransactions(
                    dayStart = selectedDayStart,
                    transactions = selectedTransactions,
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    label: String,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
        CalendarArrow(Icons.Default.ChevronLeft, "上个月", true, onPrevious)
        Spacer(Modifier.size(7.dp))
        CalendarArrow(Icons.Default.ChevronRight, "下个月", canMoveForward, onNext)
    }
}

@Composable
private fun CalendarArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.28f),
            )
        }
    }
}

@Composable
private fun MonthFlowStrip(expenseMinor: Long, incomeMinor: Long) {
    OneLedgerCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowMetric("流出", expenseMinor, ExpenseCoral, Modifier.weight(1f))
            FlowMetric("流入", incomeMinor, IncomeMint, Modifier.weight(1f))
            FlowMetric("净流", incomeMinor - expenseMinor, BrandTeal, Modifier.weight(1f), signed = true)
        }
    }
}

@Composable
private fun FlowMetric(
    label: String,
    amount: Long,
    accent: Color,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(accent, CircleShape))
            Text(label, modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            MoneyFormatter.format(amount, showSign = signed),
            style = MaterialTheme.typography.titleMedium,
            color = if (signed && amount < 0) ExpenseCoral else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun CalendarGrid(
    cells: List<CalendarDateCell>,
    transactionsByDay: Map<String, List<TransactionListItem>>,
    selectedDayStart: Long,
    todayStart: Long,
    onDaySelected: (Long) -> Unit,
) {
    Column(Modifier.padding(horizontal = 9.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        cells.chunked(7).forEachIndexed { rowIndex, week ->
            if (rowIndex > 0) Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { cell ->
                    val transactions = transactionsByDay[cell.startMillis.dayKey()].orEmpty()
                    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                    CalendarDayCell(
                        cell = cell,
                        expenseMinor = expense,
                        incomeMinor = income,
                        selected = selectedDayStart == cell.startMillis,
                        today = todayStart == cell.startMillis,
                        onClick = { if (cell.inCurrentMonth) onDaySelected(cell.startMillis) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("双轨：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.size(6.dp).background(ExpenseCoral, CircleShape))
            Text(" 支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Box(Modifier.size(6.dp).background(IncomeMint, CircleShape))
            Text(" 收入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDateCell,
    expenseMinor: Long,
    incomeMinor: Long,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasFlow = expenseMinor > 0 || incomeMinor > 0
    val background = when {
        selected -> BrandBlue.copy(alpha = 0.22f)
        hasFlow -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        else -> Color.Transparent
    }
    PressableSurface(
        onClick = onClick,
        enabled = cell.inCurrentMonth,
        modifier = modifier
            .height(62.dp)
            .then(
                if (selected) Modifier.border(1.5.dp, BrandTeal, RoundedCornerShape(11.dp))
                else Modifier,
            ),
        color = background,
        shape = RoundedCornerShape(11.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = if (today) Modifier
                    .size(20.dp)
                    .background(BrandTeal, CircleShape) else Modifier.height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cell.dayNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        today -> Color.White
                        cell.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                    },
                )
            }
            Text(
                text = if (expenseMinor > 0) "−${expenseMinor.calendarAmount()}" else "—",
                color = if (expenseMinor > 0) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = if (incomeMinor > 0) "+${incomeMinor.calendarAmount()}" else "—",
                color = if (incomeMinor > 0) IncomeMint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedDayTransactions(
    dayStart: Long,
    transactions: List<TransactionListItem>,
) {
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayStart.dayLabel(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text(
                "−${MoneyFormatter.format(expense)}  +${MoneyFormatter.format(income)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OneLedgerCard(Modifier.fillMaxWidth()) {
            if (transactions.isEmpty()) {
                Text(
                    "这一天还没有收支记录",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        CalendarTransactionRow(transaction)
                        if (index != transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTransactionRow(transaction: TransactionListItem) {
    val color = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseCoral
        TransactionType.INCOME -> IncomeMint
        else -> SavingsAmber
    }
    val sign = if (transaction.type == TransactionType.EXPENSE) -1 else 1
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryGlyph(
            iconKey = transaction.iconKey,
            color = colorFromLong(transaction.colorHex),
            modifier = Modifier.size(38.dp),
            contentDescription = transaction.categoryName,
        )
        Column(
            modifier = Modifier
                .padding(start = 11.dp)
                .weight(1f),
        ) {
            Text(transaction.categoryName, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(transaction.timeLabelSafe(), transaction.note).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(MoneyFormatter.format(transaction.amountMinor * sign), color = color, style = MaterialTheme.typography.titleMedium)
            Text(transaction.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Long.calendarAmount(): String {
    val major = this / 100.0
    return when {
        major >= 1_000 -> DecimalFormat("0.#k").format(major / 1_000)
        else -> DecimalFormat("0.#").format(major)
    }
}

private fun TransactionListItem.timeLabelSafe(): String = occurredAt.timeLabel()
